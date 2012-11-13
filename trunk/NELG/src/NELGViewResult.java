import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.stat.clustering.KMeansPlusPlusClusterer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.ArrayUtilities;
import org.tc33.jheatchart.HeatChart;


import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.trees.RandomForest;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Remove;


public class NELGViewResult {
	static double auc_cutoff=0;
	static double corr_cutoff=0;
	static String outputDir="Figure";
	static boolean reGen=false;
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
		options.addOption("f", false, "force overwrite the existing folders");
		(new File(outputDir)).mkdir();
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
			if(cmd.hasOption("f"))
				reGen=true;
			
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
		String[] comps_str=result.JobTitle.split("_");
		outputDir=outputDir+"/"+comps_str[0];
		(new File(outputDir)).mkdir();
		try {
			FileWriter outFile = new FileWriter(outputDir+"/"+comps_str[1]+".txt");
			PrintWriter out = new PrintWriter(outFile);
			out.println(result.toString().replace("with ", "with\n"));
			out.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
			
			
			if(reGen==false&&(new File(outputDir+"/"+result.JobTitle+".bar.png").exists()))
			{
				System.out.println("skip:\t"+outputDir+"/"+result.JobTitle+".bar.png");
				return;
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

				
				//heatmap
				Set<String> selFeatNames=SingleTrackFeatures.keySet();
				DoubleMatrix2D featureMatrix=LoadFeatureData(selFeatNames,result.JobTitle.split("_")[0]);
				DoubleMatrix1D targetvalue=jobdata.targetValue.viewPart(0, featureMatrix.rows());
				int stridesize=8;
				int targetColorwidth=stridesize;
				DenseDoubleMatrix2D targetvalue2=new DenseDoubleMatrix2D(targetvalue.size(), targetColorwidth);
				for (int i = 0; i < targetvalue.size(); i++) {
					//use half brand for target value, first half as separate line to feature
					for (int j = 0; j < targetColorwidth/2; j++) {
						targetvalue2.set(i, targetColorwidth/2+j, targetvalue.getQuick(i));
					}
				}
				ArrayList<String> selFeatNames2=new ArrayList<String>(selFeatNames);
				DoubleMatrix2D combined=DoubleFactory2D.sparse.appendColumns(featureMatrix, targetvalue2);
				DoubleMatrix2D combinedP_order=clusterReorder(combined);
				drawHeatMap( combinedP_order, result.JobTitle,selFeatNames2,stridesize);
				
				
				//classification result
				//feature ranking plot
				HashMap<String, Double> featRankResult=new HashMap<String, Double>();
				for (String trackname:SingleTrackFeatures.keySet()) {
					Instances data_sub=subFilter(data, SingleTrackFeatures.get(trackname));
					Double auc=classification(data_sub);
					featRankResult.put(trackname, auc);
				}
				//draw
				DrawBarChart(featRankResult,result.JobTitle,"AUC");
			}
		}
	}
	
	public static DoubleMatrix2D LoadFeatureData(Collection<String> featNames,String targetName)
	{
		File dir=new File(common.tempDir+targetName);
		File[] files=dir.listFiles();
		HashMap<String, String> featKey=new HashMap<String, String>(featNames.size());
		for (int i = 0; i < files.length; i++) {
			String flname=files[i].getName();
			if(flname.endsWith("_bg"))
				continue;
			for (String feat:featNames) {
					if(flname.contains(feat))
					{
						featKey.put(feat, flname);
						break;
					}
					
				}
			
		}
		
		featNames.retainAll(featKey.keySet());
		DoubleMatrix2D combined=null;
		int[] columnIndexes=new int[]{2,5,8,11,14,17,20,23};
		for (String feat : featNames) {
			String storekey=featKey.get(feat);
			DoubleMatrix2D temp=StateRecovery.loadCache_SparseDoubleMatrix2D(storekey);
			int[] rowIndexes=new int[temp.rows()];
			for (int i = 0; i < temp.rows(); i++) {
				rowIndexes[i]=i;
			}
			temp=temp.viewSelection(rowIndexes, columnIndexes);
			if(temp==null)
				continue;
			if(combined==null)
				combined=temp;
			else
				combined=DoubleFactory2D.sparse.appendColumns(combined, temp);
		}
			return combined;
	}
	
	public static DoubleMatrix2D clusterReorder(DoubleMatrix2D matrix)
	{
		SimpleKMeans kmean=new SimpleKMeans();
		for (int i = 0; i < matrix.columns(); i++) {
			DoubleMatrix1D vec= matrix.viewColumn(i);
			double m=vec.zSum()/vec.size();
				for (int j = 0; j < vec.size(); j++) {
					vec.set(j, Math.log((vec.getQuick(j)+1)/(m+1)));
				}
			}
		DenseDoubleMatrix2D clusterlabel=new DenseDoubleMatrix2D(matrix.rows(), 1);
		try {
			kmean.setNumClusters(5);
			Instances data=matrix2instances( matrix);
			
			kmean.buildClusterer(data);
			for (int i = 0; i < matrix.rows(); i++) {
				clusterlabel.set(i, 0, kmean.clusterInstance(data.instance(i)));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DoubleMatrix2D combined=DoubleFactory2D.dense.appendColumns(matrix, clusterlabel);
		return combined.viewSorted(combined.columns()-1);
	}
	
	public static Instances matrix2instances(DoubleMatrix2D matrix)
	{
		FastVector attrList=new FastVector(matrix.columns());
		for (int i = 0; i < matrix.columns(); i++) {
			Attribute temp=new Attribute( String.valueOf(i));
			attrList.addElement(temp);
		}
		Instances insts=new Instances("", attrList, matrix.rows());
		for (int i = 0; i < matrix.rows(); i++) {
			insts.add(new Instance(1, matrix.viewRow(i).toArray()));
		}
		return insts;
		
	}
	public static void drawHeatMap_1(DoubleMatrix2D matrix, String title)
	{
		 String pngfile=title+".heatmap.png";

			//Create our heat map chart using our data.
			HeatChart map = new HeatChart(matrix.toArray());
			map.setLowValueColour(Color.white);
			map.setHighValueColour(Color.red);
			//Customise the chart.
			map.setTitle(title);
			
//			map.setXAxisLabel("X Axis");
//			map.setYAxisLabel("Y Axis");
			
			//Output the chart to a file.
			try {
				map.saveToFile(new File(pngfile));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	public static double[][] sparseMatrix(DoubleMatrix2D matrix)
	{
		IntArrayList rows = new IntArrayList();
		IntArrayList cols = new IntArrayList();
		DoubleArrayList vals = new DoubleArrayList();
		matrix.getNonZeros(rows, cols, vals);
		double[][] ret=new double[3][rows.size()];
		for (int i = 0; i < rows.size(); i++) {
			ret[0][i]=rows.get(i);
			ret[1][i]=cols.get(i);
			ret[2][i]=vals.get(i);
		}
		return ret;
	}
	
	public static void drawHeatMap(DoubleMatrix2D matrix, String title, List<String> featName, int stride)
	{
		 String pngfile=outputDir+"/"+title+".heatmap.png";
		 ValueAxis numberaxis = new NumberAxis("Feature");
		 featName.add("targetValue");
		 String[] strAttr=new String[matrix.columns()];
		 for (int i = 0; i < matrix.columns(); i++) {
			if((i%stride)==(stride/2)&&(i/stride)<featName.size())
			{
					strAttr[i]=featName.get(i/stride);
			}
			else
			{
//				if((i%stride)==(stride-1))
//				{
//					strAttr[i]="";
//				}
//				else
				strAttr[i]="";
			}
		}
		
		 SymbolAxis symaxis=new SymbolAxis("", strAttr);
		 symaxis.setTickUnit(new NumberTickUnit(stride/2));
//		 symaxis.setRange(-stride/2,matrix.columns());
		 NumberAxis numberaxis1 = new NumberAxis("Peak");
		 numberaxis1.setRange(new Range(0, matrix.rows()));
		 DefaultXYZDataset xyzdataset = new DefaultXYZDataset();
		 xyzdataset.addSeries(title, sparseMatrix(matrix));
		 XYBlockRenderer xyblockrenderer = new XYBlockRenderer();
	        LookupPaintScale lookuppaintscale = new LookupPaintScale(-1D, Double.MAX_VALUE, Color.black);
	        lookuppaintscale.add(0D, Color.blue);
	        lookuppaintscale.add(0.5D, Color.green);
	        lookuppaintscale.add(1D, Color.orange);
	        lookuppaintscale.add(2D, Color.red);
	        xyblockrenderer.setPaintScale(lookuppaintscale);
	        XYPlot xyplot = new XYPlot(xyzdataset, numberaxis1,symaxis, xyblockrenderer);xyplot.setBackgroundPaint(Color.lightGray);
	        xyplot.setDomainGridlinePaint(Color.white);
	        xyplot.setRangeGridlinePaint(Color.white);
	        xyplot.setForegroundAlpha(0.66F);
//	        xyplot.setAxisOffset(new RectangleInsets(5D, 5D, 5D, 5D));
	        JFreeChart jfreechart = new JFreeChart(title, xyplot);
	        
	        try {
				ChartUtilities.saveChartAsPNG(new File(pngfile), jfreechart, 800, 600);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
	
	public static double classification(Instances data)
	{
		double auc=0;
		Classifier modeler =new RandomForest();
		Evaluation eval;
		try {
			eval = new Evaluation(data);
			eval.crossValidateModel(modeler, data, 3, new Random(1));
			auc=eval.areaUnderROC(1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return auc;
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
		  chart.setBackgroundPaint(Color.white);
		  chart.getTitle().setPaint(Color.blue); 
		  CategoryPlot p = chart.getCategoryPlot(); 
		  p.setRangeGridlinePaint(Color.black); 
		  chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
	        ChartPanel chartPanel = new ChartPanel(chart);
	        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
	      //  setContentPane(chartPanel);
	        try {
				ChartUtilities.saveChartAsPNG(new File(outputDir+"/"+Title+".bar.png"), chart, 800, 600);
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
				regressionRenderer.setSeriesOutlineStroke(i, new BasicStroke(4));
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
				ChartUtilities.saveChartAsPNG(new File(outputDir+"/"+pngfile), chart, 800, 600);
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
