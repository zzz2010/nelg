import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.server.protocol.JPPFTask;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class FeatureExtractJob extends JPPFTask {

	List<SimpleBEDFeature>target_signal_filtered;
	List<SimpleBEDFeature> target_signal_bg;
	TrackRecord feature_signal;
	TrackRecord target_signal;
	FeatureExtractor featureExtractor;
	DoubleMatrix1D targetValue;
	DoubleMatrix1D targetNormValue;
	JPPFClient remoteClient;
	ArrayList<FeatureSignal> IsThereFeatures;
	ArrayList<FeatureSignal> ValThereFeatures;


	public FeatureExtractJob(List<SimpleBEDFeature> target_signal_filtered,
			List<SimpleBEDFeature> target_signal_bg, TrackRecord feature_signal,
			TrackRecord target_signal, FeatureExtractor featureExtractor,
			DoubleMatrix1D targetValue, DoubleMatrix1D targetNormValue) {
		super();
		this.target_signal_filtered = target_signal_filtered;
		this.target_signal_bg = target_signal_bg;
		this.feature_signal = feature_signal;
		this.target_signal = target_signal;
		this.featureExtractor = featureExtractor;
		this.targetValue = targetValue;
		this.targetNormValue = targetNormValue;
	}
	
	
	
	public FeatureExtractJob(
			TrackRecord feature_signal,TrackRecord target_signal) {
		super();
		this.target_signal_bg = target_signal_bg;
		this.feature_signal = feature_signal;
		String storekey=target_signal.FilePrefix+common.SignalRange;
	  	target_signal_filtered= StateRecovery.loadCache_BEDFeatureList(storekey);
	  	if(target_signal_filtered==null)
	  	{
	  		target_signal_filtered=SignalTransform.fixRegionSize(SignalTransform.extractPositveSignal(target_signal),common.SignalRange,true);
	  		StateRecovery.saveCache_BEDFeatureList(target_signal_filtered, storekey);
	  	}
		this.target_signal_bg = SignalTransform.extractNegativeSignal(target_signal_filtered,2*target_signal_filtered.size());
	  	 this.targetValue=SignalTransform.BedFeatureToValues(target_signal_filtered);
	  	targetValue=DoubleFactory1D.sparse.append(targetValue, SignalTransform.BedFeatureToValues(target_signal_bg));
	  
	  	 this.targetNormValue=SignalTransform.BedFeatureToValues(SignalTransform.normalizeSignal(target_signal_filtered));
	  	
	}



	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FeatureExtractJob.class);
	@Override
	public void run() {
		// TODO Auto-generated method stub
		logger.debug(feature_signal.ExperimentId+" vs "+target_signal.ExperimentId+" :");
		IsThereFeatures=new ArrayList<FeatureSignal>();
		ValThereFeatures=new ArrayList<FeatureSignal>();
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
	        	JPPFJob job=new JPPFJob();
	        	try {
					job.addTask(new FeatureExtractJob_remote(feature_BinSignal, feature_BinSignal_bg, targetValue, targetNormValue, feature_signal.ExperimentId));
					    List<JPPFTask> jobresult =remoteClient.submit(job);
						FeatureExtractJob_remote	result1=(FeatureExtractJob_remote)jobresult.get(0);
						IsThereFeatures=result1.IsThereFeatures;
						ValThereFeatures=result1.ValThereFeatures;
	        	} catch (JPPFException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	
//	        setResult(this);
	}

}
