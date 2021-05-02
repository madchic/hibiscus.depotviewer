package Tools;

import java.awt.BorderLayout;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.swing.JFrame;

import de.open4me.depot.sql.SQLUtils;
import de.willuhn.util.ApplicationException;


public class TestChart {

	public static void main(String[] args) throws SQLException, ApplicationException {
		String url ="jdbc:mysql://192.168.2.100:3306/hibiscus?useUnicode=Yes&characterEncoding=ISO8859_1&serverTimezone=Europe/Paris&useServerPrepStmts=false&rewriteBatchedStatements=true";
		String user="hibiscus";
		String pw="q5qFmMY39XUr";
		
		SQLUtils.INSTANCE=()->{
			try {
				return DriverManager.getConnection(url, user, pw);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		};

//		BestandsEntwicklungXyDataset dataset = new BestandsEntwicklungXyDataset(DriverManager.getConnection(url, user, pw));
//        JFreeChart chart = createChart(dataset);
//        ChartPanel chartPanel = new ChartPanel(chart);
//        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
//        chartPanel.setBackground(Color.white);
        
        JFrame frame = new JFrame();
        //Add content to the window.
        frame.add(new KontoInformation(), BorderLayout.CENTER);
//        frame.add(chartPanel);
//        dataset.executeQuery();
        frame.pack();
        frame.setTitle("Line chart");
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true); 
	}
	
}
