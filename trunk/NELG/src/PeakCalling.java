import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.broad.tribble.bed.BEDFeature;
import org.broad.tribble.bed.FullBEDFeature;
import org.broad.tribble.bed.SimpleBEDFeature;

import cern.colt.matrix.impl.SparseDoubleMatrix1D;
public class PeakCalling {
	/**
	 * Detects peaks (calculates local minima and maxima) in the 
	 * vector <code>values</code>. The resulting list contains
	 * maxima at the first position and minima at the last one.
	 * 
	 * Maxima and minima maps contain the indice value for a
	 * given position and the value from a corresponding vector.
	 * 
	 * A point is considered a maximum peak if it has the maximal
	 * value, and was preceded (to the left) by a value lower by
	 * <code>delta</code>.
	 * 
	 * @param values Vector of values for whom the peaks should be detected
	 * @param delta The precedor of a maximum peak
	 * @param indices Vector of indices that replace positions in resulting maps
	 * @return List of maps (maxima and minima pairs) of detected peaks
	 */
	public static <U> List<Map<U, Float>> peak_detection(SparseDoubleMatrix1D values, double delta, List<U> indices)
	{
		assert(indices != null);
		assert(values.size() != indices.size());

		Map<U, Float> maxima = new HashMap<U, Float>();
		Map<U, Float> minima = new HashMap<U, Float>();
		List<Map<U, Float>> peaks = new ArrayList<Map<U, Float>>();
		peaks.add(maxima);
		peaks.add(minima);

		Float maximum = null;
		Float minimum = null;
		U maximumPos = null;
		U minimumPos = null;

		boolean lookForMax = true;

		Integer pos = 0;
		for (double value : values.toArray()) {
			if (maximum == null||value > maximum ) {
				maximum = (float) value;
				maximumPos = indices.get(pos);
			}

			if (minimum == null||value < minimum ) {
				minimum = (float) value;
				minimumPos = indices.get(pos);
			}

			if (lookForMax) {//start to drop
				if (value < maximum*(1- delta)) {
					maxima.put(maximumPos, (float) maximum);
					minimum = (float) value;
					minimumPos = indices.get(pos);
					lookForMax = false;
				}
			} else {
				if (value > minimum*(1+delta)) {
					minima.put(minimumPos, (float) minimum);
					maximum = (float) value;
					maximumPos = indices.get(pos);
					lookForMax = true;
				}
			}

			pos++;
		}

		return peaks;
	}

	/**
	 * Detects peaks (calculates local minima and maxima) in the 
	 * vector <code>values</code>. The resulting list contains
	 * maxima at the first position and minima at the last one.
	 * 
	 * Maxima and minima maps contain the position for a
	 * given value and the value itself from a corresponding vector.
	 * 
	 * A point is considered a maximum peak if it has the maximal
	 * value, and was preceded (to the left) by a value lower by
	 * <code>delta</code>.
	 * 
	 * @param values Vector of values for whom the peaks should be detected
	 * @param delta The precedor of a maximum peak
	 * @return List of maps (maxima and minima pairs) of detected peaks
	 */
	public static List<BEDFeature> simple_peak_detection(List<SparseDoubleMatrix1D> values,List<BEDFeature> regions)
	{
		List<BEDFeature> PeakList=new ArrayList<BEDFeature>();
		
		for (int i = 0; i < regions.size(); i++) {
			List<Integer> indices = new ArrayList<Integer>();
			for (int j=0; j<values.get(i).size(); j++) {
				indices.add(j);
			}
			List<Map<Integer, Float>> tempPeakList=peak_detection(values.get(i), 0.8, indices);
			Map<Integer, Float> maxPoints = tempPeakList.get(0);
			Iterator<Entry<Integer, Float>> iter = maxPoints.entrySet().iterator();
			double std=sd(values.get(i))+1;
			double m=mean(values.get(i));
			while(iter.hasNext())
			{
				Entry<Integer, Float> tempP = iter.next();
				float zscore=(float) ((tempP.getValue()-m)/std);
				float stepsize=(regions.get(i).getEnd()-regions.get(i).getStart())/values.get(i).size();
				if(zscore>3)//arbitary cut-off
				{
					int pos=(int) (regions.get(i).getStart()+tempP.getKey()*stepsize+0.5*stepsize);
					SimpleBEDFeature peak=new SimpleBEDFeature(pos, pos+1, regions.get(i).getChr());
					peak.setScore(zscore);
					peak.setDescription("binId:"+tempP.getKey()+"\traw:"+values.get(i).get(tempP.getKey())+"\tmean:"+m+"\tstd:"+std);
					PeakList.add(peak);
				}
			}
			
		}
		
		return PeakList;
	}
	
	
	
	public static List<BEDFeature> simple_peak_detection(List<SparseDoubleMatrix1D> values,List<SparseDoubleMatrix1D> control,List<BEDFeature> regions)
	{
		List<BEDFeature> PeakList=new ArrayList<BEDFeature>();
		
		for (int i = 0; i < regions.size(); i++) {
			List<Integer> indices = new ArrayList<Integer>();
			SparseDoubleMatrix1D controlVal = control.get(i);
			double sumcontrl=controlVal.zSum()+controlVal.size();
			for (int j=0; j<values.get(i).size(); j++) {
				indices.add(j);
				controlVal.setQuick(j,values.get(i).getQuick(j)*sumcontrl/(controlVal.getQuick(j)+1));
			}
			List<Map<Integer, Float>> tempPeakList=peak_detection(controlVal, 0.8, indices);
			Map<Integer, Float> maxPoints = tempPeakList.get(0);
			Iterator<Entry<Integer, Float>> iter = maxPoints.entrySet().iterator();
			double std=sd(values.get(i))+1;
			double m=mean(values.get(i));
			while(iter.hasNext())
			{
				Entry<Integer, Float> tempP = iter.next();
				float zscore=(float) ((tempP.getValue()-m)/std);
				float stepsize=(regions.get(i).getEnd()-regions.get(i).getStart())/values.get(i).size();
				if(zscore>3)//arbitary cut-off
				{
					int pos=(int) (regions.get(i).getStart()+tempP.getKey()*stepsize+0.5*stepsize);
					SimpleBEDFeature peak=new SimpleBEDFeature(pos, pos+1, regions.get(i).getChr());
					peak.setScore(zscore);
					peak.setDescription("binId:"+tempP.getKey()+"\traw:"+values.get(i).get(tempP.getKey())+"\tmean:"+m+"\tstd:"+std);
					PeakList.add(peak);
				}
			}
			
		}
		
		return PeakList;
	}
	
	
	public static List<BEDFeature> random_peak_detection(List<SparseDoubleMatrix1D> values,List<BEDFeature> regions)
	{
		List<BEDFeature> PeakList=new ArrayList<BEDFeature>();
		Random rand=new Random(); 
		for (int i = 0; i < regions.size(); i++) {

			float stepsize=(regions.get(i).getEnd()-regions.get(i).getStart())/values.get(i).size();
			int tempP=rand.nextInt(values.get(i).size());
			int pos=(int) (regions.get(i).getStart()+tempP*stepsize+0.5*stepsize);
			SimpleBEDFeature peak=new SimpleBEDFeature(pos, pos+1, regions.get(i).getChr());
			peak.setScore(rand.nextFloat());
			PeakList.add(peak);
			
		}
		
		return PeakList;
	}
	
	
	public static double mean (SparseDoubleMatrix1D a){
		
		        int sum = sum(a);
		
		        double mean = 0;
		
		        mean = sum / (a.size() * 1.0);
		
		        return mean;
		
		    }

	public static double sd (SparseDoubleMatrix1D a){
		
		        int sum = 0;
		
		        double mean = mean(a);
		
		        for (double i : a.toArray())
		
		            sum += Math.pow((i - mean), 2);
		
		        return Math.sqrt( sum / ( a.size()  ) ); // sample
		
		    }
	
	  public static int sum (SparseDoubleMatrix1D a){
		  	   if (a.size() > 0) 
		  	   {
		  		int sum = 0;
		     
		  		for (double i : a.toArray()) 
		  		{
		  		    sum += i;
		  		}
		  
		              return sum;
		  		 }
		  
		          return 0;
		  
		      }


}
