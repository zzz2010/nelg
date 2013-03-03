import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.PropertyConfigurator;
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
		 
		 
			public static void drawSignalAroundPeakBatch(Collection<TrackRecord> signalPool,String targetName,List<SimpleBEDFeature> query)
			{
				DoubleMatrix1D targetValue = SignalTransform.BedFeatureToValues(query);
				EqualBinFeatureExtractor FE=new EqualBinFeatureExtractor(8);
				for (TrackRecord feat : signalPool) {
					
					DoubleMatrix2D temp=FE.extractSignalFeature(feat, query);

					NELGViewResult.drawSignalAroundPeakCurve(temp, targetValue, feat.ExperimentId, targetName);
				}
					
			}
			
		public static void main(String[] args) {
			PropertyConfigurator.configure( "./log4j.properties" ); 
			// TODO Auto-generated method stub
			Options options = new Options();
			options.addOption("threadnum", true, "maximum thread number");
			options.addOption("dataDir", true, "folder with bigWig data");
			options.addOption("outputDir", true, "folder for output data");
			options.addOption("peakfile1", true, "first set of peaks(bed format)");
			options.addOption("peakfile2", true, "second set of peaks(bed format)");
	
			CommandLineParser parser = new GnuParser();
			CommandLine cmd;
			
			String peakfile1="";
			String peakfile2="";
			common.Localmode=true;
			common.NFSmode=false;
			
			//parsing paramters
			try {
				String appPath=new File(".").getCanonicalPath()+"/";
				cmd = parser.parse( options, args);
				if(cmd.hasOption("threadnum"))
				{
					max_threadNum=Integer.parseInt(cmd.getOptionValue("threadnum"));
					common.threadNum=max_threadNum;
				}
				if(cmd.hasOption("dataDir"))
				{
					common.dataDir=cmd.getOptionValue("dataDir");;
					logger.info("using dataDir:"+common.dataDir);
				}

				if(cmd.hasOption("peakfile1"))
				{
					peakfile1=cmd.getOptionValue("peakfile1");;
				}
				if(cmd.hasOption("peakfile2"))
				{
					peakfile2=cmd.getOptionValue("peakfile2");;
				}
				
				
				if(cmd.hasOption("outputDir"))
				{
					common.outputDir=cmd.getOptionValue("outputDir");
				}
				
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "PeakClassifier", options );
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			common.SynonymCheck=false;
			FileUtils.deletePath(new File(common.tempDir));
			FileUtils.deletePath(new File(common.outputDir));
			//create directory
			(new File(common.outputDir)).mkdir();
			(new File(common.tempDir)).mkdir();
			
			File datafolder=new File(common.dataDir);
			//////////////////prepare track record//////////////////
			TrackRecord peakTrack1=TrackRecord.createTrackRecord_peak(peakfile1);
			TrackRecord peakTrack2=TrackRecord.createTrackRecord_peak(peakfile2);
			List<TrackRecord>  SignalPool=new ArrayList<TrackRecord>();
			for(File sfl:datafolder.listFiles())
			{
				if(!sfl.getName().endsWith("bigWig"))
					continue;
				SignalPool.add(TrackRecord.createTrackRecord_signal(sfl.getAbsolutePath()));
			}
			//////////////////extract feature data////////////////////
			FeatureSelectionJob.resultsListener=new ClassificationResultListener();
				 JPPFClient jppfCLient = null;
		    FeatureSelectionJob FSJob=new FeatureSelectionJob(peakTrack1, peakTrack2,SignalPool,jppfCLient);
		    FSJob.run();
		    /////////////////plot signal around peak///////////////////////
		    List<SimpleBEDFeature> query =new ArrayList<SimpleBEDFeature>( FSJob.target_signal_filtered);
		    query.addAll(FSJob.target_signal_bg);
		    HashSet<TrackRecord> SelectedSignalPool=new HashSet<TrackRecord>();
		    HashSet<String> selectedName=new HashSet<String>();
		    for (FeatureSignal itFea : FSJob.IsThereFeatures) {
		    	selectedName.add(itFea.FeatureId);
		    	System.out.println(itFea.FeatureId);
			}
		    System.out.println("=============================");
		    for (TrackRecord signal_track : SignalPool) {
		    	System.out.println(signal_track.ExperimentId);
				if(selectedName.contains(signal_track.ExperimentId))
				{
					SelectedSignalPool.add(signal_track);
				}
			}
		    drawSignalAroundPeakBatch(SelectedSignalPool, peakTrack1.ExperimentId, query);
		    /////////////////view result///////////////////////
		    String resultfile="";
		    File outdir=new File(common.outputDir);
		    for(File fl :outdir.listFiles())
		    {
		    	if(fl.getName().indexOf(peakTrack1.ExperimentId)>-1&&fl.getName().endsWith("cr"))
		    	{
		    		resultfile=fl.getAbsolutePath();
		    		break;
		    	}
		    }
		    FileInputStream fileIn;
			 try {
				fileIn = new FileInputStream(resultfile);
				 ObjectInputStream in = new ObjectInputStream(fileIn);
				 ClassificationResult Result=(ClassificationResult)in.readObject();
				 NELGViewResult.reGen=true;
				 NELGViewResult.ViewClassificationResult(Result);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println(e.getMessage());
			} 
		    
		}
}
