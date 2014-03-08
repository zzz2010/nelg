import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.PropertyConfigurator;
import org.broad.tribble.annotation.Strand;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;
import org.jppf.client.JPPFClient;
import org.jppf.utils.FileUtils;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;


public class PeakClassifier {
	 private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
		/**
		 * @param args
		 */
		 public static int max_threadNum=4;
		 
		 public static ArrayList<String> selectedClusterFeature=null;
		 
		 
		public static void drawSignalAroundPeakBatch(Collection<TrackRecord> signalPool,String targetName,List<SimpleBEDFeature> query)
		{
			DoubleMatrix1D targetValue = SignalTransform.BedFeatureToValues(query);
			EqualBinFeatureExtractor FE=new EqualBinFeatureExtractor(20);
			for (TrackRecord feat : signalPool) {
				
				DoubleMatrix2D temp=FE.extractSignalFeature(feat, query);
				//System.out.println(feat.ExperimentId);
				NELGViewResult.drawSignalAroundPeakCurve(temp, targetValue, feat.ExperimentId, targetName);
			}
				
		}
		public static void drawSignalAroundPeakBatch2(Collection<TrackRecord> signalPool,String targetName,List<SimpleBEDFeature> query)
		{
			String pngfile= common.outputDir+"/"+targetName+"_allcurve"+".png";
			DoubleMatrix1D targetvalue = SignalTransform.BedFeatureToValues(query);
			
			int nbin=20;
			float binsize= (float)common.SignalRange/nbin;
			EqualBinFeatureExtractor FE=new EqualBinFeatureExtractor(nbin);
			XYSeriesCollection dataset = new XYSeriesCollection(); 
			
			for (TrackRecord feat : signalPool) {
				
				DoubleMatrix2D signal_matrix=FE.extractSignalFeature(feat, query);
			//////////////prepare data points/////////////////
		
			XYSeries posData=new XYSeries( "+"+feat.ExperimentId);
			XYSeries negData=new XYSeries( "-"+feat.ExperimentId);
			double[] tempdata1=new double[signal_matrix.columns()];
			double[] tempdata2=new double[signal_matrix.columns()];
			int poscount=0;
			int negcount=0;
			for (int i = 0; i < signal_matrix.rows(); i++) {
				if(targetvalue.get(i)>=0)
				{
					poscount++;
					for (int j = 0; j < signal_matrix.columns(); j++) {
						tempdata1[j]+=signal_matrix.get(i, j);
					}
				}
				else
				{
					negcount++;
					for (int j = 0; j < signal_matrix.columns(); j++) {
						tempdata2[j]+=signal_matrix.get(i, j);
					}
				}
			}
			for (int i = 0; i < tempdata2.length; i++) {
				posData.add(-common.SignalRange/2+i*binsize, tempdata1[i]/poscount);
				negData.add(-common.SignalRange/2+i*binsize,tempdata2[i]/negcount);
			}
			dataset.addSeries(posData);
			dataset.addSeries(negData);
			}
			//////////////ploting/////////////////
			 
			 JFreeChart chart = ChartFactory.createXYLineChart(
					 targetName, // chart title
		                "Position Around Peak", // x axis label
		                "Signal Count", // y axis label
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

		        IntervalMarker target = new IntervalMarker(0, 0.5);
		        target.setLabel("Target Range");
		        target.setPaint(new Color(222, 222, 255, 128));
		        //plot.addRangeMarker(target);
		        try {
				       for(int i=1; i<dataset.getSeries().size();i+=2)
				       {
				    	   		renderer.setSeriesFillPaint(i, chart.getXYPlot().getRenderer().getSeriesPaint(i-1));
				    	   		renderer.setSeriesStroke(i, 
				    	   	            new BasicStroke(
				    	   	                2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
				    	   	                1.0f, new float[] {10.0f, 6.0f}, 0.0f
				    	   	            )
				    	   	            );
				       }
				       ChartUtilities.saveChartAsPNG(new File(pngfile), chart, 800, 600);
				       
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
		}
		
		public static void drawSignalAroundClustBatch(Collection<TrackRecord> signalPool,String targetName,List<SimpleBEDFeature> query, DoubleMatrix1D clust){
			String pngfile= common.outputDir+"/"+targetName+"_clust"+".png";
			DoubleMatrix1D targetvalue = SignalTransform.BedFeatureToValues(query);
			
			int nbin=20, ind;
			float binsize= (float)common.SignalRange/nbin;
			double[] keys=new double[common.ClusterNum];
			int[] cnt=new int[common.ClusterNum],plus=new int[common.ClusterNum],minus=new int[common.ClusterNum];
			
			HashMap<Double, Integer> map=new HashMap<Double, Integer>();	
			EqualBinFeatureExtractor FE=new EqualBinFeatureExtractor(nbin); 				
			XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
			renderer1.setShapesVisible(false);
			
			ChartFrame frame=new ChartFrame("",null);
			frame.setLayout(new GridLayout(signalPool.size(),1,10,10)); 
	        ind=0;
	        for (int i=0;i<clust.size();i++){
	        	if (map.get(clust.get(i))==null){
	        		map.put(clust.get(i), ind);
	        		keys[ind]=clust.get(i);
	        		ind++;
	        	}
	        }
	        map.clear();
	        Arrays.sort(keys);
	        for (int i=0;i<common.ClusterNum;i++)
	        	map.put(keys[i],i);
	        for (int i=0;i<clust.size();i++){
	        	ind=map.get(clust.get(i));
	        	cnt[ind]++;
	        	if (query.get(i).strand==Strand.POSITIVE)
	        		plus[ind]++;
	        	else if (query.get(i).strand==Strand.NEGATIVE)
	        		minus[ind]++;
	        }
			for (TrackRecord feat : signalPool) {
				DoubleMatrix2D signal_matrix=FE.extractSignalFeature(feat, query);
				CombinedRangeXYPlot plot0 = new CombinedRangeXYPlot(new NumberAxis("Signal Count"));
				double[][] sum1=new double[common.ClusterNum][nbin],sum2=new double[common.ClusterNum][nbin];
				int num=Math.min(signal_matrix.rows(), clust.size());
			//////////////prepare data points/////////////////
				
				for (int i = 0; i < num; i++) {
					if(targetvalue.get(i)>=0)
					{
						ind=map.get(clust.get(i));
						for (int j = 0; j < signal_matrix.columns(); j++) {
							sum1[ind][j]+=signal_matrix.get(i, j);
						}
					}
					else
					{
						ind=map.get(clust.get(i));
						for (int j = 0; j < signal_matrix.columns(); j++) {
							sum2[ind][j]+=signal_matrix.get(i, j);
						}
					}
				}
				for (int i=0;i<common.ClusterNum;i++){
					XYSeriesCollection dataset = new XYSeriesCollection();
					XYSeries posData=new XYSeries("+clust");
					XYSeries negData=new XYSeries("-clust");
					for (int j=0;j<signal_matrix.columns();j++){
						posData.add(-common.SignalRange/2+j*binsize, sum1[i][j]/cnt[i]);
						negData.add(-common.SignalRange/2+j*binsize, sum2[i][j]/cnt[i]);
					}
					dataset.addSeries(posData);
					//dataset.addSeries(negData);
					plot0.add(new XYPlot(dataset,  new NumberAxis((i+1)+": "+cnt[i]+"/"+plus[i]+"/"+minus[i]),null, renderer1));
				}
				JFreeChart chart=new JFreeChart(feat.ExperimentId,JFreeChart.DEFAULT_TITLE_FONT, plot0, true);
				chart.removeLegend();
				ChartPanel chartPanel = new ChartPanel(chart);
		        chartPanel.setSize(new java.awt.Dimension(150*common.ClusterNum, 200)); 
		        frame.add(chartPanel);
			}
	        
			frame.setUndecorated(true);
			frame.setVisible(true);
			frame.setSize(150*common.ClusterNum, signalPool.size()*200);
			try{
				final BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics gr = image.getGraphics();
				frame.printAll(gr);
				gr.dispose();
				ImageIO.write(image, "PNG", new File(pngfile));
				//Thread.sleep(1000);
			}
			catch(Exception ex){
				ex.printStackTrace();
			}
			frame.dispose();
		}
		
		public static void drawSignalForDNase(Collection<TrackRecord> signalPool,String targetName,List<SimpleBEDFeature> query, DoubleMatrix1D clust){
			String pngfile= common.outputDir+"/"+targetName+"_DNase"+".png";
			DoubleMatrix1D targetvalue = SignalTransform.BedFeatureToValues(query);
			
			int nbin=20, ind;
			float binsize= (float)common.SignalRange/nbin;
			double[] keys=new double[common.ClusterNum];
			int[] cnt=new int[common.ClusterNum],plus=new int[common.ClusterNum],minus=new int[common.ClusterNum];
			double[][] sum1=new double[common.ClusterNum][nbin],sum2=new double[common.ClusterNum][nbin];
			
			HashMap<Double, Integer> map=new HashMap<Double, Integer>();
			CombinedRangeXYPlot plot0 = new CombinedRangeXYPlot(new NumberAxis("Signal Count"));			
			EqualBinFeatureExtractor FE=new EqualBinFeatureExtractor(nbin); 				
			XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
			XYSeriesCollection[] dataset = new XYSeriesCollection[common.ClusterNum];
			renderer1.setShapesVisible(false);
			
	        ind=0;
	        for (int i=0;i<clust.size();i++){
	        	if (map.get(clust.get(i))==null){
	        		map.put(clust.get(i), ind);
	        		keys[ind]=clust.get(i);
	        		ind++;
	        	}
	        }
	        map.clear();
	        Arrays.sort(keys);
	        for (int i=0;i<common.ClusterNum;i++){
	        	map.put(keys[i],i);
	        	dataset[i]=new XYSeriesCollection();
	        }
	        for (int i=0;i<clust.size();i++){
	        	ind=map.get(clust.get(i));
	        	cnt[ind]++;
	        	if (query.get(i).strand==Strand.POSITIVE)
	        		plus[ind]++;
	        	else if (query.get(i).strand==Strand.NEGATIVE)
	        		minus[ind]++;
	        }
			for (TrackRecord feat : signalPool) {
				DoubleMatrix2D signal_matrix=FE.extractSignalFeature(feat, query);
			//////////////prepare data points/////////////////
				
				if (!feat.ExperimentId.contains("plus")&&!feat.ExperimentId.contains("minus"))
					continue;
				for (int i = 0; i < signal_matrix.rows(); i++) {
					if(targetvalue.get(i)>=0)
					{
						ind=map.get(clust.get(i));
						for (int j = 0; j < signal_matrix.columns(); j++) {
							if (feat.ExperimentId.contains("plus"))
								sum1[ind][j]+=signal_matrix.get(i, j);
							else sum2[ind][j]+=signal_matrix.get(i, j);
						}
					}
				}
				for (int i=0;i<common.ClusterNum;i++){
					String name;
					if (feat.ExperimentId.contains("plus"))
						name="+clust"+(i+1);
					else name="-clust"+(i+1);
					XYSeries posData=new XYSeries(name);
					XYSeries negData=new XYSeries( "-clust");
					for (int j=0;j<signal_matrix.columns();j++){
						if (feat.ExperimentId.contains("plus"))
							posData.add(-common.SignalRange/2+j*binsize, sum1[i][j]/cnt[i]);
						else posData.add(-common.SignalRange/2+j*binsize, sum2[i][j]/cnt[i]);
						//negData.add(-common.SignalRange/2+j*binsize, sum2[i][j]/cnt[i]);
					}
					dataset[i].addSeries(posData);
					//dataset.addSeries(negData);
				}
			}
			for (int i=0;i<common.ClusterNum;i++){
				plot0.add(new XYPlot(dataset[i],  new NumberAxis("clust"+(i+1)+": "+cnt[i]+"/"+plus[i]+"/"+minus[i]),null, renderer1));
			}
			JFreeChart chart=new JFreeChart(targetName,JFreeChart.DEFAULT_TITLE_FONT, plot0, true);
			chart.removeLegend();
			try {
	        	ChartUtilities.saveChartAsPNG(new File(pngfile), chart, 150*common.ClusterNum, 300);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		static boolean	heatmap_log=false;
		static boolean	heatmap_medianNorm=false;
		static boolean	heatmap_sort=false;
		
		public static void main(String[] args) {
			PropertyConfigurator.configure( "./log4j.properties" ); 
			// TODO Auto-generated method stub
			Options options = new Options();
			options.addOption("threadnum", true, "maximum number of threads can be used in extract the feature data");
			options.addOption("dataDir", true, "the path of the folder with bigWig data");
			options.addOption("clusternum", true, "number of clusters in heatmap (default: 10)");
			options.addOption("outputDir", true, "the path of folder for output data");
			options.addOption("winsize", true, "if 0, mean no fix window size (default 4000)");
			options.addOption("peakfile1", true, "target peaks set  (bed format)");
			options.addOption("print", false, "fast mode for only generating figures (default: false)");
			options.addOption("peakfile2", true, "background peaks set (bed format, optional) ");
			options.addOption("clusterfeatures", true, "comma separated list of bigwig file names");
			options.addOption("log", false, "log transform in the heatmap plotting (default: no)");
			options.addOption("medianNorm", false, "normalized by the median value in the window size in the heatmap plotting (default: no)");
			options.addOption("sumNorm", false, "signal values are normalized to the same sequencing depth.(default: no)");
			options.addOption("sort", false, "sorted by the sum of all signals within each cluster (default: no)");
			options.addOption("mirror", false, "use mirror clustering: put the strong signals to one side of the peaks (default: no)");
	
			
			//deal with the headless error
//			Properties prop = System.getProperties (); 
//			prop.put ("java.awt.headless", "true"); 
//			System.setProperties (prop); 
			
			CommandLineParser parser = new GnuParser();
			CommandLine cmd;
			
			String peakfile1="";
			String peakfile2="";
			common.Localmode=true;
			common.NFSmode=false;
			int windowsize=4000;
			//parsing paramters
			try {
				String appPath=new File(".").getCanonicalPath()+"/";
				cmd = parser.parse( options, args);
				if(cmd.hasOption("threadnum"))
				{
					max_threadNum=Integer.parseInt(cmd.getOptionValue("threadnum"));
					common.threadNum=max_threadNum;
				}
				if(cmd.hasOption("winsize"))
				{
					windowsize=Integer.parseInt(cmd.getOptionValue("winsize"));
				}
				if(cmd.hasOption("print"))
					common.printMode=true;
				
				if(cmd.hasOption("log"))
				{
					System.out.println("log transform for the heatmap plotting.");
					heatmap_log=true;
				}
				if(cmd.hasOption("sort"))
				{
					System.out.println("sorted signal within each cluster for the heatmap plotting.");
					heatmap_sort=true;
				}
				if(cmd.hasOption("medianNorm"))
				{
					System.out.println("median normalization for the heatmap plotting.");
					heatmap_medianNorm=true;
				}
				if(cmd.hasOption("sumNorm"))
				{
					System.out.println("signal values are normalized to the same sequencing depth.");
					common.normalizedSignalSum=true;
				}
				if(cmd.hasOption("mirror"))
				{
					System.out.println("signal values are clustered after putting all strong signals at one side.");
					common.mirrorCluster=true;
				}
				
				if(cmd.hasOption("clusternum"))
				{
					common.ClusterNum=Integer.parseInt(cmd.getOptionValue("clusternum"));
				}
				if(cmd.hasOption("dataDir"))
				{
					common.dataDir=cmd.getOptionValue("dataDir");;
					logger.info("using dataDir:"+common.dataDir);
				}
				else {
					throw new ParseException("must provide dataDir !");
				}

				if(cmd.hasOption("peakfile1"))
				{
					peakfile1=cmd.getOptionValue("peakfile1");;
				}
				else
				{
					throw new ParseException("must provide peakfile1 !");
				}
				if(cmd.hasOption("peakfile2"))
				{
					peakfile2=cmd.getOptionValue("peakfile2");;
				}
				
				
				if(cmd.hasOption("outputDir"))
				{
					common.outputDir=cmd.getOptionValue("outputDir")+"/";
				}
				
				if(cmd.hasOption("clusterfeatures"))
				{
					String clusterfeatures=cmd.getOptionValue("clusterfeatures");
					selectedClusterFeature=new ArrayList<String>();
					selectedClusterFeature.addAll(Arrays.asList(clusterfeatures.split(",")));	
					
				}
				
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "PeakClassifier", options );
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			common.SynonymCheck=false;
			 NELGViewResult.reGen=true;
			FileUtils.deletePath(new File(common.tempDir));
			FileUtils.deletePath(new File(common.outputDir));
			//create directory
			(new File(common.outputDir)).mkdirs();
			(new File(common.tempDir)).mkdir();
			
			File datafolder=new File(common.dataDir);
			//////////////////prepare track record//////////////////
			TrackRecord peakTrack1=TrackRecord.createTrackRecord_peak(peakfile1);
			TrackRecord peakTrack2=null;
			if(peakfile2!="")
				peakTrack2=TrackRecord.createTrackRecord_peak(peakfile2);
			List<TrackRecord>  SignalPool=new ArrayList<TrackRecord>();
			for(File sfl:datafolder.listFiles())
			{
				if(!sfl.getName().endsWith("bigWig")&&!sfl.getName().endsWith("bigwig")&&!sfl.getName().endsWith("bw"))
					continue;
				SignalPool.add(TrackRecord.createTrackRecord_signal(sfl.getAbsolutePath()));
			}
			
			JPPFClient jppfCLient = null;
			FeatureSelectionJob FSJob=null;
			
			try
			{
			////////////////////single peak file clustering //////////////
			if(peakTrack2==null)
			{
				System.out.println("only one peak file, clustering analysis");
				 FSJob=new FeatureSelectionJob(peakTrack1,SignalPool,jppfCLient);
				 common.SignalRange=windowsize;
				 if(common.SignalRange==0)
					System.out.println("no fix window size!");
				common.AUC_cutoff=-1;
				//common.Corr_cutoff=-1;
				common.topNfeatures=1000000;
				NELGViewResult.stridesize=20;
				NELGViewResult.foldsize=1;
				NELGViewResult.bgFold=5;//remove bgsignal
				common.filterFeature=false;
				FeatureSelectionJob.featureExtractor=new EqualBinFeatureExtractor(20);
				FSJob.run();
			}
			else
			{
			//////////////////extract feature data////////////////////
			FeatureSelectionJob.resultsListener=new ClassificationResultListener();
			if(common.printMode)
			{
				 common.SignalRange=windowsize;
				 if(common.SignalRange==0)
					System.out.println("no fix window size!");
				common.AUC_cutoff=-1;
				//common.Corr_cutoff=-1;
				common.topNfeatures=1000000;
				NELGViewResult.stridesize=20;
				NELGViewResult.foldsize=1;
				NELGViewResult.bgFold=5;//remove bgsignal
				common.filterFeature=false;
				FeatureSelectionJob.featureExtractor=new EqualBinFeatureExtractor(20);
			}
		     FSJob=new FeatureSelectionJob(peakTrack1, peakTrack2,SignalPool,jppfCLient);
		    FSJob.run();
			}
			}
			catch (Exception E)
			{
					System.err.println(E.getMessage());
			}
		    /////////////////plot signal around peak///////////////////////
		    List<SimpleBEDFeature> query =new ArrayList<SimpleBEDFeature>( FSJob.target_signal_filtered);
		    query.addAll(FSJob.target_signal_bg);
		    HashSet<TrackRecord> SelectedSignalPool=new HashSet<TrackRecord>();
		    HashSet<String> selectedName=new HashSet<String>();
		    for (FeatureSignal itFea : FSJob.IsThereFeatures) {
		    	selectedName.add(itFea.FeatureId);
		    	
			}
//		    System.out.println(selectedName.toString());
//		    System.out.println("=============================");
		    for (TrackRecord signal_track : SignalPool) {
		    
				if(selectedName.contains(signal_track.ExperimentId))
				{
//					System.out.println(signal_track.ExperimentId);
					SelectedSignalPool.add(signal_track);
				}
			}
		    drawSignalAroundPeakBatch(SelectedSignalPool, peakTrack1.ExperimentId, query);
		    drawSignalAroundPeakBatch2(SelectedSignalPool, peakTrack1.ExperimentId, query);
		    /////////////////view result///////////////////////
		    String resultfile="";
		    NELGViewResult.outputDir=common.outputDir;
		    File outdir=new File(common.outputDir);
		    for(File fl :outdir.listFiles())
		    {
		    	if(fl.getName().indexOf(peakTrack1.ExperimentId)>-1&&fl.getName().endsWith("cr"))
		    	{
		    		resultfile=fl.getAbsolutePath();
				    FileInputStream fileIn;
					 try {
						fileIn = new FileInputStream(resultfile);
						 ObjectInputStream in = new ObjectInputStream(fileIn);
						 ClassificationResult Result=(ClassificationResult)in.readObject();
						
						 NELGViewResult.ViewClassificationResult(Result);
						//write cluster BED
						 if(common.ClusterNum>1&&NELGViewResult.clusterIdvec!=null&&NELGViewResult.clusterIdvec.size()==FSJob.target_signal_filtered.size())
						 {
							 System.out.println("write out the clustered BED file");
							 for (int i = 0; i < NELGViewResult.clusterIdvec.size(); i++) {
								 FSJob.target_signal_filtered.get(i).setScore((float) NELGViewResult.clusterIdvec.get(i));
							}
							 SimpleBEDFeature.toFile(FSJob.target_signal_filtered, peakfile1+".clust"); 
						 }
					} catch (Exception e) {
						// TODO Auto-generated catch block
						System.out.println(e.getMessage());
					} 
		    		
		    	}
		    }
		    drawSignalAroundClustBatch(SelectedSignalPool, peakTrack1.ExperimentId, query, NELGViewResult.clusterIdvec);
		    //drawSignalForDNase(SelectedSignalPool, peakTrack1.ExperimentId, query, NELGViewResult.clusterIdvec);
		}
}
