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

package org.eclipse.team.svn.core.operation.local.management;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.svn.core.IConnectedProjectInformation;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.client.ISVNClient;
import org.eclipse.team.svn.core.client.SVNEntryStatus;
import org.eclipse.team.svn.core.client.ISVNClient.Depth;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.IUnprotectedOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * This operation relocate any projects associated with the selected repository 
 * 
 * @author Alexander Gurov
 */
public class RelocateWorkingCopyOperation extends AbstractWorkingCopyOperation implements IResourceProvider {
	protected IRepositoryLocation location;
	protected List resources;

	public RelocateWorkingCopyOperation(IResource []resources, IRepositoryLocation location) {
		super("Operation.RelocateResources", resources);
		this.location = location;
	}
	
	public RelocateWorkingCopyOperation(IResourceProvider provider, IRepositoryLocation location) {
		super("Operation.RelocateResources", provider);
		this.location = location;
	}

	public IResource []getResources() {
		return this.resources == null ? new IResource[0] : (IResource [])this.resources.toArray(new IResource[this.resources.size()]);
	}
	
	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		this.resources = new ArrayList();
		IResource []projects = this.operableData();
		if (projects.length == 0) {
			return;
		}
		final ISVNClient proxy = this.location.acquireSVNProxy();
		
		try {
			final IRepositoryResource []children = this.location.getRepositoryRoot().getChildren();
			final String rootUrl = this.location.getRepositoryRootUrl();
			
			for (int i = 0; i < projects.length && !monitor.isCanceled(); i++) {
				final IProject current = (IProject)projects[i];
				this.protectStep(new IUnprotectedOperation() {
					public void run(IProgressMonitor monitor) throws Exception {
						IConnectedProjectInformation provider = (IConnectedProjectInformation)RepositoryProvider.getProvider(current, SVNTeamPlugin.NATURE_ID);
						IPath fsLocation = current.getLocation();
						if (fsLocation != null) {
							String path = fsLocation.toString();
							SVNEntryStatus st = SVNUtility.getSVNInfoForNotConnected(current);
							if (st != null) {
								String url = SVNUtility.decodeURL(st.url);
								String oldRoot = SVNUtility.getOldRoot(url, children);
								if (oldRoot != null) {
									RelocateWorkingCopyOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn switch --relocate \"" + oldRoot + "\" \"" + rootUrl + "\" \"" + FileUtility.normalizePath(path) + "\"" + FileUtility.getUsernameParam(RelocateWorkingCopyOperation.this.location.getUsername()) + "\n");
									proxy.relocate(oldRoot, rootUrl, path, Depth.INFINITY, new SVNProgressMonitor(RelocateWorkingCopyOperation.this, monitor, null));
									provider.relocateResource();
									RelocateWorkingCopyOperation.this.resources.add(current);
								}
							}
						}
					}
				}, monitor, projects.length);
			}
		}
		finally {
		    this.location.releaseSVNProxy(proxy);
		}
	}
	
	protected String getShortErrorMessage(Throwable t) {
		return MessageFormat.format(super.getShortErrorMessage(t), new String[] {this.location.getUrl()});
	}

}