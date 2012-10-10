import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.broad.tribble.bed.BEDFeature;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

//this class mainly do initial feature selection and job assignment
public class MotherModeler {
	   // setup the logging system, used by some codecs
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
    
    
	List<TrackRecord> SignalPool;
	int threadNum=10;
	public MotherModeler(List<TrackRecord> signalPool) {
		super();
		SignalPool = signalPool;
		  logger.setLevel(Level.DEBUG);
		  ConsoleAppender appender =new ConsoleAppender(new PatternLayout());
		  logger.addAppender(appender); 
	}
	
	public void Run()
	{	
		PooledExecutor executor = new PooledExecutor(new LinkedQueue());
		executor.setMinimumPoolSize(threadNum);
		executor.setKeepAliveTime(1000 * 60*5 );
		
		
		//take out one as class label, the rest as feature data
		for (TrackRecord target_signal : SignalPool) {
			
			//get filtered target signal
		  	List<BEDFeature>target_signal_filtered= SignalTransform.fixRegionSize(SignalTransform.extractPositveSignal(target_signal),4000);
		  	List<BEDFeature>target_signal_bg = SignalTransform.extractNegativeSignal(target_signal_filtered,2*target_signal_filtered.size());
		  	DoubleMatrix1D targetValue=SignalTransform.BedFeatureToValues(target_signal_filtered);
		  	targetValue=DoubleFactory1D.sparse.append(targetValue, SignalTransform.BedFeatureToValues(target_signal_bg));
		  
		  	DoubleMatrix1D targetNormValue=SignalTransform.BedFeatureToValues(SignalTransform.normalizeSignal(target_signal_filtered));
		  	
			ArrayList<FeatureSignal> IsThereFeatures=new ArrayList<FeatureSignal>(SignalPool.size()-1);
			ArrayList<FeatureSignal> ValThereFeatures=new ArrayList<FeatureSignal>(SignalPool.size()-1);
		 
			for (TrackRecord feature_signal : SignalPool) {
				
			        if (feature_signal.ExperimentId!=(target_signal.ExperimentId))
			        {
			        	logger.debug(feature_signal.ExperimentId+" vs "+target_signal.ExperimentId+" :");
			        	SparseDoubleMatrix2D feature_BinSignal=SignalTransform.OverlapBinSignal(feature_signal, target_signal_filtered,20);
			        	SparseDoubleMatrix2D feature_BinSignal_bg=SignalTransform.OverlapBinSignal(feature_signal, target_signal_bg,20);
			        	/***************isthere task****************/
			        float maxScore=-1;
			        int bestBin=-1;
			        	for (int i = 0; i < feature_BinSignal.columns(); i++) {
			        		SparseDoubleMatrix1D featureValue=(SparseDoubleMatrix1D) DoubleFactory1D.sparse.append(feature_BinSignal.viewColumn(i), feature_BinSignal_bg.viewColumn(i)) ;
			        		float score=SignalComparator.getDiscriminativeCapbaility(featureValue, targetValue);
			        		if(score>maxScore)
			        		{
			        			bestBin=i;
			        			maxScore=score;
			        		}
						}
			        	//bestBin idea, consider strand
			        	SparseDoubleMatrix1D featureBestBinValue=(SparseDoubleMatrix1D) DoubleFactory1D.sparse.append(feature_BinSignal.viewColumn(bestBin), feature_BinSignal_bg.viewColumn(bestBin)) ;
			        FeatureSignal isF=new FeatureSignal(featureBestBinValue, feature_signal.ExperimentId, maxScore,bestBin);
			      	if(maxScore>0.6)
			      	{
			      		IsThereFeatures.add(isF);
			      	}
			        	logger.debug("isthere: "+isF);
			        	/***************valthere task****************/
			        	maxScore=-1;
				    bestBin=-1;
			        	for (int i = 0; i < feature_BinSignal.columns(); i++) {
			        		SparseDoubleMatrix1D featureValue=(SparseDoubleMatrix1D) feature_BinSignal.viewColumn(i);
			        		float score=SignalComparator.getCorrelation(featureValue, targetNormValue);
			        		if(score>maxScore)
			        		{
			        			bestBin=i;
			        			maxScore=score;
			        		}
						}
			        	//bestBin idea, consider strand
			        	featureBestBinValue=(SparseDoubleMatrix1D) feature_BinSignal.viewColumn(bestBin);

			        	 FeatureSignal valF= 	new FeatureSignal(featureBestBinValue, feature_signal.ExperimentId, maxScore,bestBin);
				        	if(maxScore>0.2)
				        	{
				        		ValThereFeatures.add(valF);
				        	}
			    	logger.debug("valthere: "+valF);
			       
			        }
			    }
			 //wrap up to classification job , and put it into queue
			int TopN=20; //selected best TopN features
        //put job to the execution queue
			 try {
				 if(IsThereFeatures.size()>0)
				 {
				 Collections.sort(IsThereFeatures);
				 ClassificationJob IsThereJob=new ClassificationJob(new ArrayList<FeatureSignal>( IsThereFeatures.subList(0, Math.min(TopN,IsThereFeatures.size()))), target_signal.ExperimentId+"_IsThere", targetValue) ;
				executor.execute(IsThereJob);
				 }
				 if(ValThereFeatures.size()>0)
				 {				 
				 Collections.sort(ValThereFeatures);
				 ClassificationJob ValThereJob=new ClassificationJob(new ArrayList<FeatureSignal>( ValThereFeatures.subList(0,  Math.min(TopN,ValThereFeatures.size()))), target_signal.ExperimentId+"_ValThere", targetNormValue) ;
				 ValThereJob.Regression=true;		
				 executor.execute(ValThereJob);
				 }
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
			 
		}
		
		try {
			executor.shutdownAfterProcessingCurrentlyQueuedTasks();
			executor.awaitTerminationAfterShutdown();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
