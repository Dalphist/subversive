/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.dialog;

import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;

/**
 * Node Kind changed notification dialog
 * 
 * @author Alexander Gurov
 */
public class NotifyNodeKindChangedDialog extends MessageDialog {

    public NotifyNodeKindChangedDialog(Shell parentShell, IResource []resources) {
		super(parentShell, 
			SVNTeamUIPlugin.instance().getResource("NotifyNodeKindChangedDialog.Title"), 
			null, 
			SVNTeamUIPlugin.instance().getResource("NotifyNodeKindChangedDialog.Message", new String[] {NotifyNodeKindChangedDialog.enumerateParents(resources)}),
			MessageDialog.WARNING, 
			new String[] {IDialogConstants.OK_LABEL}, 
			0);
    }
    
    protected static String enumerateParents(IResource []resources) {
        HashSet parents = new HashSet();
        for (int i = 0; i < resources.length; i++) {
            parents.add(resources[i].getParent());
        }
        resources = (IResource [])parents.toArray(new IResource[parents.size()]);
        FileUtility.reorder(resources, true);
        String retVal = "";
        for (int i = 0; i < resources.length; i++) {
            retVal += "'" + resources[i].getFullPath().toString() + "'\n";
        }
        return retVal;
    }
    
}
