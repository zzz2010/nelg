import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class SimulationTrack {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//initialize
		FileStorageAdapter db=new FileStorageAdapter("./data");
		TrackRecord target=db.getTrackById("wgEncodeBroadHistoneK562H3k4me3");
		//get target peak list
		List<SimpleBEDFeature> target_peaks=target.getPeakData();
		List<TrackRecord> signalPool=new ArrayList<TrackRecord>();
		signalPool.add(target);
		//make 10 BG tracks
		int bgnum=10;
		for (int i = 0; i < bgnum; i++) {
			List<SimpleBEDFeature> temp=makeTrack(target_peaks,0,((double)i)/bgnum,0.5,target_peaks.size(),i%8);
			signalPool.add(parseTR(temp, "bg"+i));
		}
		//make 5 isthere tracks
		int isnum=5;
		for (int i = 0; i < isnum; i++) {
			List<SimpleBEDFeature> temp=makeTrack(target_peaks,((double)i)/(isnum-1)/2+0.5,0,((double)i)/(isnum-1),target_peaks.size(),i%8);
			signalPool.add(parseTR(temp, "isthere"+i));
		}
		int valnum=5;
		//make 5 valthere tracks
		for (int i = 0; i < valnum; i++) {
			List<SimpleBEDFeature> temp=makeTrack(target_peaks,((double)i)/(valnum-1)/2,((double)i)/(valnum-1)/2+0.5,((double)i)/(valnum-1),target_peaks.size(),i%8);
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
		tr.PeakCache=peaks;
		return tr;
	}
	
	public static List<SimpleBEDFeature> makeTrack(List<SimpleBEDFeature> peaks, double isthereRatio, double valthereRatio,double strandbias, int num, int distanceBin)
	{
		List<SimpleBEDFeature> signals=new ArrayList<SimpleBEDFeature>(num);
		Random rand=new Random(12345);
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

}
