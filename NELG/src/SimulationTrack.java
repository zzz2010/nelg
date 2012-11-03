import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.PropertyConfigurator;
import org.jppf.utils.FileUtils;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class SimulationTrack {

	static FileStorageAdapter db;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//initialize
		PropertyConfigurator.configure( "./log4j.properties" ); 
		Options options = new Options();
		 db=new FileStorageAdapter("./data");
		 options.addOption("debug", false, "use debug folder");
			CommandLineParser parser = new GnuParser();
			CommandLine cmd;
		 try {
			cmd = parser.parse( options, args);
			if(cmd.hasOption("debug"))
			{
				common.outputDir="./result_debug/";
				common.tempDir="./cache_debug/";
//				FileUtils.deletePath(new File(common.outputDir));
//				FileUtils.deletePath(new File(common.tempDir));
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//create directory
		(new File(common.outputDir)).mkdir();
		(new File(common.tempDir)).mkdir();
		
		test();
		
		TrackRecord target=db.getTrackById("wgEncodeBroadHistoneK562H3k4me3");
		//get target peak list
		List<SimpleBEDFeature> target_peaks=target.getPeakData();
		List<TrackRecord> signalPool=new ArrayList<TrackRecord>();
		signalPool.add(target);
		//make 10 BG tracks
		int bgnum=10;
//		for (int i = 0; i < bgnum; i++) {
//			List<SimpleBEDFeature> temp=makeTrack(target_peaks,0,((double)i)/bgnum,0.5,target_peaks.size()*10,i%8);
//			signalPool.add(parseTR(temp, "bg"+i));
//		}
//		//make 5 isthere tracks
//		int isnum=5;
//		for (int i = 0; i < isnum; i++) {
//			List<SimpleBEDFeature> temp=makeTrack(target_peaks,((double)i)/(isnum-1)/2+0.5,0,((double)i)/(isnum-1),target_peaks.size(),i%8);
//			signalPool.add(parseTR(temp, "isthere"+i));
//		}
		int valnum=5;
		//make 5 valthere tracks
		for (int i = 0; i < valnum; i++) {
			List<SimpleBEDFeature> temp=makeTrack(target_peaks,((double)i)/(valnum-1)/2+0.5,((double)i)/(valnum-1)/2+0.5,((double)i)/(valnum-1),target_peaks.size()*10,i%8);
			signalPool.add(parseTR(temp, "valthere"+i));
		}
		common.predictTarget_debug="wgEncode";
		common.selectFeature_debug="Simulation";
		MotherModeler MainModelMachine=new MotherModeler(signalPool);
		MainModelMachine.Run();
		
	}
	
	public static TrackRecord parseTR(List<SimpleBEDFeature> peaks, String name)
	{
		TrackRecord tr=new TrackRecord();
		tr.FilePrefix="Simulation"+name;
		tr.ExperimentId=name;
		tr.ExperimentType="Simulation";
		tr.hasPeak=true;
		tr.hasSignal=false;
		tr.DBoperator=db;
		tr.PeakCache=peaks;
		return tr;
	}
	
	public static List<SimpleBEDFeature> makeTrack(List<SimpleBEDFeature> peaks, double isthereRatio, double valthereRatio,double strandbias, int num, int distanceBin)
	{
//		System.out.println(isthereRatio+","+valthereRatio+","+strandbias+","+distanceBin);
		List<SimpleBEDFeature> signals=new ArrayList<SimpleBEDFeature>(num);
		Random rand=new Random();
		int offset=(int) (50*Math.pow(2, distanceBin)-50);
		int binsize=(int) (50*Math.pow(2, distanceBin));
		double maxscore=0;
		for (int i = 0; i < num; i++) {
			int peakid=rand.nextInt(peaks.size());
			String chrom=peaks.get(peakid).getChr();
			float score=peaks.get(peakid).getScore();
			if(score>maxscore)
				maxscore=score;
			int midpos=(int)(peaks.get(peakid).getEnd()+peaks.get(peakid).getStart())/2;
			if(rand.nextDouble()<isthereRatio)
			{
				int randpos=0;
				if(midpos<offset)
				{
					randpos=midpos+offset+rand.nextInt(binsize);
				}
				else
				{
					if(rand.nextDouble()<strandbias)
					{
						randpos=midpos+offset+rand.nextInt(binsize);
					}
					else
					{
						randpos=midpos-offset-rand.nextInt(binsize);
					}	
				}
				SimpleBEDFeature temp=new SimpleBEDFeature(randpos, randpos+1, chrom);
				if(rand.nextDouble()<valthereRatio)
					temp.setScore(score);
				else
				{
					temp.setScore((float) (rand.nextFloat()*maxscore));
				}
				signals.add(temp);
			}
			else
			{
				int randpos=rand.nextInt(100000000);
				SimpleBEDFeature temp=new SimpleBEDFeature(randpos, randpos+1, chrom);
				temp.setScore(rand.nextFloat());
				signals.add(temp);
			}
		}
		
		return signals;
	}
	
	static void test()
	{
		TrackRecord target=db.getTrackById("wgEncodeBroadHistoneK562H3k4me3");
		MultiScaleFeatureExtractor featureExtractor=new MultiScaleFeatureExtractor(8);
		List<SimpleBEDFeature> target_peaks=target.getPeakData();
		 DoubleMatrix1D target_val = SignalTransform.BedFeatureToValues(target_peaks);
		 int dbin=4;
		List<SimpleBEDFeature> temp=makeTrack(target_peaks,1,1,1,10*target_peaks.size(),4);
		TrackRecord feature_signal = parseTR(temp, "test");
		SparseDoubleMatrix2D feature_BinSignal = featureExtractor.extractSignalFeature(feature_signal,target_peaks.subList(0, 10000));
		DoubleMatrix1D signal = feature_BinSignal.viewColumn(3*dbin+2);
		for (int i = 0; i < signal.size(); i++) {
			System.out.println(signal.get(i)+"\t"+target_peaks.get(i).getScore());
		}
		System.out.print(SignalComparator.getCorrelation(signal, target_val.viewPart(0,10000)));
		
		System.exit(1);
		return;
	}

}
