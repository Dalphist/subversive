/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sergiy Logvin - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.remote;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.DepthSelectionComposite;
import org.eclipse.team.svn.ui.composite.RevisionComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.verifier.ExistingResourceVerifier;

/**
 * Export panel
 * 
 * @author Sergiy Logvin
 */
public class ExportPanel extends AbstractDialogPanel {
	protected Text locationField;
	protected String location;
	protected RevisionComposite revisionComposite;
	protected IRepositoryResource selectedResource;
	protected DepthSelectionComposite depthSelector;
	
	public ExportPanel(IRepositoryResource baseResource) {
		super();
		this.dialogTitle = SVNUIMessages.ExportPanel_Title;
		this.dialogDescription = SVNUIMessages.ExportPanel_Description;
		this.defaultMessage = SVNUIMessages.ExportPanel_Message;
		this.selectedResource = baseResource;
	}	
	
	public SVNRevision getSelectedRevision() {
		return this.revisionComposite != null ? this.revisionComposite.getSelectedRevision() : SVNRevision.INVALID_REVISION;
	}
	
	protected void saveChangesImpl() {
		this.location = this.locationField.getText();
	}

    protected void cancelChangesImpl() {
    }
    
    public SVNDepth getDepth(){
    	if (this.depthSelector == null) {
    		return SVNDepth.INFINITY;
    	}
    	return this.depthSelector.getDepth();
    }
    
	public void createControlsImpl(Composite parent) {
		GridLayout layout = null;
		GridData data = null;
		
		Composite folderComposite = new Composite(parent, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = layout.marginWidth = 0;
		folderComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		folderComposite.setLayoutData(data);
		
		Label label = new Label(folderComposite, SWT.NONE);
		data = new GridData();
		label.setLayoutData(data);
		label.setText(SVNUIMessages.ExportPanel_Folder);
		
		this.locationField = new Text(folderComposite,  SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		this.locationField.setLayoutData(data);
		this.attachTo(this.locationField, new ExistingResourceVerifier(label.getText(), false));
		
		Button browseButton = new Button(folderComposite, SWT.PUSH);
		browseButton.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browseButton);
		browseButton.setLayoutData(data);
		browseButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				DirectoryDialog fileDialog = new DirectoryDialog(ExportPanel.this.manager.getShell());
				fileDialog.setText(SVNUIMessages.ExportPanel_ExportFolder);
				fileDialog.setMessage(SVNUIMessages.ExportPanel_ExportFolder_Msg);
				String path = fileDialog.open();
				if (path != null) {
					ExportPanel.this.locationField.setText(path);
				}
			}
		});
		
		if (this.selectedResource != null) {
			this.revisionComposite = new RevisionComposite(parent, this, false, null, SVNRevision.HEAD, false);
			data = new GridData(GridData.FILL_HORIZONTAL);
			this.revisionComposite.setLayoutData(data);
			this.revisionComposite.setSelectedResource(this.selectedResource);
		}
		
		if (this.selectedResource instanceof IRepositoryContainer || this.selectedResource == null) {
			Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
			separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			separator.setVisible(false);
			
			this.depthSelector = new DepthSelectionComposite(parent, SWT.NONE, false);
			data = new GridData(GridData.FILL_HORIZONTAL);
			this.depthSelector.setLayoutData(data);
		}
	}
	
	public String getHelpId() {
    	return "org.eclipse.team.svn.help.remote_exportDialogContext"; //$NON-NLS-1$
	}
	
	public String getLocation() {
		return this.location;
	}

}
