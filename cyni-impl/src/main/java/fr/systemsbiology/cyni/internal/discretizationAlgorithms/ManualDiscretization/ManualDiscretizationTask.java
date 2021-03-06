/*
 * #%L
 * Cyni Implementation (cyni-impl)
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
package fr.systemsbiology.cyni.internal.discretizationAlgorithms.ManualDiscretization;



import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;
import fr.systemsbiology.cyni.*;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.CyTable;




/**
 * The BasicInduction provides a very simple Induction, suitable as
 * the default Induction for Cytoscape data readers.
 */
public class ManualDiscretizationTask extends AbstractCyniTask {
	
	private  final int bins;
	private final CyTable mytable;
	private final List<String> attributeArray;
	private ArrayList<Double> thresholds;
	private List<String> columnsNames;
	private Component parent;
	
	

	/**
	 * Creates a new BasicInduction object.
	 */
	public ManualDiscretizationTask(final String name, final ManualDiscretizationContext context, CyTable selectedTable)
	{
		super(name, context);
		bins = Integer.parseInt(context.interval.getSelectedValue());
		this.attributeArray = context.attributeList.getSelectedValues();
		parent = context.getParentSwingComponent();
		thresholds = new ArrayList<Double>();
		this.mytable = selectedTable;
		for(int i=1;i<bins;i++)
			thresholds.add(context.mapTh.get("th"+(bins-1)+""+i).getValue());
		
		Collections.sort(thresholds);
	}

	/**
	 *  Perform actual Discretization task.
	 *  This creates the default square Induction.
	 */
	@Override
	final protected void doCyniTask(final TaskMonitor taskMonitor) {
		
		Double progress = 0.0d;
		double  valDouble=0;
		Double step;
		String label = "";
		Object value;
		int pos = 0;
		CyColumn column;
		String newColName;
		
		columnsNames = new ArrayList<String>();
   
        step = 1.0 /  attributeArray.size();
        
        taskMonitor.setTitle("Cyni - Manual Discretization Algorithm ");
        taskMonitor.setStatusMessage("Discretizating data...");
		taskMonitor.setProgress(progress);
		
		
	
		for (final String  columnName : attributeArray)
		 {
			newColName = "nominal."+columnName;
			newColName = getUniqueColumnName(mytable,newColName);
			column = mytable.getColumn(columnName);
			
			mytable.createColumn(newColName, String.class, false);
			columnsNames.add(newColName);
			
			for ( CyRow row : mytable.getAllRows() ) 
			{
					 
				value = row.get(columnName, column.getType());
				
				if(value == null)
					continue;
				
				if(column.getType() == Integer.class)
					valDouble = ((Integer)value).doubleValue();
				else
					valDouble = (Double)value;
					
				pos = thresholds.size();
				for(int i = 0 ; i< thresholds.size()  ; i++ )
				{
					
					if(i==0)
					{
						if(valDouble <= thresholds.get(i)){
							pos = i;
							break;
						}
					}
					else
					{
						if((valDouble > thresholds.get(i-1)) && (valDouble <= thresholds.get(i)))
						{
							pos = i;
							break;
						}
						
					}
				}
				if(pos==0 || pos == thresholds.size())
				{
					if(pos==0)
						label = "(-Infinity," + String.format("%.5g",thresholds.get(pos)) + "]";
					if(pos==thresholds.size())
						label = "(" + String.format("%.5g", thresholds.get(pos-1)) + ",+Infinity )";
				}
				else
					label = "(" + String.format("%.5g", thresholds.get(pos-1)) + "," + String.format("%.5g",thresholds.get(pos)) + "]";
				row.set(newColName, label);
			}
			 
			 progress = progress + step;
			 taskMonitor.setProgress(progress);
		 }

		if(!columnsNames.isEmpty())
		{
			outputMessage = "Discretization successful: " + columnsNames.size() + " new columns created in the chosen table. Their name has the prefix nominal. ";
			taskMonitor.setStatusMessage(outputMessage);
			if(parent != null)
			{
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						JOptionPane.showMessageDialog(parent, outputMessage, "Results", JOptionPane.INFORMATION_MESSAGE);
					}
				});
			}
		}
		
		taskMonitor.setProgress(1.0d);
		
	}
	
	
	private String getUniqueColumnName(CyTable table, final String preferredName) {
		if (table.getColumn(preferredName) == null)
			return preferredName;

		String newUniqueName;
		int i = 0;
		do {
			++i;
			newUniqueName = preferredName + "-" + i;
		} while (table.getColumn(newUniqueName) != null);

		return newUniqueName;
	}
	
	@Override
	public Object getResults(Class requestedType) {
		if(columnsNames == null)
			return "FAILED";
	
		if(requestedType.equals(List.class))
			return columnsNames;
		else if(requestedType.equals(String.class))
			return outputMessage;
		
		
		return "OK";
	}
	
}