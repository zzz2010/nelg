import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.broad.tribble.bed.BEDFeature;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.JPPFResultCollector;
import org.jppf.client.event.TaskResultListener;
import org.jppf.server.protocol.JPPFTask;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class FeatureSelectionJob implements  Runnable {

	/**
	 * 
	 */
	public static TaskResultListener resultsListener ;
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FeatureSelectionJob.class);
	TrackRecord target_signal;
	List<TrackRecord> SignalPool;
	List<BEDFeature>target_signal_filtered;
	JPPFClient executor ;
	ArrayList<FeatureSignal> IsThereFeatures;
	ArrayList<FeatureSignal> ValThereFeatures;
	HashMap<String, Float> FeatureAUC;
	HashMap<String, Float> FeatureCorr;
	
	public FeatureSelectionJob(TrackRecord target_signal,
			List<TrackRecord> signalPool,JPPFClient Executor ) {
		super();
		executor=Executor;
		this.target_signal = target_signal;
		SignalPool = signalPool;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		FileInputStream fileIn;
		 ObjectInputStream in ;
		 
		 //initialize JPPF
		 JPPFJob job = new JPPFJob();
		 job.setName(target_signal.FilePrefix);
		 
		if(target_signal_filtered!=null)//
		{
			if(target_signal_filtered.size()<50)
				return;
			List<BEDFeature>target_signal_bg = SignalTransform.extractNegativeSignal(target_signal_filtered,2*target_signal_filtered.size());
		  	DoubleMatrix1D targetValue=SignalTransform.BedFeatureToValues(target_signal_filtered);
		  	targetValue=DoubleFactory1D.sparse.append(targetValue, SignalTransform.BedFeatureToValues(target_signal_bg));
		  
		  	DoubleMatrix1D targetNormValue=SignalTransform.BedFeatureToValues(SignalTransform.normalizeSignal(target_signal_filtered));
		  	
		  	int TopN=100;
			if(IsThereFeatures.size()>0)
			{
				 ClassificationResult IsThereJob2=StateRecovery.CheckClassificationJob(target_signal.FilePrefix+"_IsThere");
				 if(IsThereJob2!=null)
				 {
					 logger.info("skip classification:"+target_signal.FilePrefix+"_IsThere");
				 }
				 else
				 {			
						Collections.sort(IsThereFeatures);
						ClassificationJob IsThereJob=new ClassificationJob(new ArrayList<FeatureSignal>( IsThereFeatures.subList(0, Math.min(TopN,IsThereFeatures.size()))), target_signal.FilePrefix+"_IsThere", targetValue) ;

						try {
							IsThereJob.toFile();
							job.addTask(IsThereJob);
						} catch (JPPFException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					 
				 }
			}
			if(ValThereFeatures.size()>0)
			{
				 ClassificationResult ValThereJob2=StateRecovery.CheckClassificationJob(target_signal.FilePrefix+"_ValThere");
				 if(ValThereJob2!=null)
				 {
					 logger.info("skip regression:"+target_signal.FilePrefix+"_ValThere");
				 }
				 else
				 {
				Collections.sort(ValThereFeatures);
				ClassificationJob ValThereJob=new ClassificationJob(new ArrayList<FeatureSignal>( ValThereFeatures.subList(0,  Math.min(TopN,ValThereFeatures.size()))), target_signal.FilePrefix+"_ValThere", targetNormValue) ;
				ValThereJob.Regression=true;		
				//executor.execute(ValThereJob);
//				ValThereJob.run();
				
				try {
					ValThereJob.toFile();
					job.addTask(ValThereJob);
				} catch (JPPFException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				 }
			}

		}
		else	
		{
		//get filtered target signal
		String storekey=target_signal.FilePrefix+common.SignalRange;
	  	target_signal_filtered= StateRecovery.loadCache_BEDFeatureList(storekey);
	  	if(target_signal_filtered==null)
	  	{
	  		target_signal_filtered=SignalTransform.fixRegionSize(SignalTransform.extractPositveSignal(target_signal),common.SignalRange,true);
	  		StateRecovery.saveCache_BEDFeatureList(target_signal_filtered, storekey);
	  	}
	  		if(target_signal_filtered.size()<common.MinimumPeakNum)
	  	{
	  		toFile();
	  		return;
	  	}
	  	
	  	List<BEDFeature>target_signal_bg = SignalTransform.extractNegativeSignal(target_signal_filtered,2*target_signal_filtered.size());
	  	DoubleMatrix1D targetValue=SignalTransform.BedFeatureToValues(target_signal_filtered);
	  	targetValue=DoubleFactory1D.sparse.append(targetValue, SignalTransform.BedFeatureToValues(target_signal_bg));
	  
	  	DoubleMatrix1D targetNormValue=SignalTransform.BedFeatureToValues(SignalTransform.normalizeSignal(target_signal_filtered));
	  	
		IsThereFeatures=new ArrayList<FeatureSignal>(SignalPool.size()-1);
		ValThereFeatures=new ArrayList<FeatureSignal>(SignalPool.size()-1);
		FeatureAUC=new HashMap<String, Float>(SignalPool.size()-1);
		FeatureCorr=new HashMap<String, Float>(SignalPool.size()-1);
		
		boolean onlyBestBin=false;
		//FeatureExtractor featureExtractor=new EqualBinFeatureExtractor(20);
		FeatureExtractor featureExtractor=new MultiScaleFeatureExtractor(8);
		logger.debug("number of peaks of "+target_signal.ExperimentId+" :"+target_signal_filtered.size());
		for (TrackRecord feature_signal : SignalPool) {
//			if(!feature_signal.FilePrefix.contains("H3k36"))
//				continue;
			
		        if (!SynonymCheck.isSynonym(feature_signal, target_signal) )
		        {
		        	logger.debug(feature_signal.ExperimentId+" vs "+target_signal.ExperimentId+" :");
		        	String storekey1=target_signal.FilePrefix+feature_signal.FilePrefix+featureExtractor.getClass().getName();
		        	String storekey2=storekey1+"_bg";
		        	SparseDoubleMatrix2D feature_BinSignal=StateRecovery.loadCache_SparseDoubleMatrix2D(storekey1);
		        	if(feature_BinSignal==null)
		        	{
		        		feature_BinSignal=featureExtractor.extractSignalFeature(feature_signal, target_signal_filtered);
		        		StateRecovery.saveCache_SparseDoubleMatrix2D(feature_BinSignal, storekey1);		     
		        	}
		        	
		        	SparseDoubleMatrix2D feature_BinSignal_bg=StateRecovery.loadCache_SparseDoubleMatrix2D(storekey2);
		        	if(feature_BinSignal_bg==null)
		        	{
		        		feature_BinSignal_bg=featureExtractor.extractSignalFeature(feature_signal, target_signal_bg);
		        		StateRecovery.saveCache_SparseDoubleMatrix2D(feature_BinSignal_bg, storekey2);
		        	}
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
		        		if(!onlyBestBin)
		        		{	
		        			 FeatureAUC.put(feature_signal.FilePrefix, maxScore);
					        if(score>0.6)
					      	{
					        	SparseDoubleMatrix1D featureBestBinValue=(SparseDoubleMatrix1D) DoubleFactory1D.sparse.append(feature_BinSignal.viewColumn(i), feature_BinSignal_bg.viewColumn(i)) ;
						        FeatureSignal isF=new FeatureSignal(featureBestBinValue, feature_signal.ExperimentId, score,i);	       
					      		IsThereFeatures.add(isF);
					      		logger.debug("isthere: "+isF);
					      	}
					        	
		        		}
					}
		        	//bestBin idea, consider strand
		        	if(onlyBestBin)
		        	{
				        	SparseDoubleMatrix1D featureBestBinValue=(SparseDoubleMatrix1D) DoubleFactory1D.sparse.append(feature_BinSignal.viewColumn(bestBin), feature_BinSignal_bg.viewColumn(bestBin)) ;
				        FeatureSignal isF=new FeatureSignal(featureBestBinValue, feature_signal.ExperimentId, maxScore,bestBin);
				        FeatureAUC.put(feature_signal.FilePrefix, maxScore);
				        if(maxScore>0.6)
				      	{
				      		IsThereFeatures.add(isF);
				      	}
				        	logger.debug("isthere: "+isF);
		        	}
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
		        		if(!onlyBestBin)
		        		{	
		        			 FeatureCorr.put(feature_signal.FilePrefix, maxScore);	
					        if(score>0.2)
					      	{
					        	SparseDoubleMatrix1D featureBestBinValue = (SparseDoubleMatrix1D) feature_BinSignal.viewColumn(i);
					        	
					        	 FeatureSignal valF= 	new FeatureSignal(featureBestBinValue, feature_signal.ExperimentId, score,i);
						       
						        ValThereFeatures.add(valF);
						        logger.debug("valthere: "+valF);
					      	}
					        	
		        		}
					}
		        	//bestBin idea, consider strand
		        	if(onlyBestBin)
		        	{
			        	SparseDoubleMatrix1D featureBestBinValue = (SparseDoubleMatrix1D) feature_BinSignal.viewColumn(bestBin);
	
			        	 FeatureSignal valF= 	new FeatureSignal(featureBestBinValue, feature_signal.ExperimentId, maxScore,bestBin);
				        FeatureCorr.put(feature_signal.FilePrefix, maxScore);	
			        	 if(maxScore>0.2)
				        	{
				        		ValThereFeatures.add(valF);
				        	}
			    	logger.debug("valthere: "+valF);
		        	}
		       
		        }
		    }
		
		//save to file
		toFile();
		 //wrap up to classification job , and put it into queue
		int TopN=100; //selected best TopN features
   //put job to the execution queue
		 try {
			 if(IsThereFeatures.size()>0)
			 {
				 ClassificationResult IsThereJob2=StateRecovery.CheckClassificationJob(target_signal.FilePrefix+"_IsThere");
					if(IsThereJob2==null)
					{
						Collections.sort(IsThereFeatures);
						ClassificationJob IsThereJob=new ClassificationJob(new ArrayList<FeatureSignal>( IsThereFeatures.subList(0, Math.min(TopN,IsThereFeatures.size()))), target_signal.FilePrefix+"_IsThere", targetValue) ;
//						if(executor!=null)
//							executor.execute(IsThereJob);
//						else
//						IsThereJob.run();
						try {
							IsThereJob.toFile();
							job.addTask(IsThereJob);
						} catch (JPPFException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
					else
					{
						logger.info("skip cr:"+target_signal.FilePrefix+"_IsThere");
					}
			 }
			 if(ValThereFeatures.size()>0)
			 {				 

					ClassificationResult ValThereJob2=StateRecovery.CheckClassificationJob(target_signal.FilePrefix+"_ValThere");
					if(ValThereJob2==null)
					{
						Collections.sort(ValThereFeatures);
						ClassificationJob ValThereJob=new ClassificationJob(new ArrayList<FeatureSignal>( ValThereFeatures.subList(0,  Math.min(TopN,ValThereFeatures.size()))), target_signal.FilePrefix+"_ValThere", targetNormValue) ;
						ValThereJob.Regression=true;		
						//executor.execute(ValThereJob);
//						ValThereJob.run();
						try {
							ValThereJob.toFile();
							job.addTask(ValThereJob);
						} catch (JPPFException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					else
					{
						logger.info("skip cr:"+target_signal.FilePrefix+"_ValThere");
					}
			 }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

		//JPPFTASK
		

		// Submit the job and wait until the results are returned.
		   // The results are returned as a list of JPPFTask instances,
		   // in the same order as the one in which the tasks where initially added the job.
		   try {
			   if(resultsListener==null)
			    resultsListener=new JPPFResultCollector(job.getTasks().size());
			   
			   job.setResultListener(resultsListener);
			// set the job as non-blocking
			   job.setBlocking(false);
			 executor.submit(job);
	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	 public void toFile()
	 {
		 try {
        	 FileOutputStream fileOut =
    		         new FileOutputStream(common.tempDir+target_signal.FilePrefix+".fsj");
    		         ObjectOutputStream out =
    		                            new ObjectOutputStream(fileOut);
			out.writeObject(new FeatureSelectionSObj(IsThereFeatures, ValThereFeatures, FeatureAUC, FeatureCorr, target_signal_filtered));
	         out.close();
	          fileOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
}
