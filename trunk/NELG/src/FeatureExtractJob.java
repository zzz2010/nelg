import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.MemoryMapDataProvider;

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
		String storekey=target_signal.FilePrefix+"/"+common.SignalRange;
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
	
	public void run() {
		// TODO Auto-generated method stub
		MemoryMapDataProvider dataProvider=(MemoryMapDataProvider)getDataProvider();
		if(dataProvider!=null)
		common.loadDataProvider(dataProvider);
		
		logger.debug(feature_signal.ExperimentId+" vs "+target_signal.ExperimentId+" :");
		IsThereFeatures=new ArrayList<FeatureSignal>();
		ValThereFeatures=new ArrayList<FeatureSignal>();
	        	logger.debug(feature_signal.ExperimentId+" vs "+target_signal.ExperimentId+" :");
	        	String storekey1=target_signal.FilePrefix+"/"+feature_signal.FilePrefix+featureExtractor.getClass().getName();
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
	        		if(!common.onlyBestBin)
	        		{	
	        			
				        if(score>=common.AUC_cutoff)
				      	{
				        	SparseDoubleMatrix1D featureBestBinValue=(SparseDoubleMatrix1D) DoubleFactory1D.sparse.append(feature_BinSignal.viewColumn(i), feature_BinSignal_bg.viewColumn(i)) ;
					        FeatureSignal isF=new FeatureSignal(featureBestBinValue, feature_signal.ExperimentId, score,i);	       
				      		IsThereFeatures.add(isF);
				      		logger.debug("isthere: "+isF);
				      	}
				        	
	        		}
				}
	        	//bestBin idea, consider strand
	        	if(common.onlyBestBin)
	        	{
			        	SparseDoubleMatrix1D featureBestBinValue=(SparseDoubleMatrix1D) DoubleFactory1D.sparse.append(feature_BinSignal.viewColumn(bestBin), feature_BinSignal_bg.viewColumn(bestBin)) ;
			        FeatureSignal isF=new FeatureSignal(featureBestBinValue, feature_signal.ExperimentId, maxScore,bestBin);
			        
			        if(maxScore>=common.AUC_cutoff)
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
	        		if(!common.onlyBestBin)
	        		{	
	        			logger.debug("corr: "+score);
				        if(score>=common.Corr_cutoff)
				      	{
				        	SparseDoubleMatrix1D featureBestBinValue = (SparseDoubleMatrix1D) feature_BinSignal.viewColumn(i);
				        	
				        	 FeatureSignal valF= 	new FeatureSignal(featureBestBinValue, feature_signal.ExperimentId, score,i);
					       
					        ValThereFeatures.add(valF);
					        logger.debug("valthere: "+valF);
				      	}
				        	
	        		}
				}
	        	//bestBin idea, consider strand
	        	if(common.onlyBestBin)
	        	{
		        	SparseDoubleMatrix1D featureBestBinValue = (SparseDoubleMatrix1D) feature_BinSignal.viewColumn(bestBin);

		        	 FeatureSignal valF= 	new FeatureSignal(featureBestBinValue, feature_signal.ExperimentId, maxScore,bestBin);	
		        	 if(maxScore>=common.Corr_cutoff)
			        	{
			        		ValThereFeatures.add(valF);
			        	}
		    	logger.debug("valthere: "+valF);
	        	}
	        	
	 //clean up the input data     	
	        	target_signal_filtered=null;
	       target_signal_bg=null;
	      feature_signal=null;
	       target_signal=null;
	       featureExtractor=null;
	      targetValue=null;
	       targetNormValue=null;
	        	
	        setResult(this);
	}

}
