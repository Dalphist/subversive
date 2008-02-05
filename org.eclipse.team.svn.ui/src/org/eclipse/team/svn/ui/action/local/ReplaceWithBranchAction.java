/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.action.local;

/**
 * Replace with branch action implementation
 * 
 * @author Alexei Goncharov
 */
public class ReplaceWithBranchAction extends ReplaceWithBranchTagAction {
	
	public ReplaceWithBranchAction() {
		super(CompareWithBranchTagAction.BRANCH_OPERATED);
	}
}