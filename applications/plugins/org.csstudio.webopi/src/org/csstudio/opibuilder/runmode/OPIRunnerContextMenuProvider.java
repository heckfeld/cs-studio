/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.runmode;

import java.util.List;

import org.csstudio.opibuilder.actions.OpenRelatedDisplayAction;
import org.csstudio.opibuilder.actions.OpenRelatedDisplayAction.OPEN_DISPLAY_TARGET;
import org.csstudio.opibuilder.actions.WidgetActionMenuAction;
import org.csstudio.opibuilder.editparts.AbstractBaseEditPart;
import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.csstudio.opibuilder.util.WorkbenchWindowService;
import org.csstudio.opibuilder.widgetActions.AbstractOpenOPIAction;
import org.csstudio.opibuilder.widgetActions.AbstractWidgetAction;
import org.csstudio.opibuilder.widgetActions.ActionsInput;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

/**
 * ContextMenuProvider implementation for the OPI Runner.
 * 
 * @author Xihui Chen
 * 
 */
public final class OPIRunnerContextMenuProvider extends ContextMenuProvider {
	
	private IOPIRuntime opiRuntime;

	/**
	 * Constructor.
	 * 
	 * @param viewer
	 *            the graphical viewer
	 */
	public OPIRunnerContextMenuProvider(final EditPartViewer viewer, final IOPIRuntime opiRuntime) {
		super(viewer);
		this.opiRuntime = opiRuntime;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void buildContextMenu(final IMenuManager menu) {
		addWidgetActionToMenu(menu);
		GEFActionConstants.addStandardActionGroups(menu);
		
		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		
		ActionRegistry actionRegistry =
			(ActionRegistry) opiRuntime.getAdapter(ActionRegistry.class);
		
		menu.appendToGroup(GEFActionConstants.GROUP_EDIT, 
				WorkbenchWindowService.getInstance().getFullScreenAction(activeWindow));
		menu.appendToGroup(GEFActionConstants.GROUP_EDIT, 
				WorkbenchWindowService.getInstance().getCompactModeAction(activeWindow));
		//actionRegistry.getAction(CompactModeAction.ID));

		

		menu.appendToGroup(GEFActionConstants.GROUP_EDIT, actionRegistry.getAction(ActionFactory.PRINT.getId()));

//		MenuManager cssMenu = new MenuManager("CSS", "css");
//		cssMenu.add(new Separator("additions")); //$NON-NLS-1$
//		menu.add(cssMenu);		
	}
	
	/**
	 * Adds the defined {@link AbstractWidgetAction}s to the given {@link IMenuManager}.
	 * @param menu The {@link IMenuManager}
	 */
	private void addWidgetActionToMenu(final IMenuManager menu) {
		List<?> selectedEditParts = ((IStructuredSelection)getViewer().getSelection()).toList();
		if (selectedEditParts.size()==1) {
			if (selectedEditParts.get(0) instanceof AbstractBaseEditPart) {
				AbstractBaseEditPart editPart = (AbstractBaseEditPart) selectedEditParts.get(0);
				AbstractWidgetModel widget = editPart.getWidgetModel();
				
				//add menu Open, Open in New Tab and Open in New Window
				AbstractWidgetAction hookedAction = editPart.getHookedAction();
				if(hookedAction != null && hookedAction instanceof AbstractOpenOPIAction){
					menu.add(new OpenRelatedDisplayAction(
							(AbstractOpenOPIAction) hookedAction, OPEN_DISPLAY_TARGET.DEFAULT));
					menu.add(new OpenRelatedDisplayAction(
							(AbstractOpenOPIAction) hookedAction, OPEN_DISPLAY_TARGET.TAB));
					menu.add(new OpenRelatedDisplayAction(
							(AbstractOpenOPIAction) hookedAction, OPEN_DISPLAY_TARGET.NEW_WINDOW));					
				}
				
				ActionsInput ai = widget.getActionsInput();
				if(ai != null){
					List<AbstractWidgetAction> widgetActions = ai.getActionsList();
					if (!widgetActions.isEmpty()) {
						MenuManager actionMenu = new MenuManager("Actions", "actions");
						for (AbstractWidgetAction action : widgetActions) {
							actionMenu.add(new WidgetActionMenuAction(action));
						}
						menu.add(actionMenu);
					}
				}
			}
		}
	}
	
	
}
