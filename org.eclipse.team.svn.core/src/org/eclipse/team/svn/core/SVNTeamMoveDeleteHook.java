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

package org.eclipse.team.svn.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.IResourceTree;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.extension.options.IOptionProvider;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.AddToSVNWithPropertiesOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.property.GetPropertiesOperation;
import org.eclipse.team.svn.core.operation.local.refactor.CopyResourceFromHookOperation;
import org.eclipse.team.svn.core.operation.local.refactor.CopyResourceWithHistoryOperation;
import org.eclipse.team.svn.core.operation.local.refactor.DeleteResourceOperation;
import org.eclipse.team.svn.core.operation.local.refactor.MoveResourceOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ILoggedOperationFactory;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Hook for handle resources move(refactor) and deletion
 * 
 * @author Alexander Gurov
 */
public class SVNTeamMoveDeleteHook implements IMoveDeleteHook {
	private static ScheduledExecutorService sPool;
	private static ScheduledFuture sf;
	private static ArrayList<IResource> deleteQueue;
	
	static {
		SVNTeamMoveDeleteHook.sPool = Executors.newScheduledThreadPool(1);
		SVNTeamMoveDeleteHook.deleteQueue = new ArrayList<IResource>();
	}
	
	public SVNTeamMoveDeleteHook() {
	}

	public boolean deleteFile(IResourceTree tree, IFile file, int updateFlags, IProgressMonitor monitor) {
		return this.doScheduledDelete(tree, file, updateFlags, monitor);
	}

	public boolean deleteFolder(IResourceTree tree, final IFolder folder, int updateFlags, IProgressMonitor monitor) {
		return this.doScheduledDelete(tree, folder, updateFlags, monitor);
	}
	
	public boolean moveFile(final IResourceTree tree, final IFile source, final IFile destination, int updateFlags, IProgressMonitor monitor) {
		return this.doMove(tree, source, destination, updateFlags, monitor);
	}

	public boolean moveFolder(final IResourceTree tree, final IFolder source, final IFolder destination, int updateFlags, IProgressMonitor monitor) {
		return this.doMove(tree, source, destination, updateFlags, monitor);
	}

	public boolean deleteProject(IResourceTree tree, IProject project, int updateFlags, IProgressMonitor monitor) {
		//NOTE Eclipse bug ? Project should be disconnected first but it is not possible due to different resource locking rules used by project deletion and project unmpaping code.
		return false;
	}

	public boolean moveProject(IResourceTree tree, IProject source, IProjectDescription description, int updateFlags, IProgressMonitor monitor) {
		return false;
	}

	protected boolean doMove(IResourceTree tree, IResource source, IResource destination, int updateFlags, IProgressMonitor monitor) {
		ILocalResource local = SVNRemoteStorage.instance().asLocalResource(source);
		if (!IStateFilter.SF_VERSIONED.accept(local)) {
			return FileUtility.isSVNInternals(source);
		}
		
		// if source is versioned we MUST perform source-control related tasks
		MoveResourceOperation moveOp = new MoveResourceOperation(source, destination);
		CompositeOperation op = new CompositeOperation(moveOp.getId(), moveOp.getMessagesClass());
		SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(new IResource[] {source, destination});
		op.add(saveOp);
		if ((updateFlags & IResource.KEEP_HISTORY) != 0) {
			op.add(new SaveToLocalHistoryOperation(tree, source));
		}
		
		local = SVNRemoteStorage.instance().asLocalResource(destination.getParent());
		if (IStateFilter.SF_INTERNAL_INVALID.accept(local) || IStateFilter.SF_LINKED.accept(local) || IStateFilter.SF_OBSTRUCTED.accept(local) || !moveOp.isAllowed()) {
			//target was placed on different repository or resource is moved into non-managed project/folder -- do <copy + delete>
			AbstractActionOperation copyLocalResourceOp = new CopyResourceFromHookOperation(source, destination, FileUtility.COPY_NO_OPTIONS);
			op.add(copyLocalResourceOp);
			DeleteResourceOperation deleteOp = new DeleteResourceOperation(source);
			op.add(deleteOp, new IActionOperation[] {copyLocalResourceOp});
			op.add(new TrackMoveResultOperation(tree, source, destination, new IActionOperation[] {copyLocalResourceOp, deleteOp}));
			op.add(new RestoreProjectMetaOperation(saveOp));
		    op.add(new RefreshResourcesOperation(new IResource[] {source, destination}, IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL));
		}
    	else if (IStateFilter.SF_UNVERSIONED.accept(local)) {
	        IResource []scheduledForAddition = FileUtility.getOperableParents(new IResource[] {destination}, IStateFilter.SF_UNVERSIONED, true);
	        AbstractActionOperation addToSVNOp = new AddToSVNWithPropertiesOperation(scheduledForAddition, false); 
	        op.add(addToSVNOp);
	        if (this.checkBug281557(source, monitor)) {
				CopyResourceWithHistoryOperation copyOp = new CopyResourceWithHistoryOperation(source, destination);
				op.add(copyOp);
				// do overwrite file content in order to restore original keywords values
				CopyResourceFromHookOperation copyLocalResourceOp = new CopyResourceFromHookOperation(source, destination, FileUtility.COPY_OVERRIDE_EXISTING_FILES);
				op.add(copyLocalResourceOp);
				// end overwrite
				DeleteResourceOperation deleteOp = new DeleteResourceOperation(source);
				op.add(deleteOp, new IActionOperation[] {copyOp, copyLocalResourceOp});
				op.add(new TrackMoveResultOperation(tree, source, destination, new IActionOperation[] {copyOp, copyLocalResourceOp, deleteOp}));
			}
			else {
		       	op.add(moveOp, new IActionOperation[] {addToSVNOp});
		       	op.add(new TrackMoveResultOperation(tree, source, destination, new IActionOperation[] {moveOp}));
			}
			op.add(new RestoreProjectMetaOperation(saveOp));
	       	ArrayList<IResource> fullSet = new ArrayList<IResource>(Arrays.asList(scheduledForAddition));
	       	fullSet.addAll(Arrays.asList(new IResource[] {source, destination}));
		    op.add(new RefreshResourcesOperation(fullSet.toArray(new IResource[fullSet.size()]), IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL));
	    }
	    else {
	        if (this.checkBug281557(source, monitor)) {
				CopyResourceWithHistoryOperation copyOp = new CopyResourceWithHistoryOperation(source, destination);
				op.add(copyOp);
				// do overwrite file content in order to restore original keywords values
				CopyResourceFromHookOperation copyLocalResourceOp = new CopyResourceFromHookOperation(source, destination, FileUtility.COPY_OVERRIDE_EXISTING_FILES);
				op.add(copyLocalResourceOp);
				// end overwrite
				DeleteResourceOperation deleteOp = new DeleteResourceOperation(source);
				op.add(deleteOp, new IActionOperation[] {copyOp, copyLocalResourceOp});
				op.add(new TrackMoveResultOperation(tree, source, destination, new IActionOperation[] {copyOp, copyLocalResourceOp, deleteOp}));
			}
			else {
		    	op.add(moveOp);
		    	op.add(new TrackMoveResultOperation(tree, source, destination, new IActionOperation[] {moveOp}));	    		    	
			}
			op.add(new RestoreProjectMetaOperation(saveOp));
		    op.add(new RefreshResourcesOperation(new IResource[] {source, destination}, IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL));
		}
		this.runOperation(op, monitor);
							    	  	    
		return true;
	}
	
	protected boolean checkBug281557(IResource source, IProgressMonitor monitor) {
		//see https://bugs.eclipse.org/bugs/show_bug.cgi?id=281557
		if (source.getType() == IResource.FILE && SVNTeamPlugin.instance().getOptionProvider().getString(IOptionProvider.SVN_CONNECTOR_ID).startsWith("org.eclipse.team.svn.connector.javahl")) {
			GetPropertiesOperation op = new GetPropertiesOperation(source);
			this.runOperation(op, monitor);
			SVNProperty []props = op.getProperties();
			if (props != null) {
				for (int i = 0; i < props.length; i++) {
					if (SVNProperty.BuiltIn.KEYWORDS.equals(props[i].name)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	protected void runOperation(IActionOperation op, IProgressMonitor monitor) {
		// already in WorkspaceModifyOperation context
		//don't log errors from operations, because errors are logged by caller code (IMoveDeleteHook infrastructure)		    
	    ProgressMonitorUtility.doTaskExternal(op, monitor, new ILoggedOperationFactory() {
			public IActionOperation getLogged(IActionOperation operation) {
				//set only console stream to operation
				IActionOperation wrappedOperation = SVNTeamPlugin.instance().getOptionProvider().getLoggedOperationFactory().getLogged(operation);
				operation.setConsoleStream(wrappedOperation.getConsoleStream());
				return operation;
			}
		});
	}
	
	// It is to slow to access working copy on per-file basis when the projects are large enough, but the workflow of the actual operation heavily depends 
	//	on how the calling code is implemented. So, there are cases when recursive deletes are performed one by one and JDT is the fine example of such an approach.
	//	So, in order to reduce overhead we'll try to group the files using a reasonable timeout.
	protected boolean doScheduledDelete(IResourceTree tree, IResource resource, int updateFlags, IProgressMonitor monitor) {
		ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
		if (IStateFilter.SF_INTERNAL_INVALID.accept(local) || IStateFilter.SF_NOTEXISTS.accept(local) || IStateFilter.SF_UNVERSIONED.accept(local)) {
			return FileUtility.isSVNInternals(resource);
		}

		// since the tree object  is valid in the context of the call only, do it now!
		if (resource.getType() == IResource.FILE) {
			if ((updateFlags & IResource.KEEP_HISTORY) != 0) {
				tree.addToLocalHistory((IFile)resource);
			}
			tree.deletedFile((IFile)resource);
		}
		else /*if (CoreExtensionsManager.instance().getSVNConnectorFactory().getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_6_x)*/ {
			tree.deletedFolder((IFolder)resource);
		}
		
		synchronized (SVNTeamMoveDeleteHook.deleteQueue) {
			SVNTeamMoveDeleteHook.deleteQueue.add(resource);
			if (SVNTeamMoveDeleteHook.deleteQueue.size() > 1 && SVNTeamMoveDeleteHook.sf != null) {
				// if the timer is started already, then cancel it in order to reset the scheduled start time
				SVNTeamMoveDeleteHook.sf.cancel(false);
				SVNTeamMoveDeleteHook.sf = null;
			}
			if (SVNTeamMoveDeleteHook.deleteQueue.size() > 0) {
				// if there are files to delete
				SVNTeamMoveDeleteHook.sf = SVNTeamMoveDeleteHook.sPool.schedule(new Runnable() {
					public void run() {
						IResource []resources;
						synchronized (SVNTeamMoveDeleteHook.deleteQueue) {
							resources = SVNTeamMoveDeleteHook.deleteQueue.toArray(new IResource[SVNTeamMoveDeleteHook.deleteQueue.size()]);
							SVNTeamMoveDeleteHook.deleteQueue.clear();
						}
						if (resources.length > 0) {
							resources = FileUtility.shrinkChildNodes(resources);
							
							DeleteResourceOperation mainOp = new DeleteResourceOperation(resources);
						    CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
							SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(resources);
							op.add(saveOp);
						    op.add(mainOp);
							op.add(new RestoreProjectMetaOperation(saveOp));
						    op.add(new RefreshResourcesOperation(resources, IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_CACHE));

							IActionOperation wrappedOperation = SVNTeamPlugin.instance().getOptionProvider().getLoggedOperationFactory().getLogged(op);
						    
						    ProgressMonitorUtility.doTaskScheduledDefault(wrappedOperation);
						}
					}
				}, 200, TimeUnit.MILLISECONDS);
			}
		}
		return true;
	}
	
	protected static class SaveToLocalHistoryOperation extends AbstractActionOperation {
		protected IResourceTree tree;
		protected IResource resource;
		
		public SaveToLocalHistoryOperation(IResourceTree tree, IResource resource) {			
			super("Operation_TrackDeleteResult", SVNMessages.class); //$NON-NLS-1$
			this.tree = tree;
			this.resource = resource;
		}
	
		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			if (this.resource.getType() == IResource.FILE) {
				this.tree.addToLocalHistory((IFile)this.resource);
			}
		}
	}
	
	protected static class TrackMoveResultOperation extends AbstractActionOperation {
		protected IResourceTree tree;
		protected IResource source;
		protected IResource destination;
		protected IActionOperation []operationsToTrack;
		
		public TrackMoveResultOperation(IResourceTree tree, IResource source, IResource destination, IActionOperation []operationsToTrack) {			
			super("Operation_TrackMoveResult", SVNMessages.class); //$NON-NLS-1$
			this.tree = tree;
			this.source = source;
			this.destination = destination;
			this.operationsToTrack = operationsToTrack;
		}
	
		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			boolean failed = false;
			if (this.operationsToTrack != null) {
				for (int i = 0; i < this.operationsToTrack.length; i++) {
					if (this.operationsToTrack[i].getExecutionState() == IActionOperation.ERROR) {
						this.tree.failed(this.operationsToTrack[i].getStatus());
						failed = true;
					}
				}
			}
			if (!failed) {
				if (this.source.getType() == IResource.FILE) {
					this.tree.movedFile((IFile) this.source, (IFile) this.destination);
				} else if (this.source.getType() == IResource.FOLDER) {
					this.tree.movedFolderSubtree((IFolder) this.source, (IFolder) this.destination);
				} 				
			}
		}		
	}   
}
