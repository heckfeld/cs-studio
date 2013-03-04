/**
 * 
 */
package org.csstudio.graphene;

import static org.epics.util.time.TimeDuration.ofHertz;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.csstudio.csdata.ProcessVariable;
import org.csstudio.ui.util.BeanComposite;
import org.csstudio.ui.util.widgets.ErrorBar;
import org.csstudio.ui.util.widgets.RangeListener;
import org.csstudio.ui.util.widgets.StartEndRangeWidget;
import org.csstudio.ui.util.widgets.StartEndRangeWidget.ORIENTATION;
import org.csstudio.utility.pvmanager.ui.SWTUtil;
import org.csstudio.utility.pvmanager.widgets.VImageDisplay;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IMemento;
import org.epics.graphene.AxisRanges;
import org.epics.graphene.InterpolationScheme;
import org.epics.graphene.ScatterGraph2DRenderer;
import org.epics.graphene.ScatterGraph2DRendererUpdate;
import org.epics.pvmanager.PVManager;
import org.epics.pvmanager.PVReader;
import org.epics.pvmanager.PVReaderEvent;
import org.epics.pvmanager.PVReaderListener;
import org.epics.pvmanager.expression.DesiredRateExpression;
import org.epics.pvmanager.graphene.ExpressionLanguage;
import org.epics.pvmanager.graphene.ScatterGraphPlot;
import org.epics.pvmanager.graphene.Plot2DResult;
import org.epics.pvmanager.graphene.PlotDataRange;
import org.epics.vtype.VNumberArray;

/**
 * @author shroffk
 * 
 */
public class Scatter2DPlotWidget extends BeanComposite implements
	ISelectionProvider {

    private VImageDisplay imageDisplay;
    private ScatterGraphPlot plot;
    private ErrorBar errorBar;
    private boolean showAxis = true;
    private StartEndRangeWidget yRangeControl;
    private StartEndRangeWidget xRangeControl;

    public Scatter2DPlotWidget(Composite parent, int style) {
	super(parent, style);

	// Close PV on dispose
	addDisposeListener(new DisposeListener() {

	    @Override
	    public void widgetDisposed(DisposeEvent e) {
		if (pv != null) {
		    pv.close();
		    pv = null;
		}
	    }
	});

	setLayout(new FormLayout());

	errorBar = new ErrorBar(this, SWT.NONE);
	FormData fd_errorBar = new FormData();
	fd_errorBar.left = new FormAttachment(0, 2);
	fd_errorBar.right = new FormAttachment(100, -2);
	fd_errorBar.top = new FormAttachment(0, 2);
	errorBar.setLayoutData(fd_errorBar);

	errorBar.setMarginBottom(5);

	yRangeControl = new StartEndRangeWidget(this, SWT.NONE);
	FormData fd_yRangeControl = new FormData();
	fd_yRangeControl.top = new FormAttachment(errorBar, 2);
	fd_yRangeControl.left = new FormAttachment(0, 2);
	fd_yRangeControl.bottom = new FormAttachment(100, -15);
	fd_yRangeControl.right = new FormAttachment(0, 13);
	yRangeControl.setLayoutData(fd_yRangeControl);
	yRangeControl.setOrientation(ORIENTATION.VERTICAL);
	yRangeControl.addRangeListener(new RangeListener() {

	    @Override
	    public void rangeChanged() {
		if (plot != null) {
		    double invert = yRangeControl.getMin()
			    + yRangeControl.getMax();
		    plot.update(new ScatterGraph2DRendererUpdate()
			    .yAxisRange(AxisRanges.absolute(invert
				    - yRangeControl.getSelectedMax(), invert
				    - yRangeControl.getSelectedMin())));
		}
	    }
	});
	yRangeControl.setVisible(showAxis);

	imageDisplay = new VImageDisplay(this);
	FormData fd_imageDisplay = new FormData();
	fd_imageDisplay.top = new FormAttachment(errorBar, 2);
	fd_imageDisplay.right = new FormAttachment(100, -2);
	fd_imageDisplay.left = new FormAttachment(yRangeControl, 2);
	imageDisplay.setLayoutData(fd_imageDisplay);
	imageDisplay.setStretched(SWT.HORIZONTAL);

	imageDisplay.addControlListener(new ControlListener() {

	    @Override
	    public void controlResized(ControlEvent e) {
		if (plot != null) {
		    plot.update(new ScatterGraph2DRendererUpdate()
			    .imageHeight(imageDisplay.getSize().y)
			    .imageWidth(imageDisplay.getSize().x)
			    .interpolation(InterpolationScheme.LINEAR));
		}
	    }

	    @Override
	    public void controlMoved(ControlEvent e) {
		// Nothing to do
	    }
	});

	xRangeControl = new StartEndRangeWidget(this, SWT.NONE);
	fd_imageDisplay.bottom = new FormAttachment(xRangeControl, -2);
	FormData fd_xRangeControl = new FormData();
	fd_xRangeControl.left = new FormAttachment(0, 15);
	fd_xRangeControl.top = new FormAttachment(100, -13);
	fd_xRangeControl.right = new FormAttachment(100, -2);
	fd_xRangeControl.bottom = new FormAttachment(100, -2);
	xRangeControl.setLayoutData(fd_xRangeControl);
	xRangeControl.addRangeListener(new RangeListener() {

	    @Override
	    public void rangeChanged() {
		if (plot != null) {
		    plot.update(new ScatterGraph2DRendererUpdate()
			    .xAxisRange(AxisRanges.absolute(
				    xRangeControl.getSelectedMin(),
				    xRangeControl.getSelectedMax())));
		}
	    }
	});
	xRangeControl.setVisible(showAxis);

	addPropertyChangeListener(new PropertyChangeListener() {

	    @Override
	    public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("processVariable")
			|| event.getPropertyName().equals("xProcessVariable")) {
		    reconnect();
		} else if (event.getPropertyName().equals("showAxis")) {
		    xRangeControl.setVisible(showAxis);
		    yRangeControl.setVisible(showAxis);
		    redraw();
		}

	    }
	});
    }

    @Override
    public void setMenu(Menu menu) {
	super.setMenu(menu);
	imageDisplay.setMenu(menu);
    }

    private PVReader<Plot2DResult> pv;

    private String pvName;
    private String xPvName;

    public boolean isShowAxis() {
	return showAxis;
    }

    public boolean getShowAxis() {
	return this.showAxis;
    }

    public void setShowAxis(boolean showAxis) {
	boolean oldValue = this.showAxis;
	this.showAxis = showAxis;
	changeSupport.firePropertyChange("showAxis", oldValue, this.showAxis);
    }

    public String getXpvName() {
	return xPvName;
    }

    public void setXPvName(String xPvName) {
	String oldValue = this.xPvName;
	this.xPvName = xPvName;
	changeSupport.firePropertyChange("xProcessVariable", oldValue,
		this.xPvName);
    }

    public String getPvName() {
	return this.pvName;
    }

    public void setPvName(String pvName) {
	String oldValue = this.pvName;
	this.pvName = pvName;
	changeSupport.firePropertyChange("processVariable", oldValue,
		this.pvName);
    }

    public void setPvs(String pvName, String xPvName) {

    }

    private void setLastError(Exception lastException) {
	errorBar.setException(lastException);
    }

    @SuppressWarnings("unchecked")
    private void reconnect() {
	if (pv != null) {
	    pv.close();
	    imageDisplay.setVImage(null);
	    setLastError(null);
	    plot = null;
	    resetRange(xRangeControl);
	    resetRange(yRangeControl);
	}

	// For ScatterPlot both x and y pvs are needed
	if (getPvName() == null || getPvName().isEmpty()) {
	    return;
	}
	if (getXpvName() == null || getXpvName().isEmpty()) {
	    return;
	}

	plot = ExpressionLanguage
		.scatterGraphOf(
			(DesiredRateExpression<? extends VNumberArray>) org.epics.pvmanager.formula.ExpressionLanguage
				.formula(getXpvName()),
			(DesiredRateExpression<? extends VNumberArray>) org.epics.pvmanager.formula.ExpressionLanguage
				.formula(getPvName()));
	plot.update(new ScatterGraph2DRendererUpdate()
		.imageHeight(imageDisplay.getSize().y)
		.imageWidth(imageDisplay.getSize().x)
		.interpolation(InterpolationScheme.NONE));
	pv = PVManager.read(plot).notifyOn(SWTUtil.swtThread())
		.readListener(new PVReaderListener<Plot2DResult>() {
		    @Override
		    public void pvChanged(PVReaderEvent<Plot2DResult> event) {
			Exception ex = pv.lastException();

			if (ex != null) {
			    setLastError(ex);
			}
			if (pv.getValue() != null) {
			    setRange(xRangeControl, pv.getValue().getxRange());
			    setRange(yRangeControl, pv.getValue().getyRange());
			    imageDisplay.setVImage(pv.getValue().getImage());
			} else {
			    imageDisplay.setVImage(null);
			}
		    }
		}).maxRate(ofHertz(50));
    }

    /**
     * A helper function to set all the appropriate
     * 
     * @param control
     */
    private void setRange(StartEndRangeWidget control,
	    PlotDataRange plotDataRange) {
	control.setRange(plotDataRange.getStartIntegratedDataRange(),
		plotDataRange.getEndIntegratedDataRange());
    }

    private void resetRange(StartEndRangeWidget control) {
	control.setRanges(0, 0, 1, 1);
    }

    /** Memento tag */
    private static final String MEMENTO_PVNAME = "PVName"; //$NON-NLS-1$

    public void saveState(IMemento memento) {
	if (getPvName() != null) {
	    memento.putString(MEMENTO_PVNAME, getPvName());
	}
    }

    public void loadState(IMemento memento) {
	if (memento != null) {
	    if (memento.getString(MEMENTO_PVNAME) != null) {
		setPvName(memento.getString(MEMENTO_PVNAME));
	    }
	}
    }

    public void setConfigurable(boolean configurable) {
	// TODO Auto-generated method stub

    }

    @Override
    public ISelection getSelection() {
	if (getPvName() != null) {
	    return new StructuredSelection(new Scatter2DPlotSelection(
		    new ProcessVariable(getPvName()), new ProcessVariable(
			    getXpvName()), this));
	}
	return null;
    }

    @Override
    public void addSelectionChangedListener(
	    final ISelectionChangedListener listener) {
    }

    @Override
    public void removeSelectionChangedListener(
	    ISelectionChangedListener listener) {
    }

    @Override
    public void setSelection(ISelection selection) {
	throw new UnsupportedOperationException("Not implemented yet");
    }

}
