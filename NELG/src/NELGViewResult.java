import java.awt.Color;
import java.awt.Paint;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.ArrayUtilities;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;


public class NELGViewResult {
	static double auc_cutoff=0;
	static double corr_cutoff=0;
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
		options.addOption("tauc", true, "AUC Threshold (Default 0)");
		options.addOption("tcor", true, "Corr Threshold (Default 0)");

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
			if(cmd.hasOption("tauc"))
			{
				auc_cutoff=Double.parseDouble(cmd.getOptionValue("tauc"));
			}
			if(cmd.hasOption("tcor"))
			{
				corr_cutoff=Double.parseDouble(cmd.getOptionValue("tcor"));
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
			
			//process the job
			if(Result.getClass().equals(ClassificationJob.class))
			{
				ViewClassificationJob((ClassificationJob) Result);
			}
		}
		

	}
	
	public static void ViewClassificationJob(ClassificationJob job)
	{
		//text summary
		System.out.println(job.JobTitle);
		String outstr="";
		for (int i = 0; i <job.FeatureMatrix.size(); i++) {
			outstr+="\t"+job.FeatureMatrix.get(i).FeatureId+"."+job.FeatureMatrix.get(i).binId+"|"+job.FeatureMatrix.get(i).featureSelectScore;
		}
		System.out.println(outstr);
	}
	
	public static void ViewClassificationResult(ClassificationResult result)
	{
		if(result.isRegression&&result.Corr<corr_cutoff)
			return;
		if(!result.isRegression&&result.AUC<auc_cutoff)
			return;
		//text summary
		System.out.println(result.JobTitle);
		System.out.println(result.toString());
		
		ClassificationJob jobdata=StateRecovery.LoadClassificationJob(result.JobTitle);
		if(jobdata!=null)
		{
			Instances data = ChildModeler.getDatasetFromJob(jobdata);
			//take out the selected attribution
			//hash map
			HashMap<String,String> SingleTrackFeatures=new HashMap<String, String>(); 
			HashSet<String> selAttrName=new HashSet<String>(result.FeatureIdBin.size());
			for (int i = 0; i < result.FeatureIdBin.size(); i++) {
				selAttrName.add(result.FeatureIdBin.get(i).key+result.FeatureIdBin.get(i).value);
			}
//			ArrayList<Integer> remAttid=new ArrayList<Integer>();
			String filterStr="";
			for (int i = 0; i < jobdata.FeatureMatrix.size(); i++) {
				String trackname=jobdata.FeatureMatrix.get(i).FeatureId;
				String attrname=trackname+jobdata.FeatureMatrix.get(i).binId;
				if(!selAttrName.contains(attrname))
				{
					if(filterStr.length()>0)
						filterStr+=",";
					filterStr+=i+1;
				}
				//construct index for single track feature
				if(!SingleTrackFeatures.containsKey(trackname))
				{
					SingleTrackFeatures.put(trackname, String.valueOf(i+1));
				}
				else
				{
					String temp=SingleTrackFeatures.get(trackname);
					temp+=",";
					temp+=i+1;
					SingleTrackFeatures.put(trackname,temp);
				}
			}
		
			Instances data2=subFilter(data, filterStr);

			
			//generate predicted value
			
			Evaluation eval=null;
			double[] predictValue=null;
			try {
				eval = new Evaluation(data2);
				predictValue=eval.evaluateModel(result.LearnedModel, data2);
				System.out.println(eval.toSummaryString("\nFittingResults\n======\n", false));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		//figure generation
			if(result.isRegression)
			{
				//regression result
				XYSeries points=new XYSeries(result.JobTitle);
				for (int i = 0; i < predictValue.length; i++) {
					points.add(predictValue[i], jobdata.targetValue.get(i));
				}
				//scatter plot with line fitting
				DrawScatterPlot(result.JobTitle+".scatter.png",points);
				
				//feature ranking plot
				HashMap<String, Double> featRankResult=new HashMap<String, Double>();
				for (String trackname:SingleTrackFeatures.keySet()) {
					Instances data_sub=subFilter(data, SingleTrackFeatures.get(trackname));
					Double corr=linearRegression(data_sub);
					featRankResult.put(trackname, corr);
				}
				//draw
				DrawBarChart(featRankResult,result.JobTitle,"Correlation");
			}
			else
			{
				//classification result
				
				
				//heatmap
				
				//feature ranking plot
			}
		}
	}
	
	public static double linearRegression(Instances data)
	{
		double corr=0;
		Classifier modeler =new LinearRegression();
		Evaluation eval;
		try {
			eval = new Evaluation(data);
			eval.crossValidateModel(modeler, data, 3, new Random(1));
			corr=eval.correlationCoefficient();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return corr;
	}
	
	public static Instances subFilter(Instances data,String filterstr)
	{
		weka.filters.unsupervised.attribute.Remove filter=new Remove();
		filter.setAttributeIndices(filterstr);
		Instances data2=null;
		try {
			filter.setInputFormat(data);
			data2= Filter.useFilter(data, filter);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data2;
	}
	
	
	public static void DrawBarChart(HashMap<String, Double> data, String Title,String Ylabel)
	{
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for(String track:data.keySet())
		{
			 dataset.setValue(data.get(track), Ylabel, track);
		}
		
		  JFreeChart chart = ChartFactory.createBarChart
		  (Title,"Feature", Ylabel, dataset, 
		   PlotOrientation.VERTICAL, false,true, false);
		  chart.setBackgroundPaint(Color.yellow);
		  chart.getTitle().setPaint(Color.blue); 
		  CategoryPlot p = chart.getCategoryPlot(); 
		  p.setRangeGridlinePaint(Color.red); 

	        ChartPanel chartPanel = new ChartPanel(chart);
	        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
	      //  setContentPane(chartPanel);
	        try {
				ChartUtilities.saveChartAsPNG(new File(Title+".bar.png"), chart, 800, 600);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	public static void DrawScatterPlot(String pngfile,XYSeries points)
	{
		 XYSeriesCollection dataset = new XYSeriesCollection();
		 dataset.addSeries(points);
		 JFreeChart chart = ChartFactory.createScatterPlot((String) points.getKey(), "Predicted Value", "True Value", dataset,  PlotOrientation.VERTICAL,false, true, false);
		// NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
	        
	        XYPlot plot = (XYPlot) chart.getPlot();
			XYItemRenderer scatterRenderer = plot.getRenderer();
			StandardXYItemRenderer regressionRenderer = new StandardXYItemRenderer();
			regressionRenderer.setBaseSeriesVisibleInLegend(false);
			plot.setDataset(1, regress(dataset));
			plot.setRenderer(1, regressionRenderer);
			DrawingSupplier ds = plot.getDrawingSupplier();
			for (int i = 0; i < dataset.getSeriesCount(); i++) {
				Paint paint = ds.getNextPaint();
				scatterRenderer.setSeriesPaint(i, paint);
				regressionRenderer.setSeriesPaint(i, paint);
			}
			chart.setBackgroundPaint(Color.white);
			 plot.setBackgroundPaint(Color.white);
		        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		        plot.setDomainGridlinePaint(Color.white);
		        plot.setRangeGridlinePaint(Color.white);
		        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		        renderer.setShapesVisible(true);
		        renderer.setShapesFilled(true);
		// change the auto tick unit selection to integer units only...
		        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		        rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
	        try {
				ChartUtilities.saveChartAsPNG(new File(pngfile), chart, 800, 600);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	private static XYDataset regress(XYSeriesCollection data) {
		// Determine bounds
		double xMin = Double.MAX_VALUE, xMax = 0;
		for (int i = 0; i < data.getSeriesCount(); i++) {
			XYSeries ser = data.getSeries(i);
			for (int j = 0; j < ser.getItemCount(); j++) {
				double x = ser.getX(j).doubleValue();
				if (x < xMin) {
					xMin = x;
				}
				if (x > xMax) {
					xMax = x;
				}
			}
		}
		// Create 2-point series for each of the original series
		XYSeriesCollection coll = new XYSeriesCollection();
		for (int i = 0; i < data.getSeriesCount(); i++) {
			XYSeries ser = data.getSeries(i);
			int n = ser.getItemCount();
			double sx = 0, sy = 0, sxx = 0, sxy = 0, syy = 0;
			for (int j = 0; j < n; j++) {
				double x = ser.getX(j).doubleValue();
				double y = ser.getY(j).doubleValue();
				sx += x;
				sy += y;
				sxx += x * x;
				sxy += x * y;
				syy += y * y;
			}
			double b = (n * sxy - sx * sy) / (n * sxx - sx * sx);
			double a = sy / n - b * sx / n;
			XYSeries regr = new XYSeries(ser.getKey());
			regr.add(xMin, a + b * xMin);
			regr.add(xMax, a + b * xMax);
			coll.addSeries(regr);
		}
		return coll;
	}
	public  void DrawROC(String pngfile,  XYSeriesCollection dataset)
	{

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
