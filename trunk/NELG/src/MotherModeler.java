import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.broad.tribble.bed.BEDFeature;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

//this class mainly do initial feature selection and job assignment
public class MotherModeler {
	List<TrackRecord> SignalPool;
	int threadNum=10;
	public MotherModeler(List<TrackRecord> signalPool) {
		super();
		SignalPool = signalPool;
	}
	
	public void Run()
	{	
		PooledExecutor executor = new PooledExecutor(new LinkedQueue());
		executor.setMinimumPoolSize(threadNum);
		executor.setKeepAliveTime(1000 * 60*5 );
		
		
		//take out one as class label, the rest as feature data
		for (TrackRecord target_signal : SignalPool) {
			
			//get filtered target signal
		  	ArrayList<BEDFeature>target_signal_filtered= SignalTransform.extractPositveSignal(target_signal);
		  	ArrayList<BEDFeature>target_signal_bg = SignalTransform.extractNegativeSignal(target_signal_filtered,2*target_signal_filtered.size());
		  	ArrayList<Float> targetValue=SignalTransform.BedFeatureToValues(target_signal_filtered);
		  	targetValue.addAll(SignalTransform.BedFeatureToValues(target_signal_bg));
		  	ArrayList<Float> targetNormValue=SignalTransform.BedFeatureToValues(SignalTransform.normalizeSignal(target_signal_filtered));
		  	
			ArrayList<FeatureSignal> IsThereFeatures=new ArrayList<FeatureSignal>(SignalPool.size()-1);
			ArrayList<FeatureSignal> ValThereFeatures=new ArrayList<FeatureSignal>(SignalPool.size()-1);
			for (TrackRecord feature_signal : SignalPool) {
				
			        if (feature_signal.ExperimentId!=(target_signal.ExperimentId))
			        {
			        	
			        	List<List<Float>> feature_BinSignal=SignalTransform.OverlapBinSignal(feature_signal, target_signal_filtered,21);
			        	List<List<Float>> feature_BinSignal_bg=SignalTransform.OverlapBinSignal(feature_signal, target_signal_bg,21);
			        	/***************isthere task****************/
			        float maxScore=Float.MIN_VALUE;
			        int bestBin=-1;
			        	for (int i = 0; i < feature_BinSignal.size(); i++) {
			        		List<Float> featureValue=feature_BinSignal.get(i);
			        		featureValue.addAll(feature_BinSignal_bg.get(i));
			        		float score=SignalComparator.getDiscriminativeCapbaility(featureValue, targetValue);
			        		if(score>maxScore)
			        		{
			        			bestBin=i;
			        			maxScore=score;
			        		}
						}
			        	//bestBin idea, consider strand
			        	List<Float> featureBestBinValue=feature_BinSignal.get(bestBin);
			        	featureBestBinValue.addAll(feature_BinSignal_bg.get(bestBin));
			        	IsThereFeatures.add(new FeatureSignal(featureBestBinValue, feature_signal.ExperimentId, maxScore,bestBin));
			        	
			        	/***************valthere task****************/
			        	maxScore=Float.MIN_VALUE;
				    bestBin=-1;
			        	for (int i = 0; i < feature_BinSignal.size(); i++) {
			        		List<Float> featureValue=feature_BinSignal.get(i);
			        		float score=SignalComparator.getCorrelation(featureValue, targetValue);
			        		if(score>maxScore)
			        		{
			        			bestBin=i;
			        			maxScore=score;
			        		}
						}
			        	//bestBin idea, consider strand
			        	featureBestBinValue=feature_BinSignal.get(bestBin);
			        ValThereFeatures.add(new FeatureSignal(featureBestBinValue, feature_signal.ExperimentId, maxScore,bestBin));
			        }
			    }
			 //wrap up to classification job , and put it into queue
			 Collections.sort(ValThereFeatures);
			 Collections.sort(IsThereFeatures);
			 int TopN=20; //selected best TopN features
			 ClassificationJob ValThereJob=new ClassificationJob((ArrayList<FeatureSignal>) ValThereFeatures.subList(0,  Math.min(TopN,ValThereFeatures.size())), target_signal.ExperimentId+"_ValThere", targetNormValue) ;
			 ValThereJob.Regression=true;
			 ClassificationJob IsThereJob=new ClassificationJob((ArrayList<FeatureSignal>) IsThereFeatures.subList(0, Math.min(TopN,IsThereFeatures.size())), target_signal.ExperimentId+"_IsThere", targetValue) ;
		     //put job to the execution queue
			 try {
				executor.execute(IsThereJob);
				executor.execute(ValThereJob);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
			 
		}
		
	}

}
