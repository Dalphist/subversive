/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.remote;

import java.io.FileOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.operation.AbstractGetFileContentOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * This operation retrieve a repository file content
 * 
 * @author Alexander Gurov
 */
public class GetFileContentOperation extends AbstractGetFileContentOperation {
	protected IRepositoryResource resource;
	protected IRepositoryResourceProvider provider;
	
	public GetFileContentOperation(IRepositoryResource resource) {
		super("Revision"); //$NON-NLS-1$
		this.resource = resource;
	}

	public GetFileContentOperation(IRepositoryResourceProvider provider) {
		super("Revision"); //$NON-NLS-1$
		this.provider = provider;
	}
	
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		if (this.provider != null) {
			this.resource = this.provider.getRepositoryResources()[0];
		}
		
		String url = this.resource.getUrl();
		IRepositoryLocation location = this.resource.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		FileOutputStream stream = null;
		try {
			url = SVNUtility.encodeURL(url);
//			this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn cat " + url + "@" + this.resource.getPegRevision() + " -r " + selected + " --username \"" + location.getUsername() + "\"\n");
			this.tmpFile = this.createTempFile();
			stream = new FileOutputStream(this.tmpFile);
			
			proxy.streamFileContent(
					SVNUtility.getEntryRevisionReference(this.resource), 
					2048, 
					stream,
					new SVNProgressMonitor(GetFileContentOperation.this, monitor, null));
		}
		finally {
			location.releaseSVNProxy(proxy);
			if (stream != null) {
				try {stream.close();} catch (Exception ex) {}
			}
		}
	}
	
	protected String getExtension() {
		String name = this.resource.getName();
		int idx = name.lastIndexOf('.');
		return idx == -1 ? "" : name.substring(idx + 1); //$NON-NLS-1$
	}
	
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] {this.resource.getUrl()});
	}

}
