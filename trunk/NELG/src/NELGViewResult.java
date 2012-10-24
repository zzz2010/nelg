import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;


public class NELGViewResult {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Options options = new Options();
		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		String inputfile="";
		options.addOption("i", true, "result file");
		try {
			cmd = parser.parse( options, args);
			if(cmd.hasOption("i"))
			{
				inputfile=cmd.getOptionValue("i");
			}
			else
			{
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "NELGViewResult", options );
				return;
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "NELGViewResult", options );
			return;
		}
		
//de-serialization result file		
		Object Result=null;
		File f1=new File(inputfile);
		if(f1.exists())
		{
			 FileInputStream fileIn;
			 try {
				fileIn = new FileInputStream(f1.getAbsolutePath());
				 ObjectInputStream in = new ObjectInputStream(fileIn);
				 Result=in.readObject();
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println(e.getMessage());
			} 
			//process the result
			if(Result.getClass().equals(ClassificationResult.class))
			{
				ViewClassificationResult((ClassificationResult) Result);
			}
		}
		

	}
	
	public static void ViewClassificationResult(ClassificationResult result)
	{
		//text summary
		System.out.println(result.JobTitle);
		System.out.println(result.toString());
		
		//figure generation
		if(result.isRegression)
		{
			//regression result
			
			//scatter plot with line fitting
			
			
			//feature ranking plot
		}
		else
		{
			//classification result
			
			//heatmap
			
			//feature ranking plot
		}
	}
	
	
	public  void DrawROC(String pngfile, HashMap<String,XYSeries> ROCdata)
	{
		  XYSeriesCollection dataset = new XYSeriesCollection();
		 for(String name: ROCdata.keySet())
		 {
			 dataset.addSeries(ROCdata.get(name));
		 }
		 JFreeChart chart = ChartFactory.createXYLineChart(
	                "ROC curve", // chart title
	                "False Positive Rate", // x axis label
	                "True Positive Rate", // y axis label
	                dataset, // data
	                PlotOrientation.VERTICAL,
	                true, // include legend
	                true, // tooltips
	                false // urls
	                );
	// NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
	        chart.setBackgroundPaint(Color.white);
	// get a reference to the plot for further customisation...
	        XYPlot plot = (XYPlot) chart.getPlot();
	        plot.setBackgroundPaint(Color.lightGray);
	        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
	        plot.setDomainGridlinePaint(Color.white);
	        plot.setRangeGridlinePaint(Color.white);
	        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
	        renderer.setShapesVisible(true);
	        renderer.setShapesFilled(true);
	// change the auto tick unit selection to integer units only...
	        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
	        rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
	        
	        ChartPanel chartPanel = new ChartPanel(chart);
	        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
	      //  setContentPane(chartPanel);
	        try {
				ChartUtilities.saveChartAsPNG(new File(pngfile), chart, 800, 600);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}

}
