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


import org.broad.tribble.annotation.Strand;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.JPPFResultCollector;
import org.jppf.client.concurrent.JPPFExecutorService;
import org.jppf.client.event.TaskResultListener;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.ExecutionPolicy;
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
	public  static TaskResultListener resultsListener ;
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FeatureSelectionJob.class);
//	FeatureExtractor featureExtractor=new EqualBinFeatureExtractor(20);
	public  static FeatureExtractor featureExtractor=new MultiScaleFeatureExtractor(8);
	TrackRecord target_signal;
	List<TrackRecord> SignalPool;
	List<SimpleBEDFeature>target_signal_filtered;
	JPPFClient executor ;
	ArrayList<FeatureSignal> IsThereFeatures;
	ArrayList<FeatureSignal> ValThereFeatures;
	List<SimpleBEDFeature>target_signal_bg=null;
	
	public FeatureSelectionJob(TrackRecord target_signal,
			List<TrackRecord> signalPool,JPPFClient Executor ) {
		super();
		executor=Executor;
		this.target_signal = target_signal;
		SignalPool = signalPool;
	}
	
	
	public FeatureSelectionJob(TrackRecord target_signal,TrackRecord target_bg_track,
			List<TrackRecord> signalPool,JPPFClient Executor ) {
		super();
		executor=Executor;
		this.target_signal = target_signal;
		SignalPool = signalPool;
		if(common.SignalRange==0)
			target_signal_bg=SignalTransform.extractPositveSignal(target_bg_track);
		else
		target_signal_bg=SignalTransform.fixRegionSize(SignalTransform.extractPositveSignal(target_bg_track),common.SignalRange,true);
		//make negative score
		for (int i = 0; i < target_signal_bg.size(); i++) {
			target_signal_bg.get(i).setScore(0-target_signal_bg.get(i).getScore());
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		FileInputStream fileIn;
		 ObjectInputStream in ;
		
		 //initialize JPPF
		 JPPFJob job = new JPPFJob(common.getDataProvider());
		 job.setName(target_signal.FilePrefix);
		 
		if(target_signal_filtered!=null)//
		{
			if(target_signal_filtered.size()<50){
				System.err.println("Peak number should be at least 50");
				System.exit(1);
			}
			if(target_signal_bg==null)
				target_signal_bg = SignalTransform.extractNegativeSignal(target_signal_filtered,2*target_signal_filtered.size());
		  	
			DoubleMatrix1D targetValue=SignalTransform.BedFeatureToValues(target_signal_filtered);
			boolean[] strand=null;
		  	targetValue=DoubleFactory1D.sparse.append(targetValue, SignalTransform.BedFeatureToValues(target_signal_bg));
		  	if (target_signal_filtered.get(0).strand!=Strand.NONE){
			  	strand=new boolean[target_signal_filtered.size()];
			  	for (int i=0;i<strand.length;i++){
			  		strand[i]=(target_signal_filtered.get(i).strand==Strand.POSITIVE);
			  	}
		  	}
		  
		  	DoubleMatrix1D targetNormValue=SignalTransform.BedFeatureToValues(SignalTransform.normalizeSignal(target_signal_filtered));
		  	
		  	int TopN=common.topNfeatures;
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
						ClassificationJob IsThereJob=new ClassificationJob(new ArrayList<FeatureSignal>( IsThereFeatures.subList(0, Math.min(TopN,IsThereFeatures.size()))), target_signal.FilePrefix+"_IsThere", targetValue, strand) ;

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
				ClassificationJob ValThereJob=new ClassificationJob(new ArrayList<FeatureSignal>( ValThereFeatures.subList(0,  Math.min(TopN,ValThereFeatures.size()))), target_signal.FilePrefix+"_ValThere", targetNormValue, strand) ;
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
		String storekey=target_signal.FilePrefix+"/"+common.SignalRange;
	  	target_signal_filtered= StateRecovery.loadCache_BEDFeatureList(storekey);
	  	if(target_signal_filtered==null)
	  	{
	  		if(common.SignalRange==0)
	  			target_signal_filtered=SignalTransform.extractPositveSignal(target_signal);
	  		else
	  			target_signal_filtered=SignalTransform.fixRegionSize(SignalTransform.extractPositveSignal(target_signal),common.SignalRange,true);
	  		StateRecovery.saveCache_BEDFeatureList(target_signal_filtered, storekey);
	  	}
	  		if(target_signal_filtered.size()<common.MinimumPeakNum)
	  	{
	  		toFile();
	  		return;
	  	}
	  	
	  	if(target_signal_bg==null)
	  	      target_signal_bg = SignalTransform.extractNegativeSignal(target_signal_filtered,2*target_signal_filtered.size());
	  	
	  	
	  	StateRecovery.saveCache_BEDFeatureList(target_signal_bg, storekey+"bg.bed");
	  	DoubleMatrix1D targetValue=SignalTransform.BedFeatureToValues(target_signal_filtered);
	  	boolean[] strand=null;
	  	targetValue=DoubleFactory1D.sparse.append(targetValue, SignalTransform.BedFeatureToValues(target_signal_bg));
	  	if (target_signal_filtered.get(0).strand!=Strand.NONE){
		  	strand=new boolean[target_signal_filtered.size()];
		  	for (int i=0;i<strand.length;i++){
		  		strand[i]=(target_signal_filtered.get(i).strand==Strand.POSITIVE);
		  	}
	  	}
	  
	  	DoubleMatrix1D targetNormValue=SignalTransform.BedFeatureToValues(SignalTransform.normalizeSignal(target_signal_filtered));
	  	
		IsThereFeatures=new ArrayList<FeatureSignal>(SignalPool.size()-1);
		ValThereFeatures=new ArrayList<FeatureSignal>(SignalPool.size()-1);

		
		boolean onlyBestBin=false;

		logger.debug("number of peaks of "+target_signal.ExperimentId+" :"+target_signal_filtered.size());
		 JPPFJob localjob = new JPPFJob(common.getDataProvider());
		 localjob.setName("local_"+target_signal.FilePrefix);
		 
		 JPPFClient Jclient=null;
		 PooledExecutor localclient=null;
		 if(!common.Localmode)
		 {
			 Jclient=new JPPFClient("JPPF executor");
			 if (executor==null)
				 executor=Jclient;
		 }
		 else
		 {
			 localclient=new PooledExecutor(new LinkedQueue());
		 localclient.setMinimumPoolSize(common.threadNum);		
		 localclient.setKeepAliveTime(1000 * 60*500 );
		 }
			
		for (TrackRecord feature_signal : SignalPool) {
			if(common.selectFeature_debug!=""&&!feature_signal.FilePrefix.contains(common.selectFeature_debug))
			{
				logger.debug("filter: "+feature_signal.FilePrefix);
				continue;
			}
			
		        if (!SynonymCheck.isSynonym(feature_signal, target_signal) )
		        {
		        	logger.debug("add Tasks:"+target_signal.FilePrefix+"_"+feature_signal.FilePrefix);
		        	FeatureExtractJob FEJob=new FeatureExtractJob(target_signal_filtered, target_signal_bg, feature_signal, target_signal, featureExtractor, targetValue, targetNormValue);
		        	try {
		        		FEJob.setTimeoutSchedule(new JPPFSchedule(1000*60*60));
						localjob.addTask(FEJob);
						if(common.Localmode)
							localclient.execute(FEJob);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
		        }
		        else
		        {
		        	logger.debug("Synonym filter:"+target_signal.FilePrefix+"_"+feature_signal.FilePrefix);
		        }
		    }
		try {
			logger.debug("Number of Feature Extraction Tasks:"+localjob.getTasks().size());
			 localjob.setBlocking(true);
			 localjob.getSLA().setMaxNodes(1000);
			 localjob.getSLA().setPriority(1);
			 localjob.getSLA().setCancelUponClientDisconnect(false);
			 localjob.getSLA().setJobExpirationSchedule(new JPPFSchedule(1000*60*60));
			 List<JPPFTask> jobresult =null;
			if(!common.Localmode)
			{
//				while (!executor.hasAvailableConnection()) Thread.sleep(1L);
				jobresult=Jclient.submit(localjob);
			}
			else
			{
				try {
					localclient.shutdownAfterProcessingCurrentlyQueuedTasks();
					localclient.awaitTerminationAfterShutdown();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
				jobresult =localjob.getTasks();
			}
			
			
			
			for (int i = 0; i < jobresult.size(); i++) {
				FeatureExtractJob	result1=(FeatureExtractJob)jobresult.get(i);
				 if (result1.getException() != null) {
				       // process the exception here ...
					// logger.debug(result1.feature_signal.ExperimentId+" got exception.");
					 logger.debug("FEjob Exception: "+result1.getException().getMessage());
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
		int TopN=common.topNfeatures; //selected best TopN features
   //put job to the execution queue
		 try {
			 if(IsThereFeatures.size()>0)
			 {
				 ClassificationResult IsThereJob2=StateRecovery.CheckClassificationJob(target_signal.FilePrefix+"_IsThere");
					if(IsThereJob2==null)
					{
						Collections.sort(IsThereFeatures);
						ClassificationJob IsThereJob=new ClassificationJob(new ArrayList<FeatureSignal>( IsThereFeatures.subList(0, Math.min(TopN,IsThereFeatures.size()))), target_signal.FilePrefix+"_IsThere", targetValue, strand) ;
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
						ClassificationJob ValThereJob=new ClassificationJob(new ArrayList<FeatureSignal>( ValThereFeatures.subList(0,  Math.min(TopN,ValThereFeatures.size()))), target_signal.FilePrefix+"_ValThere", targetNormValue, strand) ;
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
		if(job.getTasks().size()>0)
		   try {
			   if(common.Localmode)
			   {
                  for (JPPFTask iterable_element : job.getTasks()) {
                	  iterable_element.run();
                	ClassificationResult result=(ClassificationResult) iterable_element.getResult();
                	 result.toFile();
				} 
			   }
			   else
			   {
				   if(!common.Localmode||resultsListener==null)
				   {
					   resultsListener=new JPPFResultCollector(job);		  
				   }
				   if(!common.NFSmode) //NFSmode using node to write, no need to send back
				   {
				     job.setResultListener(resultsListener);
				   }
				// set the job as non-blocking
				   job.getSLA().setCancelUponClientDisconnect(false);
				   job.setBlocking(false);
//				while (!executor.hasAvailableConnection()) Thread.sleep(1L);
				   executor.submit(job);
				   List<JPPFTask> resultLst=((JPPFResultCollector)resultsListener).waitForResults();
				   if (!common.NFSmode){
					   for (JPPFTask iterable_element : resultLst) {
						   ClassificationResult result=(ClassificationResult) iterable_element.getResult();
						   result.toFile();
						} 
				   }
			   }
			
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
