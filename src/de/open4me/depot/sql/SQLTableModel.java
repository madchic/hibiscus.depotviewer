package de.open4me.depot.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.table.AbstractTableModel;

public class SQLTableModel extends AbstractTableModel{
	
	private ResultSet ret;
	
    public SQLTableModel(ResultSet ret) {
		super();
		this.ret = ret;
	}


    @Override
    public String getColumnName(int column) {
    	try {
			return ret.getMetaData().getColumnName(column+1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	return "";
    }

    /**
     *  Returns <code>Object.class</code> regardless of <code>columnIndex</code>.
     *
     *  @param columnIndex  the column being queried
     *  @return the Object.class
     */
    public Class<?> getColumnClass(int columnIndex) {
    	try {
			return Class.forName(ret.getMetaData().getColumnClassName(columnIndex+1));
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
    	return super.getColumnClass(columnIndex);
    }

	/**
	 * 
	 */
	private static final long serialVersionUID = -4430435410621583094L;

	@Override
	public int getRowCount() {
		try {
			ret.last();
			//System.out.println("ROWS: "+ret.getRow());
			return ret.getRow();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return 0;
//		try {
//			System.out.println("Row Count: "+ret.getFetchSize());
//			return ret.getFetchSize();
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return 0;
	}

	@Override
	public int getColumnCount() {
		try {
			//System.out.println("Col Count: "+ret.getMetaData().getColumnCount());
			return ret.getMetaData().getColumnCount();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		try {
			int relative=rowIndex-(ret.getRow()-1);
			ret.relative(relative);
			return ret.getObject(columnIndex+1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
