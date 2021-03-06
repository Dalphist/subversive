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

package org.eclipse.team.svn.core.operation.remote;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNNotification;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.IRevisionProvider;
import org.eclipse.team.svn.core.operation.IUnprotectedOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Create new remote file by importing it from local file system 
 * 
 * @author Sergiy Logvin
 */
public class CreateFileOperation extends AbstractRepositoryOperation implements IRevisionProvider {
	protected String path;
	protected String message;
	protected String []fileNames;
	protected RevisionPair []revisionPair;
	
	public CreateFileOperation(IRepositoryResource resource, String path, String message, String []fileNames) {
		super("Operation_CreateFile", SVNMessages.class, new IRepositoryResource[] {resource}); //$NON-NLS-1$
		this.path = path;
		this.message = message;
		this.fileNames = fileNames;
	}
	
	public RevisionPair []getRevisions() {
		return this.revisionPair;
	}
	
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		final IRepositoryResource resource = this.operableData()[0];
		final IRepositoryLocation location = resource.getRepositoryLocation();
		this.revisionPair = new RevisionPair[1];
		final ISVNConnector proxy = location.acquireSVNProxy();
		ISVNNotificationCallback notify = new ISVNNotificationCallback() {
			public void notify(SVNNotification info) {
				if (info.revision != SVNRevision.INVALID_REVISION_NUMBER) {
					String []path = new String[] {resource.getUrl()};
					CreateFileOperation.this.revisionPair[0] = new RevisionPair(info.revision, path, location);
					String message = SVNMessages.format(SVNMessages.Console_CommittedRevision, new String[] {String.valueOf(info.revision)});
					CreateFileOperation.this.writeToConsole(IConsoleStream.LEVEL_OK, message);
				}
			}
		};
		try {
			SVNUtility.addSVNNotifyListener(proxy, notify);
			for (int i = 0; i < this.fileNames.length; i++) {
				final String []currentFile = new String[] {this.fileNames[i]};
				this.protectStep(new IUnprotectedOperation() {
					public void run(IProgressMonitor monitor) throws Exception {
						String path = FileUtility.normalizePath(CreateFileOperation.this.path + "/" + currentFile[0]); //$NON-NLS-1$
						String url = resource.getUrl() + "/" + currentFile[0]; //$NON-NLS-1$
						CreateFileOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn import \"" + path + "\" \"" + url + "\" -m \"" + CreateFileOperation.this.message + "\"" + ISVNConnector.Options.asCommandLine(ISVNConnector.Options.INCLUDE_IGNORED | ISVNConnector.Options.IGNORE_UNKNOWN_NODE_TYPES) + FileUtility.getUsernameParam(location.getUsername()) + " -N\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
						proxy.importTo(path, 
								SVNUtility.encodeURL(url), 
								CreateFileOperation.this.message, 
								SVNDepth.FILES,
								ISVNConnector.Options.INCLUDE_IGNORED | ISVNConnector.Options.IGNORE_UNKNOWN_NODE_TYPES, 
								null, null, new SVNProgressMonitor(CreateFileOperation.this, monitor, null));		
					}}, monitor, this.fileNames.length);
			}
		}
		finally {
			SVNUtility.removeSVNNotifyListener(proxy, notify);
			location.releaseSVNProxy(proxy);
		}
	}

}