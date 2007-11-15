/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sergiy Logvin (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.compare;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Compare panel
 * 
 * @author Sergiy Logvin
 */
public class ComparePanel extends AbstractDialogPanel {
	
	protected CompareEditorInput compareInput;
	protected IResource resource;
	protected Composite parent;
	
	public ComparePanel(CompareEditorInput compareInput, IResource resource) {
		super(new String[] {SVNTeamUIPlugin.instance().getResource("CompareLocalPanel.Save"), IDialogConstants.CANCEL_LABEL});
		this.compareInput = compareInput;
		this.resource = resource;
		this.dialogTitle = SVNTeamUIPlugin.instance().getResource("CompareLocalPanel.Title");
		this.dialogDescription = SVNTeamUIPlugin.instance().getResource("CompareLocalPanel.Description");
		this.defaultMessage = SVNTeamUIPlugin.instance().getResource("CompareLocalPanel.Message");
	}
	
	public void createControls(Composite parent) {
		this.parent = parent;
		Control control = this.compareInput.createContents(parent);
		control.setLayoutData(new GridData(GridData.FILL_BOTH));
		Shell shell= control.getShell();
		shell.setText(this.compareInput.getTitle());
		shell.setImage(this.compareInput.getTitleImage());
	}

	protected void cancelChanges() {
		this.retainSizeAndWeights();
	}

	protected void saveChanges() {
		this.retainSizeAndWeights();
		
		RefreshResourcesOperation refreshOp = new RefreshResourcesOperation(new IResource[] {this.resource.getProject()});
		AbstractWorkingCopyOperation mainOp = new AbstractWorkingCopyOperation("Operation.SaveChanges", new IResource[] {this.resource.getProject()}) {
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				ComparePanel.this.compareInput.saveChanges(monitor);
			}
		};
		CompositeOperation composite = new CompositeOperation(mainOp.getId());
		composite.add(mainOp);
		composite.add(refreshOp);
		UIMonitorUtility.doTaskBusyWorkspaceModify(composite);
	}
	
	public Point getPrefferedSize() {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
    	
    	return new Point(SVNTeamPreferences.getCommitDialogInt(store, SVNTeamPreferences.COMPARE_DIALOG_WIDTH_NAME), 
    			SVNTeamPreferences.getCommitDialogInt(store, SVNTeamPreferences.COMPARE_DIALOG_HEIGHT_NAME));
	}
	
	protected void retainSizeAndWeights() {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		Point size = this.parent.getSize();
		SVNTeamPreferences.setCommitDialogInt(store, SVNTeamPreferences.COMPARE_DIALOG_WIDTH_NAME, size.x);
		SVNTeamPreferences.setCommitDialogInt(store, SVNTeamPreferences.COMPARE_DIALOG_HEIGHT_NAME, size.y);
	}

}