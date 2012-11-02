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


import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.JPPFResultCollector;
import org.jppf.client.concurrent.JPPFExecutorService;
import org.jppf.client.event.TaskResultListener;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.server.protocol.JPPFTask;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
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
	List<SimpleBEDFeature>target_signal_filtered;
	JPPFClient executor ;
	ArrayList<FeatureSignal> IsThereFeatures;
	ArrayList<FeatureSignal> ValThereFeatures;

	
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
			List<SimpleBEDFeature>target_signal_bg = SignalTransform.extractNegativeSignal(target_signal_filtered,2*target_signal_filtered.size());
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
	  	
	  	List<SimpleBEDFeature>target_signal_bg = SignalTransform.extractNegativeSignal(target_signal_filtered,2*target_signal_filtered.size());
	  	
	  	
	  	StateRecovery.saveCache_BEDFeatureList(target_signal_bg, storekey+"bg.bed");
	  	DoubleMatrix1D targetValue=SignalTransform.BedFeatureToValues(target_signal_filtered);
	  	targetValue=DoubleFactory1D.sparse.append(targetValue, SignalTransform.BedFeatureToValues(target_signal_bg));
	  
	  	DoubleMatrix1D targetNormValue=SignalTransform.BedFeatureToValues(SignalTransform.normalizeSignal(target_signal_filtered));
	  	
		IsThereFeatures=new ArrayList<FeatureSignal>(SignalPool.size()-1);
		ValThereFeatures=new ArrayList<FeatureSignal>(SignalPool.size()-1);

		
		boolean onlyBestBin=false;
//		FeatureExtractor featureExtractor=new EqualBinFeatureExtractor(20);
		FeatureExtractor featureExtractor=new MultiScaleFeatureExtractor(8);
		logger.debug("number of peaks of "+target_signal.ExperimentId+" :"+target_signal_filtered.size());
		 JPPFJob localjob = new JPPFJob();
		 localjob.setName("local_"+target_signal.FilePrefix);
		
//		 JPPFClient localclient=new JPPFClient("local executor");
//		 localclient.setLocalExecutionEnabled(true);
		 PooledExecutor localclient=new PooledExecutor(new LinkedQueue());
		 localclient.setMinimumPoolSize(common.threadNum);		
		 localclient.setKeepAliveTime(1000 * 60*500 );
			
		for (TrackRecord feature_signal : SignalPool) {
			if(common.selectFeature_debug!=""&&!feature_signal.FilePrefix.contains(common.selectFeature_debug))
			{
				logger.debug("filter: "+feature_signal.FilePrefix);
				continue;
			}
			
		        if (!SynonymCheck.isSynonym(feature_signal, target_signal) )
		        {
		        	
		        	FeatureExtractJob FEJob=new FeatureExtractJob(target_signal_filtered, target_signal_bg, feature_signal, target_signal, featureExtractor, targetValue, targetNormValue);
		        	try {
						localjob.addTask(FEJob);
						
						localclient.execute(FEJob);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
		        }
		    }
		try {
			logger.debug("Number of Feature Extraction Tasks:"+localjob.getTasks().size());
			 localjob.setBlocking(true);
			 
//			 String dateFormat = "MM/dd/yyyy hh:mm a z";
//			 // set the job to expire on September 30, 2010 at 12:08 PM in the CEDT time zone
//			 JPPFSchedule schedule = new JPPFSchedule("09/30/2014 12:08 PM CEDT", dateFormat);
////
//			 localjob.getSLA().setJobExpirationSchedule(schedule);
			
				try {
					localclient.shutdownAfterProcessingCurrentlyQueuedTasks();
					localclient.awaitTerminationAfterShutdown();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		 
			 List<JPPFTask> jobresult =localjob.getTasks();// localclient.submit(localjob); //
			for (int i = 0; i < jobresult.size(); i++) {
				FeatureExtractJob	result1=(FeatureExtractJob)jobresult.get(i);
				 if (result1.getException() != null) {
				       // process the exception here ...
					 logger.debug(result1.feature_signal.ExperimentId+" got exception.");
				     } else {
				       // process the result here ...
							IsThereFeatures.addAll(result1.IsThereFeatures);
							ValThereFeatures.addAll(result1.ValThereFeatures);
				     }
			}
					
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
			out.writeObject(new FeatureSelectionSObj(IsThereFeatures, ValThereFeatures, target_signal_filtered));
	         out.close();
	          fileOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
}
