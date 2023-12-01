/**
 * Program Name: ProblemTableModel.java
 * Purpose: TODO
 * Coder: Jason Benoit 0885941
 * Date: Nov 29, 2023
 */

import java.util.LinkedList;

import javax.swing.table.AbstractTableModel;

public class ProblemTableModel extends AbstractTableModel
{
	
	private static final long serialVersionUID = 1L;
	
	private static final String[] columnNames = {"Page", "Link", "Status", "Message"};
	private LinkedList<String[]> list; 
	
	public ProblemTableModel()
	{
		list = new LinkedList<String[]>();
	}
	
	public void addEntry(String[] entry) {
    // Adds the element in the last position in the list
    list.add(entry);
    fireTableRowsInserted(list.size()-1, list.size()-1);
}

	@Override
	public int getRowCount()
	{
		// TODO Auto-generated method stub
		return list.size();
	}

	@Override
	public int getColumnCount()
	{
		// TODO Auto-generated method stub
		return columnNames.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (columnIndex >= 0 && columnIndex < 4)
		{
			return list.get(rowIndex)[columnIndex];
		}
		return null;
	}
	
	@Override
	public String getColumnName(int col) {
	    return columnNames[col];
	}
	
	public void clear()
	{
		if (list.size() == 0)
		{
			return;
		}
		int n = list.size();
		list.clear();
	  fireTableRowsDeleted(0, n);
	}

}
//end class