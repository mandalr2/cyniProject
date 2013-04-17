/*
 * #%L
 * Cyni API (cyni-api)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.cytoscape.cyni;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.VirtualColumnInfo;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;


import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;


/**
 * This is a class that contains several methods to help while creating a new network
 * 
 */
public class CyniNetworkUtils  {


	private final CyNetworkTableManager netTableMgr;
	private final CyRootNetworkManager rootNetMgr;
	protected CyNetworkViewFactory viewFactory;
	protected CyNetworkManager netMgr;
	protected CyNetworkViewManager viewMgr;
	protected VisualMappingManager vmMgr;

	/**
	 * Constructor.
	 * 
	 * @param networkViewFactory The network view factory to create a new network view
	 * @param networkManager The network manager
	 * @param networkViewManager The network view manager
	 * @param netTableMgr The network table manager to generate a new network and its table
	 * @param rootNetMgr The root network manager to generate a new root network
	 * @param vmMgr The Visual mapping manager that allows changing the visual style of a network
	 */
	public CyniNetworkUtils( CyNetworkViewFactory networkViewFactory,CyNetworkManager networkManager,CyNetworkViewManager networkViewManager,
			final CyNetworkTableManager netTableMgr,final CyRootNetworkManager rootNetMgr, final VisualMappingManager visualMapperManager) {

		this.viewFactory = networkViewFactory;
		this.viewMgr = networkViewManager;
		this.netMgr = networkManager;
		this.netTableMgr = netTableMgr;
		this.rootNetMgr = rootNetMgr;
		this.vmMgr = visualMapperManager;
		
	}
	
	/**
	 * Add columns that belong to a table associated to a network to a new table associated to a new network.
	 * Basically, allows copying all columns from one table to another one
	 * 
	 * @param origNet The original network
	 * @param newNet The new network
	 * @param origTable The original table
	 * @param tableType The type of the table
	 * @param namespace The namespace of the table
	 */
	public void addColumns(final CyNetwork origNet,final CyNetwork newNet, final CyTable origTable,
		final Class<? extends CyIdentifiable> tableType,final String namespace) {
		CyTable from = origTable; 
		CyTable to = newNet.getTable(tableType, namespace); 
		CyRootNetwork origRoot = null ;
		CyRootNetwork newRoot = rootNetMgr.getRootNetwork(newNet);
		Map<String, CyTable> origRootTables  ;
			
		for (final CyColumn col : from.getColumns())
		{
			final String name = col.getName();
			
			if (to.getColumn(name) == null){
			final VirtualColumnInfo info = col.getVirtualColumnInfo();
			
				if (info.isVirtual() && origNet != null) {
					origRoot = rootNetMgr.getRootNetwork(origNet);
					origRootTables = netTableMgr.getTables(origRoot, tableType);
					if (origRootTables.containsValue(info.getSourceTable())) {
						// If the virtual column is from a root-network table, do NOT set this virtual column directly to
						// the new table:
						// Get the original column (not the virtual one!)
						final CyColumn origCol = info.getSourceTable().getColumn(info.getSourceColumn());
						// Copy the original column to the root-network's table first
						final CyTable newRootTable = newRoot.getTable(tableType, namespace);
						
						if (newRootTable.getColumn(origCol.getName()) == null)
							copyColumn(origCol, newRootTable);
					
						// Now we can add the new "root" column as a virtual one to the new network's table
						to.addVirtualColumn(name, origCol.getName(), newRootTable, CyIdentifiable.SUID, col.isImmutable());
					} else {
						// Otherwise (e.g. virtual column from a global table) just add the virtual column directly
						addVirtualColumn(col, to);
					}
				} else {
					// Not a virtual column, so just copy it to the new network's table
					copyColumn(col, to);
				}
			}
		}
	}
	
	/**
	 * Add a virtual column to a table
	 * 
	 * @param col The column to add.
	 * @param subTable The CyTable to add the column.
	 */
	private void addVirtualColumn(CyColumn col, CyTable subTable){
		VirtualColumnInfo colInfo = col.getVirtualColumnInfo();
		CyColumn checkCol= subTable.getColumn(col.getName());
		
		if (checkCol == null)
			subTable.addVirtualColumn(col.getName(), colInfo.getSourceColumn(), colInfo.getSourceTable(), colInfo.getTargetJoinKey(), true);
		else if (!checkCol.getVirtualColumnInfo().isVirtual() ||
					!checkCol.getVirtualColumnInfo().getSourceTable().equals(colInfo.getSourceTable()) ||
					!checkCol.getVirtualColumnInfo().getSourceColumn().equals(colInfo.getSourceColumn()))
			subTable.addVirtualColumn(col.getName(), colInfo.getSourceColumn(), colInfo.getSourceTable(), colInfo.getTargetJoinKey(), true);
	}

	/**
	 * Copy a column to a table
	 * 
	 * @param col The column to copy
	 * @param subTable The CyTable to add the column.
	 */
	private void copyColumn(CyColumn col, CyTable subTable) {
		if (List.class.isAssignableFrom(col.getType()))
			subTable.createListColumn(col.getName(), col.getListElementType(), false);
		else
			subTable.createColumn(col.getName(), col.getType(), false);	
	}
	
	/**
	 * Clone a row to be added to a new network
	 * 
	 * @param newNet The new network.
	 * @param tableType The type of the table.
	 * @param from The source row
	 * @param to The target row
	 */
	public void cloneRow(final CyNetwork newNet, final Class<? extends CyIdentifiable> tableType, final CyRow from,
			final CyRow to) {
		final CyRootNetwork newRoot = rootNetMgr.getRootNetwork(newNet);
		Map<String, CyTable> rootTables = netTableMgr.getTables(newRoot, tableType);
		
		for (final CyColumn col : to.getTable().getColumns()){
			final String name = col.getName();
			
			if (name.equals(CyIdentifiable.SUID))
				continue;
			
			final VirtualColumnInfo info = col.getVirtualColumnInfo();
			
			// If it's a virtual column whose source table is assigned to the new root-network,
			// then we have to set the value, because the rows of the new root table may not have been copied yet
			if (!info.isVirtual() || rootTables.containsValue(info.getSourceTable()))
				to.set(name, from.getRaw(name));
		}
	}
	
	/**
	 * Checks if the table is associated to a network, if so it return the associated network otherwise it returns null
	 * 
	 * @param table The table to check
	 * @return null if no network is associated to the table or the network associated
	 */
	public CyNetwork getNetworkAssociatedToTable(CyTable table)
	{
		CyNetwork networkFound = null;
		for(CyNetwork net : netMgr.getNetworkSet())
		{
			if(table.equals(net.getDefaultNodeTable()))
			{
				networkFound = net;
			}
		}
		return networkFound;
	}
	
	/**
	 * Remove nodes that do not have any edge
	 * 
	 * @param network The network to remove nodes
	 */
	public void removeNodesWithoutEdges(CyNetwork network)
	{
		ArrayList<CyNode> list = new ArrayList<CyNode>();
		
		for(CyNode node : network.getNodeList())
		{
			if(network.getAdjacentEdgeList(node, CyEdge.Type.ANY).size() == 0)
				list.add(node);	
		}
		if(list.size() > 0)
			network.removeNodes(list);
	}
	
	
	/**
	 * This method displays the new network and return a network view that might be used to modify the display features such as the layout
	 * 
	 * @param newNetwork The new network
	 * @param oldNetwork The old network in case we are using a table associated to a network otherwise it is a null
	 * @param directed Tells whether the new network is a directed graph or not
	 * @return the new network view
	 */
	public CyNetworkView displayNewNetwork(CyNetwork newNetwork,CyNetwork oldNetwork, boolean directed)
	{
		CyNetworkView newNetworkView;
		netMgr.addNetwork(newNetwork);
		newNetworkView = viewFactory.createNetworkView(newNetwork);
		if(oldNetwork != null && viewMgr.getNetworkViews(oldNetwork).size() > 0)
		{
			final VisualStyle style = vmMgr.getVisualStyle(viewMgr.getNetworkViews(oldNetwork).iterator().next());
			style.apply(newNetworkView);
		}
		if(directed)
		{
			for(View<CyEdge> edgeView : newNetworkView.getEdgeViews())
			{
				edgeView.setVisualProperty(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.ARROW);
			}
		}
		newNetworkView.updateView();
		viewMgr.addNetworkView(newNetworkView);
		return newNetworkView;
	}
	
	
}