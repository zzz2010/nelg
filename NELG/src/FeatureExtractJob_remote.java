import java.util.ArrayList;

import org.jppf.server.protocol.JPPFTask;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class FeatureExtractJob_remote extends JPPFTask {
	SparseDoubleMatrix2D feature_BinSignal;
	SparseDoubleMatrix2D feature_BinSignal_bg;
	DoubleMatrix1D targetValue;
	DoubleMatrix1D targetNormValue;
	String feature_signal_ExperimentId;
	
	
	
	public FeatureExtractJob_remote(SparseDoubleMatrix2D feature_BinSignal,
			SparseDoubleMatrix2D feature_BinSignal_bg,
			DoubleMatrix1D targetValue, DoubleMatrix1D targetNormValue,
			String feature_signal_ExperimentId) {
		super();
		this.feature_BinSignal = feature_BinSignal;
		this.feature_BinSignal_bg = feature_BinSignal_bg;
		this.targetValue = targetValue;
		this.targetNormValue = targetNormValue;
		this.feature_signal_ExperimentId = feature_signal_ExperimentId;
	}
	ArrayList<FeatureSignal> IsThereFeatures;
	ArrayList<FeatureSignal> ValThereFeatures;
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FeatureExtractJob_remote.class);
	
	public void run() {
		// TODO Auto-generated method stub
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
        			
			        if(score>common.AUC_cutoff)
			      	{
			        	SparseDoubleMatrix1D featureBestBinValue=(SparseDoubleMatrix1D) DoubleFactory1D.sparse.append(feature_BinSignal.viewColumn(i), feature_BinSignal_bg.viewColumn(i)) ;
				        FeatureSignal isF=new FeatureSignal(featureBestBinValue, feature_signal_ExperimentId, score,i);	       
			      		IsThereFeatures.add(isF);
			      		logger.debug("isthere: "+isF);
			      	}
			        	
        		}
			}
        	//bestBin idea, consider strand
        	if(common.onlyBestBin)
        	{
		        	SparseDoubleMatrix1D featureBestBinValue=(SparseDoubleMatrix1D) DoubleFactory1D.sparse.append(feature_BinSignal.viewColumn(bestBin), feature_BinSignal_bg.viewColumn(bestBin)) ;
		        FeatureSignal isF=new FeatureSignal(featureBestBinValue, feature_signal_ExperimentId, maxScore,bestBin);
		        
		        if(maxScore>common.AUC_cutoff)
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
        				
			        if(score>common.Corr_cutoff)
			      	{
			        	SparseDoubleMatrix1D featureBestBinValue = (SparseDoubleMatrix1D) feature_BinSignal.viewColumn(i);
			        	
			        	 FeatureSignal valF= 	new FeatureSignal(featureBestBinValue, feature_signal_ExperimentId, score,i);
				       
				        ValThereFeatures.add(valF);
				        logger.debug("valthere: "+valF);
			      	}
			        	
        		}
			}
        	//bestBin idea, consider strand
        	if(common.onlyBestBin)
        	{
	        	SparseDoubleMatrix1D featureBestBinValue = (SparseDoubleMatrix1D) feature_BinSignal.viewColumn(bestBin);

	        	 FeatureSignal valF= 	new FeatureSignal(featureBestBinValue, feature_signal_ExperimentId, maxScore,bestBin);	
	        	 if(maxScore>common.Corr_cutoff)
		        	{
		        		ValThereFeatures.add(valF);
		        	}
	    	logger.debug("valthere: "+valF);
        	}
        	
        	this.feature_BinSignal = null;
    		this.feature_BinSignal_bg = null;
    		this.targetValue = null;
    		this.targetNormValue = null;
    		setResult(this);
	}

}
