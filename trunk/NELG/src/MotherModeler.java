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
    
    
	List<TrackRecord> SignalPool;
	int threadNum=10;
	public MotherModeler(List<TrackRecord> signalPool) {
		super();
		SignalPool = signalPool;
		  logger.setLevel(Level.DEBUG);
		  
//		  ConsoleAppender appender =new ConsoleAppender(new PatternLayout());
		  FileAppender appender;
		try {
			appender = new FileAppender(new SimpleLayout(), "log.txt");
			 logger.addAppender(appender); 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
	}
	
	public void Run()
	{	
		PooledExecutor executor = new PooledExecutor(new LinkedQueue());
		executor.setMinimumPoolSize(threadNum);
		executor.setKeepAliveTime(1000 * 60*50 );
		
		
		//take out one as class label, the rest as feature data
		for (TrackRecord target_signal : SignalPool) {

			if(!target_signal.FilePrefix.contains("RikenCage"))
				continue;
			FeatureSelectionJob FSJob=new FeatureSelectionJob(target_signal, SignalPool,executor);
			 try {
				executor.execute(FSJob);
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
