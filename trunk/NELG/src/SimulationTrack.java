import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class SimulationTrack {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	
	public static List<SimpleBEDFeature> makeTrack(List<SimpleBEDFeature> peaks, double isthereRatio, double valthereStd,double strandbias, int num, int distanceBin)
	{
		List<SimpleBEDFeature> signals=new ArrayList<SimpleBEDFeature>(num);
		Random rand=new Random(12345);
		int offset=(int) (50*Math.pow(2, distanceBin)-50);
		int binsize=(int) (50*Math.pow(2, distanceBin));
		for (int i = 0; i < num; i++) {
			int peakid=rand.nextInt(peaks.size());
			String chrom=peaks.get(peakid).getChr();
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
