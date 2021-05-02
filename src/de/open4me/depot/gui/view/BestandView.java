package de.open4me.depot.gui.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.rmi.RemoteException;
import java.sql.SQLException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.TabFolder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.experimental.chart.swt.ChartComposite;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.DatumsSlider;
import de.open4me.depot.gui.control.BestandPieChartControl;
import de.open4me.depot.gui.control.BestandTableControl;
import de.open4me.depot.gui.control.BestandsControl;
import de.open4me.depot.jfreechart.BestandsEntwicklungXyDataset;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.tools.Bestandspruefung;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.LabelGroup;
import de.willuhn.jameica.gui.util.ScrolledContainer;
import de.willuhn.jameica.gui.util.TabGroup;
import de.willuhn.util.ApplicationException;

public class BestandView extends AbstractView
{
	private JFreeChart chartWert;

	/**
	 * @see de.willuhn.jameica.gui.AbstractView#bind()
	 */
	public void bind() throws Exception {
		if (!Bestandspruefung.isOK()) {
			LabelGroup group = new LabelGroup(this.getParent(),
					Settings.i18n().tr("Inkonsistenzen zwischen Umsätzen und Beständen"));
			group.addText("Der Abgleich zwischen Umsatz und Bestand hat Inkonsistenz ergeben.\n"
					+ "Falls sie eine Transaktion vor wenigen Tagen getätig haben, hat die Bank sie evtl. noch nicht als Umsatz und im Bestand gebucht.\nBitte korrigieren sie die Fehler, falls nötig!", true);
			group.addPart(new Button("Inkonsistenzen anzeigen",new Action() {

				@Override
				public void handleAction(Object context)
						throws ApplicationException {
					String output;
					try {
						output = Bestandspruefung.exec();
						GUI.startView(BestandsAbgleichView.class,output);
					} catch (RemoteException e) {
						e.printStackTrace();
						throw new ApplicationException(e);
					}
				}

			}));


		}
		BestandsControl bestandsControl = new BestandsControl(this); 
		GUI.getView().setTitle(Settings.i18n().tr("Bestand"));

		DatumsSlider datumsSlider = new DatumsSlider(bestandsControl.getDates());

		final TabFolder folder = new TabFolder(getParent(), SWT.CENTER);
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));

		TabGroup tabellenTab = new TabGroup(folder, "Tabellarisch");
		BestandTableControl control = new BestandTableControl(this, datumsSlider);
		Container container = new ScrolledContainer(tabellenTab.getComposite());
		container.addPart(control.getBestandsTabelle());

		final TabGroup piechartTab = new TabGroup(folder, "Graphisch");
		piechartTab.getComposite().setLayout(new FillLayout());
		BestandPieChartControl chart = new BestandPieChartControl(this, datumsSlider);
		chart.getBestandChart(piechartTab.getComposite());
		
		final BestandsEntwicklungXyDataset data = new BestandsEntwicklungXyDataset(SQLUtils.getConnection(),3);
		new TabGroup(folder, "Wertentwicklung Maurice", false) {
			
			String lastSelection = "";
			private XYItemRenderer renderer;
			{
				getComposite().setLayout(new FillLayout());

				chartWert = createChart(data);
				new ChartComposite(getComposite(), SWT.NONE, chartWert, true);
			}
		};
		final BestandsEntwicklungXyDataset data2 = new BestandsEntwicklungXyDataset(SQLUtils.getConnection(),15);
		new TabGroup(folder, "Wertentwicklung Airbus", false) {
			
			String lastSelection = "";
			private XYItemRenderer renderer;
			{
				getComposite().setLayout(new FillLayout());

				chartWert = createChart(data2);
				new ChartComposite(getComposite(), SWT.NONE, chartWert, true);
			}
		};

		datumsSlider.paint(getParent());

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

        try {
			data.executeQuery();
		} catch (SQLException | ApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return chart;
    }
}