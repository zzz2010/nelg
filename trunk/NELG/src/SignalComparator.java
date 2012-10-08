import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.broad.tribble.bed.BEDFeature;


public class SignalComparator {

	public static float getDiscriminativeCapbaility(List<Float> feature_signal, List<Float> target_class)
	{
		double costRatio=100;//penalty higher for false positive
		int postiveNum=0;
		for (int i = 0; i < target_class.size(); i++) {
			if(target_class.get(i)>0)
				postiveNum+=1;
		}
		int negativeNum=target_class.size()-postiveNum;
		HashMap<Float, Float> scorePairs=new HashMap<Float, Float>();
		 SortedSet<Entry<Float, Float>> sortedset=entriesSortedByValues(scorePairs);
		 int[] cumPositives=new int[sortedset.size()];
		 int ii=0;
		for(Entry<Float, Float> pair:sortedset)
		{
			if(pair.getValue()>0)
			{
				if(ii>0)
				cumPositives[ii]=cumPositives[ii-1]+1;
			}				
		}
		double bestPrecision=0;
		for (int i = 1; i < cumPositives.length; i++) {
			int Precision = (postiveNum-cumPositives[i])/(cumPositives.length-i);
			if(Precision>bestPrecision)
			{
				bestPrecision=Precision;	
			}
		}
		return (float) bestPrecision;
	}
	


	//spearman correlation
	public static float getCorrelation(List<Float> feature_signal, List<Float> target_class)
	{
		 double[] ranks = new double[feature_signal.size()];
		    
		 double[] ranks_null = new double[feature_signal.size()];
		 
		    for (int i=0; i<feature_signal.size(); i++)
		    {
		        ranks[i]=((double) Collections.binarySearch(feature_signal, target_class.get(i)));//indexOf(PWMScore.get(i)));
		        ranks_null[i]=((double)i);
		    }
		    float spearman=(float) getPearsonCorrelation(ranks, ranks_null);
		return spearman;
	}
	
	
	
	
	public static double getPearsonCorrelation(double[] scores1,double[] scores2){
        double result = 0;
        double sum_sq_x = 0;
        double sum_sq_y = 0;
        double sum_coproduct = 0;
        double mean_x = scores1[0];
        double mean_y = scores2[0];
        for(int i=2;i<scores1.length+1;i+=1){
            double sweep =Double.valueOf(i-1)/i;
            double delta_x = scores1[i-1]-mean_x;
            double delta_y = scores2[i-1]-mean_y;
            sum_sq_x += delta_x * delta_x * sweep;
            sum_sq_y += delta_y * delta_y * sweep;
            sum_coproduct += delta_x * delta_y * sweep;
            mean_x += delta_x / i;
            mean_y += delta_y / i;
        }
        double pop_sd_x = (double) Math.sqrt(sum_sq_x/scores1.length);
        double pop_sd_y = (double) Math.sqrt(sum_sq_y/scores1.length);
        double cov_x_y = sum_coproduct / scores1.length;
        result = cov_x_y / (pop_sd_x*pop_sd_y);
        return result;
    }
	
	public 	static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
                new Comparator<Map.Entry<K,V>>() {
                    @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                        int res = e1.getValue().compareTo(e2.getValue());
                        return res != 0 ? res : 1; // Special fix to preserve items with equal values
                    }
                }
            );
            sortedEntries.addAll(map.entrySet());
            return sortedEntries;
        }
}
