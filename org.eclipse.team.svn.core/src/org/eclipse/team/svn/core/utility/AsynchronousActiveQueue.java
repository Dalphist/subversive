package org.eclipse.team.svn.core.utility;

import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;

public class AsynchronousActiveQueue<Data extends IQueuedElement<Data>> {
	
	public static interface IRecordHandler<Data extends IQueuedElement<Data>>{
		public void process(IProgressMonitor monitor, IActionOperation op, Data record);
	}
	
	protected final String name;
	protected final LinkedList<Data> queue;
	protected final IRecordHandler<Data> handler;
	protected final boolean system;
	
	static final boolean DEBUG = SVNTeamPlugin.instance().isDebugging();

	public AsynchronousActiveQueue(String queueName, IRecordHandler<Data> handler, boolean system) {
		this.name = queueName;
		this.queue = new LinkedList<Data>();
		this.handler = handler;
		this.system = system;
	}
	
	public void push(Data data) {
		synchronized (queue) {
			// avoid duplicated events, Start search from the end, the possibility
			// to find similar events added recently is higher
			if(!queue.isEmpty() && data.canSkip()){
				for (int i = queue.size() - 1; i >= 0; i--) {
					Data old = queue.get(i);
					if(old.equals(data)){
						if(DEBUG){
							logDebug("skipped: " + data);
						}
						return;
					}
				}
			}
			if(queue.size() > 1){
				// try to merge with all except the first one, which could be
				// being dispatched right now
				for (int i = queue.size() - 1; i > 0; i--) {
					Data old = queue.get(i);
					if(old.canMerge(data)){
						queue.set(i, old.merge(data));
						if(DEBUG){
							logDebug("merged " + old + " with " + data);
						}
						return;
					}
				}
			}
			queue.add(data);
			if(DEBUG){
				logDebug("added " + data);
			}
	    	if (queue.size() == 1) {
				ProgressMonitorUtility.doTaskScheduledDefault(new QueuedOperation(name), system);
	    	}
		}		
	}
	
	private final class QueuedOperation extends AbstractActionOperation {
		private QueuedOperation(String operationName) {
			super(operationName, SVNMessages.class);
		}

		@Override
		public ISchedulingRule getSchedulingRule() {
			return null;
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			while (true) {
				Data record;
				synchronized (queue) {
					if (monitor.isCanceled() || queue.isEmpty()) {
						queue.clear();
						break;
					}
					record = queue.get(0);
				}
				handler.process(monitor, this, record);
				if(DEBUG){
					logDebug("processed " + record);
				}
				synchronized (queue) {
					queue.remove(0);
					if(queue.isEmpty()) {
						break;
					}
				}
			}
		}
	}
	
	private void logDebug(String message){
		if(DEBUG){
			System.out.println("[" +name + "] size: " + queue.size() + ", " + message);
		}
	}
}
