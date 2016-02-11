package org.csstudio.saverestore.ui;

import static org.csstudio.ui.fx.util.FXUtilities.setGridConstraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.csstudio.saverestore.DataProviderWrapper;
import org.csstudio.saverestore.FileUtilities;
import org.csstudio.saverestore.SaveRestoreService;
import org.csstudio.saverestore.data.BeamlineSet;
import org.csstudio.saverestore.data.BeamlineSetData;
import org.csstudio.saverestore.data.BeamlineSetData.Entry;
import org.csstudio.saverestore.ui.util.RunnableWithID;
import org.csstudio.ui.fx.util.FXEditorPart;
import org.csstudio.ui.fx.util.FXMessageDialog;
import org.csstudio.ui.fx.util.StaticTextField;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;

/**
 *
 * <code>BeamlineSetEditor</code> is an implementation of the {@link EditorPart} which allows editing the beamline sets.
 * User is allowed to change the description and the list of pvs in the set.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class BeamlineSetEditor extends FXEditorPart {

    public static final String ID = "org.csstudio.saverestore.ui.editor.beamlineseteditor";

    private final PseudoClass alertedPseudoClass = PseudoClass.getPseudoClass("alerted");

    private TextArea descriptionArea;
    private TextArea contentArea;

    private boolean dirty;
    private final BeamlineSetController controller;

    /**
     * Constructs a new beamline set editor.
     */
    public BeamlineSetEditor() {
        this.controller = new BeamlineSetController(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.ui.fx.util.FXEditorPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.csstudio.saverestore.ui.help.beamlineseteditor");
    }

    /**
     * Checks if the data provider of edited beamline set supports saving or editing of beamline sets. If yes, the
     * method returns true, otherwise it returns false.
     *
     * @return true if beamline set can be saved or false otherwise
     */
    private boolean canExecute() {
        return controller.getSavedBeamlineSetData().filter(d -> {
            DataProviderWrapper wrapper = SaveRestoreService.getInstance()
                .getDataProvider(d.getDescriptor().getDataProviderId());
            if (wrapper == null) {
                return false;
            }
            if (!wrapper.getProvider().isBeamlineSetSavingSupported()) {
                FXMessageDialog.openInformation(getSite().getShell(), "Save Beamline Set",
                    wrapper.getName() + " does not support editing or saving beamline sets.");
                return false;
            }
            return true;
        }).isPresent();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        Optional<BeamlineSetData> d = controller.getSavedBeamlineSetData();
        if (!d.isPresent() || d.get().getStoredDate() == null) {
            doSaveAs();
        } else if (canExecute()) {
            monitor.beginTask("Save beamline set", 1);
            final BeamlineSetData data = createData();
            if (data == null) {
                MessageDialog.openError(getSite().getShell(), "Save Beamline Set",
                    "There is an error in the file contents.");
                return;
            }
            if (data.equalContent(d.get())) {
                MessageDialog.openInformation(getSite().getShell(), "Save Beamline Set",
                    "Theare are no changes between the saved and this beamline set.");
                setDirty(false);
                return;
            }

            SaveRestoreService.getInstance().execute("Save Beamline Set", () -> {
                final Optional<BeamlineSetData> ds = controller.save(data);
                getSite().getShell().getDisplay().asyncExec(() -> {
                    monitor.done();
                    ds.ifPresent(e -> setInput(new BeamlineSetEditorInput(e)));
                });
            });
        }
    }

    private BeamlineSetData createData() {
        return createData(descriptionArea.getText().trim(), contentArea.getText(), true);
    }

    private BeamlineSetData createData(String description, String text, boolean markError) {
        String[] content = text.split("\\n");
        if (content.length == 0) {
            return null;
        }
        String[] d = FileUtilities.split(content[0]); // TODO, there could be some "
        if (d == null) {
            return null;
        }
        int length = d.length;
        List<String> pvList = new ArrayList<>(content.length);
        List<String> readbacksList = new ArrayList<>(content.length);
        List<String> deltasList = new ArrayList<>(content.length);
        for (String s : content) {
            s = s.trim();
            if (s.isEmpty()) {
                continue;
            }
            d = FileUtilities.split(s);
            if (d == null || d.length != length) {
                if (markError) {
                    // if marError == true we are in the UI thread
                    int idx = text.indexOf(s);
                    if (idx > -1) {
                        contentArea.selectRange(idx, idx + s.length());
                    }
                }
                return null;
            }
            pvList.add(d[0].trim());
            if (d.length > 1) {
                readbacksList.add(d[1].trim());
            }
            if (d.length > 2) {
                deltasList.add(d[2].trim());
            }
        }
        Optional<BeamlineSetData> bsd = controller.getSavedBeamlineSetData();
        BeamlineSet descriptor = bsd.isPresent() ? bsd.get().getDescriptor() : new BeamlineSet();
        return new BeamlineSetData(descriptor, pvList, readbacksList, deltasList, description);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {
        if (canExecute()) {
            final BeamlineSetData data = createData();
            if (data == null) {
                MessageDialog.openError(getSite().getShell(), "Save Beamline Set",
                    "There is an error in the file contents.");
                return;
            }
            if (data.equalContent(controller.getSavedBeamlineSetData().orElse(null)) && MessageDialog.openQuestion(
                getSite().getShell(), "Save Beamline Set As",
                "Theare are no changes between the saved and this beamline set. Are you sure you want to save it as a new beamline set?")) {
                setDirty(false);
                return;
            }
            new RepositoryTreeBrowser(this, data.getDescriptor()).openAndWait()
                .ifPresent(beamlineSet -> SaveRestoreService.getInstance().execute("Save Beamline Set",
                    () -> controller
                        .save(new BeamlineSetData(beamlineSet, data.getPVList(), data.getReadbackList(),
                            data.getDeltaList(), data.getDescription()))
                    .ifPresent(d -> getSite().getShell().getDisplay()
                        .asyncExec(() -> setInput(new BeamlineSetEditorInput(d))))));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Sets the dirt property of this editor and fires a change event.
     *
     * @param dirty true if the editor should be marked dirty or false if not dirty
     */
    private void setDirty(boolean dirty) {
        this.dirty = dirty;
        firePropertyChange(PROP_DIRTY);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
     */
    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }

    private void init() {
        IEditorInput input = getEditorInput();
        BeamlineSetData data = input.getAdapter(BeamlineSetData.class);
        if (data != null) {
            SaveRestoreService.getInstance().execute("Open beamline set", () -> setBeamlineSet(data));
        }
        firePropertyChange(PROP_TITLE);
    }

    private void setBeamlineSet(final BeamlineSetData data) {
        if (descriptionArea != null) {
            List<Entry> list = data.getEntries();
            final StringBuilder sb = new StringBuilder(list.size() * 200);
            list.forEach(e -> sb.append(e).append('\n'));
            Platform.runLater(() -> {
                controller.setSavedBeamlineSetData(data);
                descriptionArea.setText(data.getDescription());
                contentArea.setText(sb.toString().trim());
                setDirty(false);
            });
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
     */
    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        setPartName(input.getName());
        firePropertyChange(PROP_INPUT);
        init();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.ui.util.FXEditorPart#createFxScene()
     */
    @Override
    protected Scene createFxScene() {
        Scene scene = new Scene(new BorderPane(createCenterPane()));
        init();
        return scene;
    }

    private Node createCenterPane() {
        GridPane grid = new GridPane();
        grid.setVgap(3);
        grid.setPadding(new Insets(5, 5, 5, 5));
        Label descriptionLabel = new Label("Description:");
        descriptionLabel.setFont(Font.font(15));
        descriptionArea = new TextArea();
        descriptionArea.setEditable(true);
        descriptionArea.setTooltip(new Tooltip("Brief description of this beamline set"));
        descriptionArea.setMaxWidth(Double.MAX_VALUE);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(4);
        descriptionArea.textProperty().addListener((a, o, n) -> setDirty(true));

        Label contentLabel = new Label("PV List:");
        contentLabel.setFont(Font.font(15));

        contentArea = new TextArea();
        contentArea.getStylesheets()
            .add(BeamlineSetEditor.class.getResource(SnapshotViewerEditor.STYLE).toExternalForm());
        contentArea.setEditable(true);
        contentArea.setTooltip(new Tooltip("The list of PVs in this beamline set"));
        contentArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        contentArea.setWrapText(false);
        contentArea.textProperty().addListener((a, o, n) -> {
            setDirty(true);
            final String description = descriptionArea.getText();
            final String content = n;
            // execute the content check in the background thread, but only if the same task is not already being
            // executed
            Activator.getDefault().getBackgroundWorker().execute(new RunnableWithID() {
                @Override
                public void run() {
                    final BeamlineSetData data = createData(description, content, false);
                    Platform.runLater(() -> contentArea.pseudoClassStateChanged(alertedPseudoClass, data == null));
                }

                @Override
                public int getID() {
                    return BeamlineSetEditor.this.hashCode();
                }
            });
        });
        TextField titleArea = new StaticTextField();
        titleArea.setText(FileUtilities.BEAMLINE_SET_HEADER);

        GridPane contentPanel = new GridPane();
        contentPanel.setVgap(-1);
        setGridConstraints(titleArea, true, false, Priority.ALWAYS, Priority.NEVER);
        setGridConstraints(contentArea, true, true, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);
        contentPanel.add(titleArea, 0, 0);
        contentPanel.add(contentArea, 0, 1);

        setGridConstraints(descriptionLabel, true, true, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        setGridConstraints(contentLabel, true, true, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        setGridConstraints(descriptionArea, true, true, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER);
        setGridConstraints(contentPanel, true, true, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);

        grid.add(descriptionLabel, 0, 0);
        grid.add(descriptionArea, 0, 1);
        grid.add(contentLabel, 0, 2);
        grid.add(contentPanel, 0, 3);

        return grid;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.ui.util.FXEditorPart#setFxFocus()
     */
    @Override
    public void setFxFocus() {
        if (contentArea != null) {
            contentArea.requestFocus();
        }
    }
}
