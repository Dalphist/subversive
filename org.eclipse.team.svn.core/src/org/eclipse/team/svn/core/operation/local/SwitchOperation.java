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

package org.eclipse.team.svn.core.operation.local;

import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.svn.core.IConnectedProjectInformation;
import org.eclipse.team.svn.core.client.ISVNClient;
import org.eclipse.team.svn.core.client.ISVNClient.Depth;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.IUnprotectedOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.remote.AbstractRepositoryOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Switch working copy base url operation implementation
 * 
 * @author Alexander Gurov
 */
public class SwitchOperation extends AbstractRepositoryOperation {
	protected IResource []resources;

	public SwitchOperation(IResource []resources, IRepositoryResourceProvider destination) {
		super("Operation.Switch", destination);
		this.resources = resources;
	}

	public SwitchOperation(IResource []resources, IRepositoryResource []destination) {
		super("Operation.Switch", destination);
		this.resources = resources;
	}
	
	public ISchedulingRule getSchedulingRule() {
    	HashSet ruleSet = new HashSet();
    	for (int i = 0; i < this.resources.length; i++) {
			ruleSet.add(this.resources[i] instanceof IProject ? this.resources[i] : this.resources[i].getParent());
    	}
    	return new MultiRule((IResource [])ruleSet.toArray(new IResource[ruleSet.size()]));
	}
	
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource []destinations = this.operableData();
		for (int i = 0; i < this.resources.length; i++) {
			final IResource resource = resources[i];
			final IRepositoryResource destination = destinations[i];
			final IRepositoryLocation location = destination.getRepositoryLocation();
			final ISVNClient proxy = location.acquireSVNProxy();
			
			this.protectStep(new IUnprotectedOperation() {
				public void run(IProgressMonitor monitor) throws Exception {
					String wcPath = FileUtility.getWorkingCopyPath(resource);
					SwitchOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn switch \"" + destination.getUrl() + "\" \"" + FileUtility.normalizePath(wcPath) + "\" -r " + destination.getSelectedRevision() + FileUtility.getUsernameParam(location.getUsername()) + "\n");
					proxy.doSwitch(wcPath, 
							SVNUtility.getEntryRevisionReference(destination), 
							Depth.infinityOrFiles(true),
							false, 
							false,
							new SVNProgressMonitor(SwitchOperation.this, monitor, null));
					
					if (resource instanceof IProject) {
						IConnectedProjectInformation provider = (IConnectedProjectInformation)RepositoryProvider.getProvider((IProject)resource);
						provider.switchResource(destination);
					}
				}
			}, monitor, 0);
			
			location.releaseSVNProxy(proxy);		
		}
	}

}