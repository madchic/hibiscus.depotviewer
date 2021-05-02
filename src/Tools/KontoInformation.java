package Tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/*
 * TabbedPaneDemo.java requires one additional file:
 *   images/middle.gif.
 */
 
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.TableRowSorter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;

import de.open4me.depot.jfreechart.BestandsEntwicklungXyDataset;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLTableModel;
import de.open4me.depot.sql.SQLUtils;
 
public class KontoInformation extends JPanel {
	
	private static final String QUERY_UMSATZ="select datum, empfaenger_konto, empfaenger_blz, empfaenger_name, betrag, saldo, zweck, zweck2, umsatztyp.name as kategorie from umsatz left join umsatztyp on umsatz.umsatztyp_id = umsatztyp.id where umsatz.konto_id=";
	private Connection connection;
	
	
    public KontoInformation() {
        super(new GridLayout(1, 1));
        
        try {
		 connection = SQLUtils.getConnection();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
         
        JTabbedPane tabbedPane = new JTabbedPane();
        ImageIcon icon = createImageIcon("images/middle.gif");
        int count=0;
        for (GenericObjectSQL o: SQLUtils.getResultSet("select * from konto where name like \"%Maurice%\"", "konto", "id")) {
			try {
		        JComponent giroKonto = createKontoInfo(o.getID());
		        tabbedPane.addTab(o.getAttribute("bezeichnung")+": "+o.getAttribute("saldo"), icon, giroKonto,
		        		o.getAttribute("name")+" "+o.getAttribute("bezeichnung")+": "+o.getAttribute("saldo")+" vom "+o.getAttribute("saldo_datum"));
		        tabbedPane.setMnemonicAt(0, KeyEvent.VK_0+(++count));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
         
        JComponent chartWertpapiere = createChartPanel(3);
        tabbedPane.addTab("DKB Wertpapier Deport", icon, chartWertpapiere,
                "Still does nothing");
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_0+(++count));
        
        chartWertpapiere = createChartPanel(15);
        tabbedPane.addTab("Airbus", icon, chartWertpapiere,
                "Still does nothing");
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_0+(++count));

         
        //Add the tabbed pane to this panel.
        add(tabbedPane);
         
        //The following line enables to use scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }


	protected JComponent createChartPanel(int kontoID) {
		
    	Connection connection=null;
		try {
			connection = SQLUtils.getConnection();
			BestandsEntwicklungXyDataset dataset = new BestandsEntwicklungXyDataset(connection,kontoID);
	        JFreeChart chart = createChart(dataset);
	        ChartPanel chartPanel = new ChartPanel(chart);
	        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
	        chartPanel.setBackground(Color.white);
	        dataset.executeQuery();
	        return chartPanel;    	
		} catch (Exception e1) {
			e1.printStackTrace();
			JPanel jp = new JPanel();
			jp.add(new JLabel(e1.getMessage()));
			return jp;
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e1) {
				}
			}
			
		}

    }
    
	private static JFreeChart createChart(BestandsEntwicklungXyDataset data) {

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
				"",  "Datum",
				"Gewinn[%]", data, true, true, false);

        XYPlot plot = chart.getXYPlot();
        Stroke soild = new BasicStroke(2.0f);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setBaseShapesVisible(false);
        renderer.setSeriesStroke(0, soild);

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);

        chart.getLegend().setFrame(BlockBorder.NONE);

        chart.setTitle(new TextTitle("Wertentwicklung",
                        new Font("SanSerif", java.awt.Font.BOLD, 18)
                )
        );
        
       
        

        final NumberAxis axis2 = new NumberAxis("Profit[€]");
        axis2.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(1, axis2);
        plot.setDataset(1, data.getAbsoluteProfit());
        plot.mapDatasetToRangeAxis(1, 1);
        
        final StandardXYItemRenderer renderer2 = new StandardXYItemRenderer();
        renderer2.setSeriesPaint(0, Color.darkGray);
        renderer2.setSeriesStroke(0, soild);
        renderer2.setBaseShapesVisible(false);
        plot.setRenderer(1, renderer2);

        return chart;
    }
     
    protected JComponent createKontoInfo(String konto_id) {
//    	Connection connection=null;
		try {
//			connection = SQLUtils.getConnection();
	    	Statement statement = connection.createStatement();
			
			ResultSet resultSet = statement.executeQuery(QUERY_UMSATZ+konto_id);
			JScrollPane js = new JScrollPane();
	        // Der TableRowSorter wird die Daten des Models sortieren
	        TableRowSorter<SQLTableModel> sorter = new TableRowSorter<SQLTableModel>();
	        SQLTableModel model = new SQLTableModel(resultSet);
	        JTable table = new JTable(model);
	        // Der Sorter muss dem JTable bekannt sein
	        table.setRowSorter( sorter );
	        
	        // ... und der Sorter muss wissen, welche Daten er sortieren muss
	        sorter.setModel( model );
			js.setViewportView(table);
			return js;
		} catch (Exception e1) {
			e1.printStackTrace();
			JPanel jp = new JPanel();
			jp.add(new JLabel(e1.getMessage()));
			return jp;
		} finally {
//			if (connection != null) {
//				try {
//					connection.close();
//				} catch (SQLException e1) {
//				}
//			}
			
		}
    }
     
    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = KontoInformation.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
}
