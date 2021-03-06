/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Igor Burilo - Initial API and implementation
 *******************************************************************************/
package org.eclipse.team.svn.revision.graph.graphic.figure;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Cursors;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.svn.revision.graph.SVNRevisionGraphMessages;
import org.eclipse.team.svn.revision.graph.SVNRevisionGraphPlugin;
import org.eclipse.team.svn.revision.graph.PathRevision.ReviosionNodeType;
import org.eclipse.team.svn.revision.graph.PathRevision.RevisionNodeAction;
import org.eclipse.team.svn.revision.graph.graphic.ChangedPath;
import org.eclipse.team.svn.revision.graph.graphic.GraphConstants;
import org.eclipse.team.svn.revision.graph.graphic.NodeMergeData;
import org.eclipse.team.svn.revision.graph.graphic.RevisionNode;
import org.eclipse.team.svn.revision.graph.graphic.figure.RevisionTooltipFigure.Range;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Figure for revision node
 * 
 * @author Igor Burilo
 */
public class RevisionFigure extends Figure {
	
	public final static Color TRUNK_COLOR;
	public final static Color BRANCH_COLOR;
	public final static Color TAG_COLOR;	
	public final static Color CREATE_OR_COPY_COLOR;	
	public final static Color RENAME_COLOR;
	public final static Color DELETE_COLOR;	
	public final static Color MODIFY_COLOR;
	public final static Color OTHER_COLOR;
	public final static Color SELECTED_COLOR;
	
	public final static Image TRUNK_IMAGE;
	public final static Image BRANCH_IMAGE;
	public final static Image TAG_IMAGE;
	public final static Image ADD_IMAGE;
	public final static Image DELETE_IMAGE;
	public final static Image MODIFY_IMAGE;
	public final static Image RENAME_IMAGE;
	public final static Image OTHER_IMAGE;
	public final static Image INCOMING_MERGE_IMAGE;
	public final static Image INCOMING_MERGE_IMAGE_PRESSED;
	public final static Image OUTGOING_MERGE_IMAGE;
	public final static Image OUTGOING_MERGE_IMAGE_PRESSED;
	public final static Image MERGE_IMAGE;
	
	protected RevisionNode revisionNode;
	protected String path;
	
	protected Color originalNodeColor;	
	protected Color nodeColor;
	protected Color borderColor;
		
	protected Label revisionFigure;
	protected Label statusFigure;
	protected PathFigure pathFigure;
	protected Label commentFigure;
		
	protected Label outgoingMergeLabel;
	protected boolean isOutgoingMergePressed;
	
	protected Label incomingMergeLabel;
	protected boolean isIncomingMergePressed;
	
	protected boolean isSelected;
	
	static {
		//images
		TRUNK_IMAGE = SVNRevisionGraphPlugin.instance().getImageDescriptor("icons/trunk.gif").createImage(); //$NON-NLS-1$
		SVNRevisionGraphPlugin.disposeOnShutdown(TRUNK_IMAGE);
		
		BRANCH_IMAGE = SVNRevisionGraphPlugin.instance().getImageDescriptor("icons/branch.gif").createImage(); //$NON-NLS-1$
		SVNRevisionGraphPlugin.disposeOnShutdown(BRANCH_IMAGE);
		
		TAG_IMAGE = SVNRevisionGraphPlugin.instance().getImageDescriptor("icons/tag.gif").createImage(); //$NON-NLS-1$
		SVNRevisionGraphPlugin.disposeOnShutdown(TAG_IMAGE);
		
		ADD_IMAGE = SVNRevisionGraphPlugin.instance().getImageDescriptor("icons/add.gif").createImage(); //$NON-NLS-1$
		SVNRevisionGraphPlugin.disposeOnShutdown(ADD_IMAGE);
		
		DELETE_IMAGE = SVNRevisionGraphPlugin.instance().getImageDescriptor("icons/delete.gif").createImage(); //$NON-NLS-1$
		SVNRevisionGraphPlugin.disposeOnShutdown(DELETE_IMAGE);
		
		MODIFY_IMAGE = SVNRevisionGraphPlugin.instance().getImageDescriptor("icons/modify.gif").createImage(); //$NON-NLS-1$
		SVNRevisionGraphPlugin.disposeOnShutdown(MODIFY_IMAGE);
		
		RENAME_IMAGE = SVNRevisionGraphPlugin.instance().getImageDescriptor("icons/rename.gif").createImage(); //$NON-NLS-1$
		SVNRevisionGraphPlugin.disposeOnShutdown(RENAME_IMAGE);
		
		OTHER_IMAGE = SVNRevisionGraphPlugin.instance().getImageDescriptor("icons/other.png").createImage(); //$NON-NLS-1$
		SVNRevisionGraphPlugin.disposeOnShutdown(OTHER_IMAGE);
		
		INCOMING_MERGE_IMAGE = SVNRevisionGraphPlugin.instance().getImageDescriptor("icons/incoming.png").createImage(); //$NON-NLS-1$
		SVNRevisionGraphPlugin.disposeOnShutdown(INCOMING_MERGE_IMAGE);
		
		INCOMING_MERGE_IMAGE_PRESSED = SVNRevisionGraphPlugin.instance().getImageDescriptor("icons/incoming_down.png").createImage(); //$NON-NLS-1$
		SVNRevisionGraphPlugin.disposeOnShutdown(INCOMING_MERGE_IMAGE_PRESSED);
		
		OUTGOING_MERGE_IMAGE = SVNRevisionGraphPlugin.instance().getImageDescriptor("icons/outgoing.png").createImage(); //$NON-NLS-1$
		SVNRevisionGraphPlugin.disposeOnShutdown(OUTGOING_MERGE_IMAGE);
		
		OUTGOING_MERGE_IMAGE_PRESSED = SVNRevisionGraphPlugin.instance().getImageDescriptor("icons/outgoing_down.png").createImage(); //$NON-NLS-1$
		SVNRevisionGraphPlugin.disposeOnShutdown(OUTGOING_MERGE_IMAGE_PRESSED);
		
		MERGE_IMAGE = SVNRevisionGraphPlugin.instance().getImageDescriptor("icons/merge.gif").createImage(); //$NON-NLS-1$
		SVNRevisionGraphPlugin.disposeOnShutdown(MERGE_IMAGE);
		
		//colors
		TRUNK_COLOR = new Color(UIMonitorUtility.getDisplay(), 188, 255, 188);
		SVNRevisionGraphPlugin.disposeOnShutdown(TRUNK_COLOR);
		
		BRANCH_COLOR = new Color(UIMonitorUtility.getDisplay(), 229, 255, 229);
		SVNRevisionGraphPlugin.disposeOnShutdown(BRANCH_COLOR);
		
		TAG_COLOR = new Color(UIMonitorUtility.getDisplay(), 239, 252, 162);
		SVNRevisionGraphPlugin.disposeOnShutdown(TAG_COLOR);
		
		CREATE_OR_COPY_COLOR = new Color(UIMonitorUtility.getDisplay(), 229, 255, 229);
		SVNRevisionGraphPlugin.disposeOnShutdown(CREATE_OR_COPY_COLOR);
		
		RENAME_COLOR = new Color(UIMonitorUtility.getDisplay(), 229, 229, 255);
		SVNRevisionGraphPlugin.disposeOnShutdown(RENAME_COLOR);
		
		DELETE_COLOR = new Color(UIMonitorUtility.getDisplay(), 255, 229, 229);
		SVNRevisionGraphPlugin.disposeOnShutdown(DELETE_COLOR);
		
		MODIFY_COLOR = new Color(UIMonitorUtility.getDisplay(), 229, 229, 229);
		SVNRevisionGraphPlugin.disposeOnShutdown(MODIFY_COLOR);
		
		OTHER_COLOR = new Color(UIMonitorUtility.getDisplay(), 240, 240, 240);
		SVNRevisionGraphPlugin.disposeOnShutdown(OTHER_COLOR);
		
		SELECTED_COLOR = new Color(UIMonitorUtility.getDisplay(), 47, 104, 200);
		SVNRevisionGraphPlugin.disposeOnShutdown(SELECTED_COLOR);
	}
	
	public RevisionFigure(RevisionNode revisionNode, String path) {
		this.revisionNode = revisionNode;
		this.path = path;
		
		this.createControls();								
		this.initControls();
		
		//non-transparent
		this.setOpaque(true);
		
		//set border in order to change client area because of shadow
		this.setBorder(new MarginBorder(0, 0, GraphConstants.NODE_SHADOW_OFFSET, GraphConstants.NODE_SHADOW_OFFSET));
	}
	
	protected void createControls() {
		GridLayout layout = new GridLayout();		
		//layout.marginHeight = layout.marginWidth = 2;
		//layout.horizontalSpacing = layout.verticalSpacing = 3; 		
		this.setLayoutManager(layout);
							
		Figure revisionParent = new Figure();
		this.add(revisionParent);		
		FlowLayout revisionLayout = new FlowLayout(true);
		revisionParent.setLayoutManager(revisionLayout);
		GridData data = new GridData();
		layout.setConstraint(revisionParent, data);
		
		this.revisionFigure = new Label();
		revisionParent.add(this.revisionFigure);		
		Font boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
		this.revisionFigure.setFont(boldFont);		
		
		//status
		this.statusFigure = new Label();
		revisionParent.add(this.statusFigure);		
		
		//merge
		if (this.revisionNode.hasIncomingMerges() || this.revisionNode.hasOutgoingMerges()) {
			Figure mergeParent = new Figure();
			this.add(mergeParent);		
			FlowLayout mergeParentLayout = new FlowLayout(true);
			mergeParent.setLayoutManager(mergeParentLayout);
			data = new GridData();
			layout.setConstraint(mergeParent, data);						
			
			Label mergeTextLabel = new Label(SVNRevisionGraphMessages.RevisionFigure_Merges);
			mergeParent.add(mergeTextLabel);						
			mergeTextLabel.setIcon(MERGE_IMAGE);
			mergeTextLabel.setFont(boldFont);							
			
			//incoming
			if (this.revisionNode.hasIncomingMerges()) {
				this.incomingMergeLabel = new Label();
				mergeParent.add(this.incomingMergeLabel);			
				this.incomingMergeLabel.setIcon(INCOMING_MERGE_IMAGE);
				this.incomingMergeLabel.setCursor(Cursors.HAND);
				this.incomingMergeLabel.addMouseListener(new MouseListener.Stub() {
					public void mousePressed(MouseEvent me) {
						if (RevisionFigure.this.isIncomingMergePressed) {
							RevisionFigure.this.revisionNode.removeAllIncomingMergeConnections();
							RevisionFigure.this.incomingMergeLabel.setIcon(INCOMING_MERGE_IMAGE);
							RevisionFigure.this.isIncomingMergePressed = false;
						} else {
							RevisionFigure.this.revisionNode.addAllIncomingMergeConnections();
							RevisionFigure.this.incomingMergeLabel.setIcon(INCOMING_MERGE_IMAGE_PRESSED);
							RevisionFigure.this.isIncomingMergePressed = true;
						}
					}
				});
			}			
			
			//outgoing
			if (this.revisionNode.hasOutgoingMerges()) {
				this.outgoingMergeLabel = new Label();
				mergeParent.add(this.outgoingMergeLabel);				
				this.outgoingMergeLabel.setIcon(OUTGOING_MERGE_IMAGE);
				this.outgoingMergeLabel.setCursor(Cursors.HAND);
				this.outgoingMergeLabel.addMouseListener(new MouseListener.Stub() {
					public void mousePressed(MouseEvent me) {
						if (RevisionFigure.this.isOutgoingMergePressed) {
							RevisionFigure.this.revisionNode.removeAllOutgoingMergeConnections();
							RevisionFigure.this.outgoingMergeLabel.setIcon(OUTGOING_MERGE_IMAGE);
							RevisionFigure.this.isOutgoingMergePressed = false;
						} else {
							RevisionFigure.this.revisionNode.addAllOutgoingMergeConnections();
							RevisionFigure.this.outgoingMergeLabel.setIcon(OUTGOING_MERGE_IMAGE_PRESSED);
							RevisionFigure.this.isOutgoingMergePressed = true;
						}
					}
				});
			}
		}
				
		//path
		if (this.revisionNode.getAction() == RevisionNodeAction.ADD || 
			this.revisionNode.getAction() == RevisionNodeAction.COPY ||
			this.revisionNode.getAction() == RevisionNodeAction.RENAME) {

			this.pathFigure = new PathFigure();
			data = new GridData();
			data.widthHint = GraphConstants.NODE_WIDTH - 10;
			layout.setConstraint(this.pathFigure, data);
			this.add(this.pathFigure);
		}									
		
		//comment		
		String comment = this.revisionNode.getMessage();
		if (comment != null && comment.length() > 0) {
			this.commentFigure = new Label();
			this.add(commentFigure);
			data = new GridData();
			data.widthHint = GraphConstants.NODE_WIDTH - 10;
			data.horizontalAlignment = SWT.BEGINNING;							
			layout.setConstraint(this.commentFigure, data);
			this.commentFigure.setLabelAlignment(PositionConstants.LEFT);			
		}
	}			

	@Override
	protected void paintFigure(Graphics g) {
		super.paintFigure(g);		
		
		final Dimension corner = new Dimension(8, 8);		
		final Rectangle initialBounds = getBounds();		
		final int lineWidth = 1;
		//g.drawRectangle(initialBounds);		
		
		//shadow		
		Rectangle shadowBounds = initialBounds.getCopy();
		shadowBounds.resize(-GraphConstants.NODE_SHADOW_OFFSET, - GraphConstants.NODE_SHADOW_OFFSET)
			.translate(GraphConstants.NODE_SHADOW_OFFSET, GraphConstants.NODE_SHADOW_OFFSET);
		g.setBackgroundColor(ColorConstants.gray);		
		g.fillRoundRectangle(shadowBounds, corner.width, corner.height);							
		
		//main		
		g.setLineWidthFloat(lineWidth);
		
		Rectangle mainBounds = initialBounds.getCopy();
		mainBounds.resize(-GraphConstants.NODE_SHADOW_OFFSET, -GraphConstants.NODE_SHADOW_OFFSET);
										
		g.setBackgroundColor(this.nodeColor);		
		g.setForegroundColor(this.borderColor);
		
		g.fillRoundRectangle(mainBounds, corner.width, corner.height);			
		this.drawOutline(g, corner, mainBounds, lineWidth);	
		
		//set color for children
 		Iterator<?> iter = this.getChildren().iterator();
 		while (iter.hasNext()) {
 			IFigure child = (IFigure) iter.next();
 			if (child == this.commentFigure) {
 				//set explicitly color for comment
				Color color = ColorConstants.gray; 
				if (this.isSelected) {
					color = FigureUtilities.lighter(color);
				}
				child.setForegroundColor(color);
 			} else if (child == this.pathFigure) { 
 				this.pathFigure.setSelected(this.isSelected);
 			} else {
 				Color color = ColorConstants.black;
				if (this.isSelected) {
					color = RevisionFigure.inverseColor(color);
				}
				child.setForegroundColor(color);
 			}
 		}
	}
	
	protected void drawOutline(Graphics graphics, Dimension corner, Rectangle bounds, int lineWidth) {
	    float lineInset = Math.max(1.0f, lineWidth) / 2.0f;
	    int inset1 = (int)Math.floor(lineInset);
	    int inset2 = (int)Math.ceil(lineInset);
	
	    Rectangle r = Rectangle.SINGLETON.setBounds(bounds);
	    r.x += inset1 ; 
	    r.y += inset1; 
	    r.width -= inset1 + inset2;
	    r.height -= inset1 + inset2;
		
		graphics.drawRoundRectangle(r, Math.max(0, corner.width - (int)lineInset), Math.max(0, corner.height - (int)lineInset));
	} 
	
	protected void initControls() {
		this.revisionFigure.setText(String.valueOf(this.revisionNode.getRevision()));
				
		this.statusFigure.setText(RevisionFigure.getRevisionNodeStatusText(this.revisionNode));
		
		if (this.pathFigure != null) {
			String parentPath = null;
			RevisionNode copiedFrom = this.revisionNode.getCopiedFrom();
			if (copiedFrom != null) {
				parentPath = copiedFrom.getPath();
			}						
			this.pathFigure.setShowFullPath(!this.revisionNode.isTruncatePath());
			this.pathFigure.setPaths(this.path, parentPath);
		}				
		
		if (this.commentFigure != null) {
			String comment = this.revisionNode.getMessage();					
			comment = comment.replaceAll("\r\n|\r|\n", " "); //$NON-NLS-1$ //$NON-NLS-2$
			this.commentFigure.setText(comment);
		}		 						
		
		//init color and node icon
	    Image nodeIcon = RevisionFigure.getRevisionNodeIcon(this.revisionNode);	    						
		this.revisionFigure.setIcon(nodeIcon);
		
		this.originalNodeColor = RevisionFigure.getRevisionNodeColor(this.revisionNode);
		this.nodeColor = this.originalNodeColor;
		this.borderColor = RevisionFigure.getRevisionNodeBorderColor(this.revisionNode);		
		
		//merges
		
		/*
		 * show number of merges and number of revisions
		 * in these merges, e.g. 2 (40 revs) 
		 */
		if (this.revisionNode.hasIncomingMerges()) {
			StringBuilder str = new StringBuilder();
			NodeMergeData[] md = this.revisionNode.getIncomingMerges();
			str.append(md.length).append(" ("); //$NON-NLS-1$
			int revsCount = 0;
			for (NodeMergeData data : md) {
				revsCount += data.getRevisionsCount();
			}
			str.append(revsCount).append(" ").append(SVNRevisionGraphMessages.RevisionFigure_Revisions).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
			this.incomingMergeLabel.setText(str.toString());
		}
		//shown only number of merges
		if (this.revisionNode.hasOutgoingMerges()) {
			NodeMergeData[] md = this.revisionNode.getOutgoingMerges();
			int revsCount = 0;
			for (NodeMergeData data : md) {
				revsCount += data.getRevisionsCount();
			}
			String str = String.valueOf(revsCount);
			this.outgoingMergeLabel.setText(str);
		}		
	}
	
	public void init() {
		this.setPreferredSize(GraphConstants.NODE_WIDTH, this.getPreferredSize().height);
	}
	
	public void changeMerges() {
		if (this.isIncomingMergePressed && !this.revisionNode.hasIncomingMergeConnections()) {
			this.incomingMergeLabel.setIcon(INCOMING_MERGE_IMAGE);
			this.isIncomingMergePressed = false;
		}
		
		if (this.isOutgoingMergePressed && !this.revisionNode.hasOutgoingMergeConnections()) {
			this.outgoingMergeLabel.setIcon(OUTGOING_MERGE_IMAGE);
			this.isOutgoingMergePressed = false;
		}
	}
	
	public void changeTruncatePath() {		
		if (this.pathFigure != null) {			
			this.pathFigure.setShowFullPath(!this.revisionNode.isTruncatePath());
		}		
	}
	
	public void setSelected(boolean isSelected) {
		if (this.isSelected != isSelected) {
			this.isSelected = isSelected;
			 
			if (this.isSelected) {
	 			this.nodeColor = SELECTED_COLOR;
	 		} else {
	 			this.nodeColor = this.originalNodeColor;
	 		}
			
			this.repaint();
 		}
	}
	
	public RevisionNode getRevisionNode() {
		return this.revisionNode;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#invalidate()
	 */
	@Override
	public void invalidate() {
		//clear preferred size as path figure can change its bounds depending on truncation mode
		this.prefSize = null;		
		super.invalidate();
	}
	
	public static Color inverseColor(Color color) {
		return new Color(null, 255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue());		
	}
	
	public static Color getRevisionNodeBorderColor(RevisionNode revisionNode) {
	    ReviosionNodeType type = revisionNode.getType();
		RevisionNodeAction action = revisionNode.getAction();		
	    Color color;	   	    
		if (ReviosionNodeType.TRUNK.equals(type)) {
			color = ColorConstants.green;			
		} else if (ReviosionNodeType.BRANCH.equals(type)) {
			color = ColorConstants.green;
		} else if (ReviosionNodeType.TAG.equals(type)) {
			color = ColorConstants.black;
		} else if (RevisionNodeAction.ADD.equals(action) || RevisionNodeAction.COPY.equals(action)) {
			color = ColorConstants.green;
		} else if (RevisionNodeAction.RENAME.equals(action)) {
			color = ColorConstants.blue;
		} else if (RevisionNodeAction.DELETE.equals(action)) {
			color = ColorConstants.red;
		} else if (RevisionNodeAction.MODIFY.equals(action)) {
			color = ColorConstants.black;
		} else {
			color = ColorConstants.black;
		}
		return color;
	}
	
	public static String getRevisionNodeStatusText(RevisionNode revisionNode) {
	    ReviosionNodeType type = revisionNode.getType();
		RevisionNodeAction action = revisionNode.getAction();		
	    String text = "[";	   	     //$NON-NLS-1$
		if (ReviosionNodeType.TRUNK.equals(type)) {
			text += SVNRevisionGraphMessages.RevisionFigure_Trunk;
		} else if (ReviosionNodeType.BRANCH.equals(type)) {
			text += SVNRevisionGraphMessages.RevisionFigure_Branch;
		} else if (ReviosionNodeType.TAG.equals(type)) {
			text += SVNRevisionGraphMessages.RevisionFigure_Tag;
		} else if (RevisionNodeAction.ADD.equals(action)) {
			text += SVNRevisionGraphMessages.RevisionFigure_Create;
		} else if (RevisionNodeAction.COPY.equals(action)) {
			text += SVNRevisionGraphMessages.RevisionFigure_Copy;
		} else if (RevisionNodeAction.RENAME.equals(action)) {
			text += SVNRevisionGraphMessages.RevisionFigure_Rename;
		} else if (RevisionNodeAction.DELETE.equals(action)) {
			text += SVNRevisionGraphMessages.RevisionFigure_Delete;
		} else if (RevisionNodeAction.MODIFY.equals(action)) {
			text += SVNRevisionGraphMessages.RevisionFigure_Edit;
		} else {
			text += SVNRevisionGraphMessages.RevisionFigure_NoChanges;
		}
		text += "]"; //$NON-NLS-1$
		return text;
	}
	
	public static Color getRevisionNodeColor(RevisionNode revisionNode) {
	    ReviosionNodeType type = revisionNode.getType();
		RevisionNodeAction action = revisionNode.getAction();		
	    Color color;	   	    
		if (ReviosionNodeType.TRUNK.equals(type)) {
			color = TRUNK_COLOR;			
		} else if (ReviosionNodeType.BRANCH.equals(type)) {
			color = BRANCH_COLOR;
		} else if (ReviosionNodeType.TAG.equals(type)) {
			color = TAG_COLOR;
		} else if (RevisionNodeAction.ADD.equals(action) || RevisionNodeAction.COPY.equals(action)) {
			color = CREATE_OR_COPY_COLOR;
		} else if (RevisionNodeAction.RENAME.equals(action)) {
			color = RENAME_COLOR;
		} else if (RevisionNodeAction.DELETE.equals(action)) {
			color = DELETE_COLOR;
		} else if (RevisionNodeAction.MODIFY.equals(action)) {
			color = MODIFY_COLOR;
		} else {
			color = OTHER_COLOR;
		}
		return color;
	}
	
	public static Image getRevisionNodeIcon(RevisionNode revisionNode) {
	    ReviosionNodeType type = revisionNode.getType();
		RevisionNodeAction action = revisionNode.getAction();			    
	    Image nodeIcon = null;
		if (ReviosionNodeType.TRUNK.equals(type)) {			
			nodeIcon = TRUNK_IMAGE;
		} else if (ReviosionNodeType.BRANCH.equals(type)) {
			nodeIcon = BRANCH_IMAGE;
		} else if (ReviosionNodeType.TAG.equals(type)) {
			nodeIcon = TAG_IMAGE;
		} else if (RevisionNodeAction.ADD.equals(action) || RevisionNodeAction.COPY.equals(action)) {
			nodeIcon = ADD_IMAGE;
		} else if (RevisionNodeAction.RENAME.equals(action)) {
			nodeIcon = RENAME_IMAGE;
		} else if (RevisionNodeAction.DELETE.equals(action)) {
			nodeIcon = DELETE_IMAGE;
		} else if (RevisionNodeAction.MODIFY.equals(action)) {
			nodeIcon = MODIFY_IMAGE;
		} else {
			nodeIcon = OTHER_IMAGE;
		}
		return nodeIcon;
	}
	
	public static String getActionAsString(RevisionNodeAction action) {
		switch (action) {
			case ADD:
				return SVNRevisionGraphMessages.RevisionFigure_ActionAdd;
			case DELETE:
				return SVNRevisionGraphMessages.RevisionFigure_ActionDelete;
			case MODIFY:
				return SVNRevisionGraphMessages.RevisionFigure_ActionModify;
			case COPY:
				return SVNRevisionGraphMessages.RevisionFigure_ActionCopy;
			case RENAME:
				return SVNRevisionGraphMessages.RevisionFigure_ActionRename;
			case NONE:
				return SVNRevisionGraphMessages.RevisionFigure_ActionNone;
			default:
				return null;
		}
	}
	
	public static String getChangedPathsAsString(RevisionNode node) {
		StringBuilder str = new StringBuilder();
		ChangedPath[] changedPaths = node.getChangedPaths();
		for (int i = 0; i < changedPaths.length; i ++) {
			ChangedPath changedPath = changedPaths[i];
			str.append(changedPath.action).append(" ").append(changedPath.path); //$NON-NLS-1$
			if (changedPath.copiedFromPath != null) {
				str.append(" copied from: " + changedPath.copiedFromPath + "@" + changedPath.copiedFromRevision); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (i != changedPaths.length - 1) {
				str.append("\n"); //$NON-NLS-1$
			}
		}
		return str.toString();
	}
	
	public static String getIncomingMergesAsString(RevisionNode node) {
		if (node.hasIncomingMerges()) {
			StringBuilder str = new StringBuilder();
			NodeMergeData[] mergedData = node.getIncomingMerges();
						
			/*
			 * As there can be many revisions then we sort and
			 * group them and limit revisions number in one row
			 */
			
			//as first row contains path, then revisions count should be less
			final int maxRevisionsInFirstRow = 5;
			final int maxRevisionsInRow = 15;
								
			for (int i = 0; i < mergedData.length; i ++) {
				str.append(mergedData[i].path).append(": "); //$NON-NLS-1$
				
				boolean isFirstRow = true;
				List<Range> ranges = Range.getRanges(mergedData[i].getRevisions());
				for (int j = 0, n = ranges.size(); j < n; j ++) {
					str.append(ranges.get(j));
																				
					if (j != n - 1) {
						str.append(","); //$NON-NLS-1$
						
						if (isFirstRow && ((j + 1) % maxRevisionsInFirstRow == 0)) {
							str.append("\n"); //$NON-NLS-1$
							isFirstRow = false;
						} else if (!isFirstRow && ((j + 1) % maxRevisionsInRow == 0)) {
							str.append("\n"); //$NON-NLS-1$
						}
					}										
				}												
				
				if (i != mergedData.length - 1) {
					str.append("\n"); //$NON-NLS-1$
				}
			}	
			return str.toString();
		} else {
			return null;
		}
	}
	
	public static String getOutgoingMergesAsString(RevisionNode node) {
		if (node.hasOutgoingMerges()) {
			StringBuilder str = new StringBuilder();
			NodeMergeData[] mergedData = node.getOutgoingMerges();
			for (int i = 0; i < mergedData.length; i ++) {
				str.append(mergedData[i].path).append(": "); //$NON-NLS-1$
				
				long[] revisions = mergedData[i].getRevisions();
				Arrays.sort(revisions);
				for (int j = 0, n = revisions.length; j < n; j ++) {
					str.append(revisions[j]);
					if (j != n - 1) {
						str.append(","); //$NON-NLS-1$
					}
				}
								
				if (i != mergedData.length - 1) {
					str.append("\n"); //$NON-NLS-1$
				}
			}												
			return str.toString(); 
		} else {
			return null;
		}			
	}
}
