/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.file;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNEntryStatusCallback;
import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.connector.SVNNotification;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevision.Number;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * This operation fetch remote resource statuses
 * 
 * @author Alexander Gurov
 */
public class RemoteStatusOperation extends AbstractStatusOperation implements ISVNNotificationCallback {
	protected Map<String, Number> pegRevisions = new HashMap<String, Number>();

	public RemoteStatusOperation(File []files, boolean recursive) {
		super("Operation_UpdateStatusFile", SVNMessages.class, files, recursive); //$NON-NLS-1$
	}

	public RemoteStatusOperation(IFileProvider provider, boolean recursive) {
		super("Operation_UpdateStatusFile", SVNMessages.class, provider, recursive); //$NON-NLS-1$
	}
	
	public SVNRevision getPegRevision(File change) {
	    IPath resourcePath = new Path(change.getAbsolutePath());
	    for (Iterator<?> it = this.pegRevisions.entrySet().iterator(); it.hasNext(); ) {
	        Map.Entry entry = (Map.Entry)it.next();
	        IPath rootPath = new Path((String)entry.getKey());
	        if (rootPath.isPrefixOf(resourcePath)) {
	            return (SVNRevision)entry.getValue();
	        }
	    }
	    return null;
	}

    public void notify(SVNNotification info) {
    	if (info.revision != SVNRevision.INVALID_REVISION_NUMBER) {
    		this.pegRevisions.put(info.path, SVNRevision.fromNumber(info.revision));
    	}
    }
    
	protected void reportStatuses(final ISVNConnector proxy, final ISVNEntryStatusCallback cb, final File current, IProgressMonitor monitor, int tasks) {
		SVNUtility.addSVNNotifyListener(proxy, this);
    	super.reportStatuses(proxy, cb, current, monitor, tasks);
		SVNUtility.removeSVNNotifyListener(proxy, this);
    }
    
    protected boolean isRemote() {
    	return true;
    }
    
}
