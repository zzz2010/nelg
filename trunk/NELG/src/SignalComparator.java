import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.broad.tribble.bed.BEDFeature;

import auc.AUCCalculator;
import auc.Confusion;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.doublealgo.Sorting;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.util.Pair;

public class SignalComparator {

	//AUC score
	public static float getDiscriminativeCapbaility(DoubleMatrix1D feature_signal, DoubleMatrix1D target_class)
	{
		double[] scores=new double[feature_signal.size()];
		double[] scores2=new double[feature_signal.size()];
		int[] labels=new int[target_class.size()];
		int[] labels2=new int[target_class.size()];
		//disable the AUC program output
//		PrintStream original = new PrintStream(System.out);
//		try {
//			System.setOut(new PrintStream(new FileOutputStream("/dev/null")));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		Random rand=new Random(12345);
		
		TreeMap<Double,Integer> Sorted_labels=new TreeMap<Double,Integer>();
		
		for (int i = 0; i < labels.length; i++) {
			if(target_class.get(i)>0)
			    Sorted_labels.put(feature_signal.get(i)+rand.nextDouble()*0.00001, 1);
			else
				Sorted_labels.put(feature_signal.get(i)+rand.nextDouble()*0.00001, 0);
		}
	  	 int ii=0;
       	 int one=0;
		 for(Double key:Sorted_labels.descendingKeySet())
       	 {
       		 labels[ii]=Sorted_labels.get(key);
       		 labels2[labels2.length-ii-1]=labels[ii];
       		 if(labels[ii]==1)
       			 one++;
       		 scores[ii]=key;
       		scores2[labels2.length-ii-1]=scores[ii];
       		        ii++;
       	 }
		 
		 //only the label order matters!
		Confusion AUCcalc=AUCCalculator.readArrays(labels, scores);	
		double auc=AUCcalc.calculateAUCROC();	
		
		AUCcalc=AUCCalculator.readArrays(labels2, scores2);	
		double auc2=AUCcalc.calculateAUCROC();

		
		if(auc2>auc)
			auc=auc2;
		
//		System.setOut(original);
		return (float) auc;
	}
	


	//spearman correlation
	public static float getCorrelation(DoubleMatrix1D feature_signal,DoubleMatrix1D target_class)
	{
		if(feature_signal.zSum()==0)
			return 0;
		if(target_class.zSum()==0)
			return 0;
		
		SpearmansCorrelation corr=new SpearmansCorrelation();
		double spearman=corr.correlation(feature_signal.toArray(), target_class.toArray());
		if(Double.isInfinite(spearman)||Double.isNaN(spearman))
			return 0;
		return (float) Math.abs(spearman);
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
	
	
}
