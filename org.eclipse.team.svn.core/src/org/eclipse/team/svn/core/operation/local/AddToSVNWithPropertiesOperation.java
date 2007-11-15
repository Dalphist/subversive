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

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.Team;
import org.eclipse.team.svn.core.client.ISVNClient;
import org.eclipse.team.svn.core.client.SVNClientException;
import org.eclipse.team.svn.core.client.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.client.SVNProperty;
import org.eclipse.team.svn.core.client.ISVNClient.Depth;
import org.eclipse.team.svn.core.client.SVNProperty.BuiltIn;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Set content properties operation
 * 
 * @author Sergiy Logvin
 */
public class AddToSVNWithPropertiesOperation extends AddToSVNOperation {
	protected static final String BINARY_FILE = "application/octet-stream";
	protected static final String TEXT_FILE = "text/plain";
	
	public AddToSVNWithPropertiesOperation(IResource[] resources) {
		this(resources, false);
	}
	
	public AddToSVNWithPropertiesOperation(IResource[] resources, boolean isRecursive) {
		super(resources, isRecursive);
	}
	
	public AddToSVNWithPropertiesOperation(IResourceProvider provider, boolean isRecursive) {
		super(provider, isRecursive);
	}

	protected void doAdd(IResource current, final ISVNClient proxy, final IProgressMonitor monitor) throws Exception {
		super.doAdd(current, proxy, monitor);
		
		if (!this.isRecursive) {
			this.processResource(current, proxy, monitor);
		}
		else {
			FileUtility.visitNodes(current, new IResourceVisitor() {

				public boolean visit(IResource resource) throws CoreException {
					if (monitor.isCanceled()) {
						return false;
					}
					
					try {
						AddToSVNWithPropertiesOperation.this.processResource(resource, proxy, monitor);
					}
					catch (SVNClientException cwe) {
						AddToSVNWithPropertiesOperation.this.reportError(cwe);
						return false;
					}
					return true;
				}
				
			}, IResource.DEPTH_INFINITE);
		}
	}
	
	protected void processResource(IResource resource, ISVNClient proxy, IProgressMonitor monitor) throws SVNClientException {
		String path = FileUtility.getWorkingCopyPath(resource);
		SVNProperty[] properties = CoreExtensionsManager.instance().getOptionProvider().getAutomaticProperties(resource.getName());
		for (int pCount = 0; pCount < properties.length; pCount++) {
			proxy.propertySet(path, properties[pCount].name, properties[pCount].value, Depth.EMPTY, false, new SVNProgressMonitor(this, monitor, null));
		}
		if (resource.getType() == IResource.FILE) {
			this.processFile(resource, proxy, monitor);
		}
	}
	
	protected void processFile(IResource resource, ISVNClient proxy, IProgressMonitor monitor) throws SVNClientException {
		String path = FileUtility.getWorkingCopyPath(resource);
		for (int i = 0; i < 2; i++) {
			int type = (i == 0) ?
					Team.getFileContentManager().getTypeForExtension(resource.getFileExtension() == null ? "" : resource.getFileExtension()) :
					Team.getFileContentManager().getTypeForName(resource.getName());
			SVNProperty data = proxy.propertyGet(new SVNEntryRevisionReference(path), BuiltIn.MIME_TYPE, new SVNProgressMonitor(this, monitor, null));
			if (data == null) {
				if (type == Team.BINARY) {
					proxy.propertySet(path, BuiltIn.MIME_TYPE, AddToSVNWithPropertiesOperation.BINARY_FILE, Depth.EMPTY, false, new SVNProgressMonitor(this, monitor, null));
				}
				else if (type == Team.TEXT) {
					proxy.propertySet(path, BuiltIn.MIME_TYPE, AddToSVNWithPropertiesOperation.TEXT_FILE, Depth.EMPTY, false, new SVNProgressMonitor(this, monitor, null));
				}
			}
		}
	}
	
}