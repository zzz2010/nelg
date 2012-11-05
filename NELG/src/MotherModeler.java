import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.SimpleLayout;
import org.broad.tribble.bed.BEDFeature;
import org.jppf.client.JPPFClient;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

//this class mainly do initial feature selection and job assignment
public class MotherModeler {
	   // setup the logging system, used by some codecs
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MotherModeler.class);
    
    PooledExecutor executor;
	List<TrackRecord> SignalPool;
	int threadNum=4;
	public MotherModeler(List<TrackRecord> signalPool) {
		super();
		SignalPool = signalPool;

		 
	}
	
	public void Run()
	{	
		if(common.NFSmode)
		{
			executor = new PooledExecutor(new LinkedQueue(),common.threadNum);
			executor.setMinimumPoolSize(common.threadNum/2);
		}
		else
		  executor = new PooledExecutor(2);
//		
//		
		
		executor.setKeepAliveTime(1000 * 60*500 );
		executor.waitWhenBlocked();
		 
		ClassificationResultListener resultListener=new ClassificationResultListener();
		FeatureSelectionJob.resultsListener=resultListener;
		 JPPFClient jppfCLient = new JPPFClient();
		//take out one as class label, the rest as feature data
		for (TrackRecord target_signal : SignalPool) {
			System.out.println(target_signal.FilePrefix);

			if(target_signal.ExperimentId.contains("Control")||target_signal.ExperimentId.contains("Input"))
				continue;
			if(common.predictTarget_debug!=""&&!target_signal.FilePrefix.contains(common.predictTarget_debug))
				continue;
					
			FeatureSelectionJob FSJob=new FeatureSelectionJob(target_signal, SignalPool,jppfCLient);
			FeatureSelectionJob FSJob2=StateRecovery.CheckFeatureSelectionJob(target_signal);
			try {
				if(FSJob2==null)
				{
				executor.execute(FSJob);
//				FSJob.run();
				}
				else
				{
					logger.info("loading fsj: "+target_signal.FilePrefix);
					executor.execute(FSJob2);
//					FSJob2.run();
				}
			} catch (Exception e) {
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
		jppfCLient.close();
	}
	
	

}
