package de.open4me.depot.jfreechart;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jfree.data.Range;
import org.jfree.data.RangeInfo;
import org.jfree.data.general.Dataset;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.util.Log;

import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.util.ApplicationException;

public class BestandsEntwicklungXyDataset extends AbstractXYDataset
implements XYDataset, TableXYDataset, RangeInfo {
	
	/*
	 select depotviewer_kurse.wpid,kursdatum,depotviewer_kurse.kurs,anzahl,kosten+transaktionskosten FROM depotviewer_kurse left join depotviewer_umsaetze on depotviewer_kurse.wpid = depotviewer_umsaetze.wpid AND depotviewer_kurse.kursdatum=depotviewer_umsaetze.buchungsdatum where depotviewer_kurse.kursdatum>=(select min(buchungsdatum) from depotviewer_umsaetze) order by kursdatum,wpid;
	 */

	private static final String QUERY_VALUE_PROGRESS = "select kursdatum,depotviewer_kurse.wpid,depotviewer_kurse.kurs,anzahl,kosten+transaktionskosten as kosten,depotviewer_umsaetze.kontoid "+
	        "FROM depotviewer_kurse left join depotviewer_umsaetze on "+
			"depotviewer_kurse.wpid = depotviewer_umsaetze.wpid AND depotviewer_kurse.kursdatum=depotviewer_umsaetze.buchungsdatum "+
	        "where depotviewer_kurse.kursdatum>=(select min(buchungsdatum) from depotviewer_umsaetze)";
	        static final String QUERY_ORDER_PROGRESS = " order by kursdatum,wpid;"; 

	/** The database connection. */
	private transient Connection connection;


	/** Column names. */
	private String[] columnNames = {};

	/** Rows. */
	private ArrayList<Number[]> rows;

	/** The maximum y value of the returned result set */
	private double maxValue = 0.0;

	/** The minimum y value of the returned result set */
	private double minValue = 0.0;
	
	private final int kontoID;
	

	public BestandsEntwicklungXyDataset(Connection connection, int kontoID) {
		super();
		this.connection = connection;
		this.kontoID = kontoID;
	}

	
	/**
	 * ExecuteQuery will attempt execute the query passed to it against the
	 * existing database connection.  If no connection exists then no action
	 * is taken.
	 *
	 * The results from the query are extracted and cached locally, thus
	 * applying an upper limit on how many rows can be retrieved successfully.

	 *
	 * @throws SQLException if there is a problem executing the query.
	 * @throws ApplicationException 
	 */
	public void executeQuery() throws SQLException, ApplicationException {
		executeQuery(this.connection);
	}

	/**
	 * ExecuteQuery will attempt execute the query passed to it against the
	 * provided database connection.  If connection is null then no action is
	 * taken.
	 *
	 * The results from the query are extracted and cached locally, thus
	 * applying an upper limit on how many rows can be retrieved successfully.
	 *

	 * @param  con  the connection the query is to be executed against.
	 *
	 * @throws SQLException if there is a problem executing the query.
	 * @throws ApplicationException 
	 */
	public void executeQuery(Connection con)
			throws SQLException, ApplicationException {

		if (con == null) {
			throw new SQLException(
					"There is no database to execute the query."
					);
		}

		ResultSet resultSet = null;
		Statement statement = null;
		try {
			Map<String,BestandsItem> wertpapiere = new HashMap<>();
			int count=1;

			for (GenericObjectSQL o: SQLUtils.getResultSet("select * from depotviewer_wertpapier", "depotviewer_wertpapier", "id")) {
				try {
					wertpapiere.put(o.getID(),new BestandsItem(++count, o.getAttribute("wertpapiername")));
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
			columnNames = new String[wertpapiere.size()+1];
			columnNames[0]="Alles";
			wertpapiere.forEach((k,v)->columnNames[v.getId()-1]=v.getName());

			statement = con.createStatement();
			System.out.println(QUERY_VALUE_PROGRESS+QUERY_ORDER_PROGRESS);
			resultSet = statement.executeQuery(QUERY_VALUE_PROGRESS+QUERY_ORDER_PROGRESS);

			int numberOfColumns = wertpapiere.size()+3;
			
			Long old=Long.MIN_VALUE;
			Number[] values = null;
			rows = new ArrayList<Number[]>(resultSet.getFetchSize());
			// Get all rows.
			// rows = new ArrayList();
			this.maxValue = Double.NEGATIVE_INFINITY;
			this.minValue = 0;
			boolean kontoFirst=false;
			while (resultSet.next()) {
				Object xKonto = resultSet.getObject("kontoid");
				if((xKonto==null && kontoFirst) || (xKonto!=null && ((Integer)xKonto).intValue() == kontoID ) ){
					kontoFirst=true;
					Object xObject = resultSet.getObject("kursdatum");
					Long date = new Long(((Date) xObject).getTime());
					if(date.longValue()!=old.longValue()) {
						addRow(values,wertpapiere);
						values=new Number[numberOfColumns];
						values[0]=date;
	
						old=date;
					}
					String wpid=resultSet.getObject("wpid").toString();
					BestandsItem item = wertpapiere.get(wpid); 
					if(item!=null){
						Number anzahl = (Number) resultSet.getObject("anzahl");
						Number kosten = (Number) resultSet.getObject("kosten");
						if(anzahl!=null) {
							item.addAnzahl(anzahl.doubleValue());
						}
						if(kosten!=null) {
							item.addKosten(kosten.doubleValue());
						}
						if(item.isRelevant()) {
						  double w =item.gewinnRelative(((Number) resultSet.getObject("kurs")).doubleValue())*100;
						  if(w>maxValue) {
							  maxValue=w;
						  }
						  if(w<minValue) {
							  minValue=w;
						  }
						  values[item.getId()]=w;
						}
					} else {
						Log.warn("Undefined Wertpappier "+resultSet.getObject("wpid"));
					}
				}
				
			}
			addRow(values,wertpapiere);			

			/// a kludge to make everything work when no rows returned
			if (this.rows.isEmpty()) {
				this.rows.add(new Number[numberOfColumns]);
			}


			fireDatasetChanged(); // Tell the listeners a new table has arrived.
		}
		finally {
			if (resultSet != null) {
				try {
					resultSet.close();
				}
				catch (Exception e) {
					// TODO: is this a good idea?
				}
			}
			if (statement != null) {
				try {
					statement.close();
				}
				catch (Exception e) {
					// TODO: is this a good idea?
				}
			}
		}

	}
	
	
	private void addRow(Number[] values, Map<String,BestandsItem> wertpapiere) {
		if(values!=null) {
			double wert=0;
			double kosten=0;
			for( BestandsItem e : wertpapiere.values()) {
				if(e.isRelevant()) {
				  wert+=e.getWert();
				  kosten+=e.getKosten();
				}
			}
			values[1]=(wert*100/kosten*-1)-100;
			values[values.length-1]=wert+kosten;
			rows.add(values);
		}
	}
	
	private static Number DEFAULT_VALUE=0;
	
	public Number getValue(int colIndex, int itemIndex) {
		Number res=this.rows.get(itemIndex)[colIndex];
		if(res==null) {
			res=interpolate(itemIndex,colIndex);
		}
//		return res!=null?res:DEFAULT_VALUE;
		return res;
	}

	private Number interpolate(int itemIndex, int colIndex) {
		Number first =null;
		Number last=null;
		int fStep=0;
		int lStep=0;
		for(int i=itemIndex;--i>=0;) {
			fStep++;
			first=this.rows.get(i)[colIndex];
			if(first!=null) {
				break;
			}
		}
		for(int i=itemIndex;++i<rows.size();) {
			lStep++;
			last=this.rows.get(i)[colIndex];
			if(last!=null) {
				break;
			}
		}
		if(last!=null && first!=null) {
			return (last.doubleValue()*fStep+first.doubleValue()*lStep)/(lStep+fStep);
		}
		if(first!=null) {
			return first;
		}
		
		return null;
	}


	/**
	 * Returns the x-value for the specified series and item.  The
	 * implementation is responsible for ensuring that the x-values are
	 * presented in ascending order.
	 *
	 * @param  seriesIndex  the series (zero-based index).
	 * @param  itemIndex  the item (zero-based index).
	 *
	 * @return The x-value
	 *
	 * @see XYDataset
	 */
	@Override
	public Number getX(int seriesIndex, int itemIndex) {
		return getValue(0,itemIndex);
	}

	/**
	 * Returns the y-value for the specified series and item.
	 *
	 * @param  seriesIndex  the series (zero-based index).
	 * @param  itemIndex  the item (zero-based index).
	 *
	 * @return The yValue value
	 *
	 * @see XYDataset
	 */
	@Override
	public Number getY(int seriesIndex, int itemIndex) {
		return getValue(seriesIndex+1,itemIndex);
	}

	/**
	 * Returns the number of items in the specified series.
	 *
	 * @param  seriesIndex  the series (zero-based index).
	 *
	 * @return The itemCount value
	 *
	 * @see XYDataset
	 */
	@Override
	public int getItemCount(int seriesIndex) {
		return this.rows.size();
	}

	/**
	 * Returns the number of items in all series.  This method is defined by
	 * the {@link TableXYDataset} interface.
	 *
	 * @return The item count.
	 */
	@Override
	public int getItemCount() {
		return getItemCount(0);
	}

	/**
	 * Returns the number of series in the dataset.
	 *
	 * @return The seriesCount value
	 *
	 * @see XYDataset
	 * @see Dataset
	 */
	@Override
	public int getSeriesCount() {
		return this.columnNames.length;
	}

	/**
	 * Returns the key for the specified series.
	 *
	 * @param seriesIndex  the series (zero-based index).
	 *
	 * @return The seriesName value
	 *
	 * @see XYDataset
	 * @see Dataset
	 */
	@Override
	public Comparable getSeriesKey(int seriesIndex) {

		if ((seriesIndex < this.columnNames.length)
				&& (this.columnNames[seriesIndex] != null)) {
			return this.columnNames[seriesIndex];
		}
		else {
			return "";
		}

	}


	/**
	 * Close the database connection
	 */
	public void close() {

		try {
			this.connection.close();
		}
		catch (Exception e) {
			System.err.println("JdbcXYDataset: swallowing exception.");
		}

	}

	/**
	 * Returns the minimum y-value in the dataset.
	 *
	 * @param includeInterval  a flag that determines whether or not the
	 *                         y-interval is taken into account.
	 *
	 * @return The minimum value.
	 */
	@Override
	public double getRangeLowerBound(boolean includeInterval) {
		return this.minValue;
	}

	/**
	 * Returns the maximum y-value in the dataset.
	 *
	 * @param includeInterval  a flag that determines whether or not the
	 *                         y-interval is taken into account.
	 *
	 * @return The maximum value.
	 */
	@Override
	public double getRangeUpperBound(boolean includeInterval) {
		return this.maxValue;
	}

	/**
	 * Returns the range of the values in this dataset's range.
	 *
	 * @param includeInterval  a flag that determines whether or not the
	 *                         y-interval is taken into account.
	 *
	 * @return The range.
	 */
	@Override
	public Range getRangeBounds(boolean includeInterval) {
		return new Range(this.minValue, this.maxValue);
	}

	public XYDataset getAbsoluteProfit() {
		return new AbstractXYDataset() {

			@Override
			public int getItemCount(int seriesIndex) {
				return rows.size();
			}

			@Override
			public Number getX(int seriesIndex, int itemIndex) {
				return getValue(0,itemIndex);
			}

			@Override
			public Number getY(int seriesIndex, int itemIndex) {
				return getValue(columnNames.length+seriesIndex+1,itemIndex);
			}

			@Override
			public int getSeriesCount() {
				return columnNames.length>0?1:0;
			}

			@Override
			public Comparable getSeriesKey(int seriesIndex) {
				return "All";
			}
		};
	}

}
