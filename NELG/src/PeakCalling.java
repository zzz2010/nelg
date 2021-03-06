import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.ResourceBundle.Control;

import org.broad.tribble.annotation.Strand;



import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.jet.random.Poisson;
import cern.jet.stat.Gamma;
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
		Random rand=new Random(1);
		boolean lookForMax = true;

		Integer pos = 0;
		for (double value : values.toArray()) {
			if (maximum == null||value > maximum ) {
				maximum = (float) value;
				maximumPos = indices.get(pos);
			}
			else if(value>0&&value == maximum&&pos<indices.size()-1)
			{
				if( rand.nextBoolean())
				   maximumPos =indices.get(pos); //shift to the middle of the plain 
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
	public static List<SimpleBEDFeature> simple_peak_detection(List<SparseDoubleMatrix1D> values,List<SimpleBEDFeature> regions)
	{
		List<SimpleBEDFeature> PeakList=new ArrayList<SimpleBEDFeature>();

		double sumVals=1;
		for (int i = 0; i < regions.size(); i++) {

			sumVals+=values.get(i).zSum();
		}
		for (int i = 0; i < regions.size(); i++) {
			List<Integer> indices = new ArrayList<Integer>();
			for (int j=0; j<values.get(i).size(); j++) {
				indices.add(j);
			}
			List<Map<Integer, Float>> tempPeakList=peak_detection(values.get(i), 0.8, indices);
			Map<Integer, Float> maxPoints = tempPeakList.get(0);
			Iterator<Entry<Integer, Float>> iter = maxPoints.entrySet().iterator();
//			double std_bg=sd(values.get(i));
			double lamda=mean(values.get(i));
			double stepsize=Math.ceil((regions.get(i).getEnd()-regions.get(i).getStart())/(double)values.get(i).size());
			while(iter.hasNext())
			{
				Entry<Integer, Float> tempP = iter.next();
				//float MACSscore=(float) logPoissionCDF(lamda,tempP.getValue().intValue());
			
				float MACSscore=(float) (tempP.getValue()/(lamda+stepsize));//(float) ((tempP.getValue()-lamda)/std_bg);
				
				if(MACSscore>2)//arbitary cut-off
				{
					
					int pos=(int) (regions.get(i).getStart()+tempP.getKey()*stepsize+0.5*stepsize);

					SimpleBEDFeature peak=new SimpleBEDFeature((int)(pos-0.5*stepsize), (int)(pos+0.5*stepsize), regions.get(i).getChr());
					peak.setScore(MACSscore);
					peak.setDescription("binId:"+tempP.getKey()+"\traw:"+values.get(i).get(tempP.getKey())+"\tmean:"+lamda);
					PeakList.add(peak);
				}
			}
			
		}
		
		return PeakList;
	}
	
	
	
	public static List<SimpleBEDFeature> simple_peak_detection(List<SparseDoubleMatrix1D> values,List<SparseDoubleMatrix1D> control,List<SimpleBEDFeature> regions)
	{
		List<SimpleBEDFeature> PeakList=new ArrayList<SimpleBEDFeature>();
		double sumCtrl=1;
		double sumVals=1;
		for (int i = 0; i < regions.size(); i++) {
			sumCtrl+=control.get(i).zSum();
			sumVals+=values.get(i).zSum();
		}
		double ratio=sumVals/sumCtrl;
		for (int i = 0; i < regions.size(); i++) {
			List<Integer> indices = new ArrayList<Integer>();
			SparseDoubleMatrix1D controlVal = control.get(i);
			for (int j=0; j<values.get(i).size(); j++) {
				indices.add(j);
				controlVal.setQuick(j,controlVal.getQuick(j)*ratio);
			}
			List<Map<Integer, Float>> tempPeakList=peak_detection(values.get(i), 0.8, indices);
			Map<Integer, Float> maxPoints = tempPeakList.get(0);
			Iterator<Entry<Integer, Float>> iter = maxPoints.entrySet().iterator();
			
			double lamda_bg=mean(values.get(i));
			double std_bg=sd(values.get(i));
			
			
			while(iter.hasNext())
			{
				Entry<Integer, Float> tempP = iter.next();
				double lamda=lamda_bg;
				if(lamda<controlVal.getQuick(tempP.getKey()))
					lamda=controlVal.getQuick(tempP.getKey());
				double std=sd(controlVal);
				if(std>std_bg)
					std_bg=std;
				
				//float MACSscore=(float) logPoissionCDF(lamda,tempP.getValue().intValue());
					
				
				float MACSscore=(float) ((tempP.getValue()-lamda)/std_bg);
				
				float stepsize=(regions.get(i).getEnd()-regions.get(i).getStart())/values.get(i).size();
				if(MACSscore>2)//arbitary cut-off
				{
					int pos=(int) (regions.get(i).getStart()+tempP.getKey()*stepsize+0.5*stepsize);
					SimpleBEDFeature peak=new SimpleBEDFeature(pos, pos+1, regions.get(i).getChr());
					peak.setScore(MACSscore);
					peak.setStrand(Strand.POSITIVE);
					if(tempP.getKey()==(values.get(i).size()-1)||(tempP.getKey()>0&&values.get(i).get(tempP.getKey()+1)<values.get(i).get(tempP.getKey()-1)))
					{
						peak.setStrand(Strand.NEGATIVE);
					}
					peak.setDescription("binId:"+tempP.getKey()+"\traw:"+values.get(i).get(tempP.getKey())+"\tmean:"+lamda);
					PeakList.add(peak);
				}
			}
			
		}
		
		return PeakList;
	}
	
	
	public static List<SimpleBEDFeature> random_peak_detection(List<SparseDoubleMatrix1D> values,List<SimpleBEDFeature> regions)
	{
		List<SimpleBEDFeature> PeakList=new ArrayList<SimpleBEDFeature>();
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
	  public static double logPoissionCDF(double lamda, int k)
	  {
		  double pvalue=0;
		  if(lamda<700)
		  {
		  pvalue=Gamma.incompleteGamma((double)(k+1) ,lamda);
		
		  }
		  else
		  {
			  pvalue=__poisson_cdf_Q_large_lambda ( k,lamda);
		  }
		  double logpvalue=  -10*Math.log10(pvalue);
		  return logpvalue;
	  }
	  
	  public static double __poisson_cdf_Q_large_lambda (int k,double a)
	  {
		 double LSTEP = 200;
		 double EXPTHRES = Math.exp(LSTEP);
		 double EXPSTEP  = Math.exp(-LSTEP);

		    if (k < 0)
		        return 1 ;                   
		   int num_parts = (int) (a/LSTEP);
		   int last_part = (int) (a % LSTEP);
		   double lastexp = Math.exp(-last_part);
		   double  next = EXPSTEP;
		    num_parts -= 1;

		    for(int i=1; i<k+1;i++)
		    {
		       double last = next;
		        next = last * a / i;
		        if (next > EXPTHRES)
		        {
		           if (num_parts>=1)
		           {
		               next *= EXPSTEP;
		               num_parts -= 1;
		           }
		           else
		           {
		               lastexp = 1;
		           }
		        }
		    }
		   double cdf = 0;
		   int i = k+1;
		    while(next >0)
		    {
		       double last = next;
		        next = last * a / i;
		        cdf += next;
		        i+=1;
		        if (next > EXPTHRES || cdf > EXPTHRES)
		        {
		           if (num_parts>=1)
		           {
		               cdf *= EXPSTEP;
		               next *= EXPSTEP;
		               num_parts -= 1;
		           }
		           else
		           {
		               cdf *= lastexp;
		               lastexp = 1;
		           }
		        }
		    }
		    for( i=0;i<num_parts;i++)
		    {
		        cdf *= EXPSTEP;
		    }
		    cdf *= lastexp;
		    return cdf;
	  
	  }
	  

}
