import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.broad.tribble.bed.BEDFeature;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class FeatureSelectionJob implements Serializable, Runnable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7511557855846548797L;
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FeatureSelectionJob.class);
	TrackRecord target_signal;
	List<TrackRecord> SignalPool;
	PooledExecutor executor ;
	ArrayList<FeatureSignal> IsThereFeatures;
	ArrayList<FeatureSignal> ValThereFeatures;
	HashMap<String, Float> FeatureAUC;
	HashMap<String, Float> FeatureCorr;
	
	public FeatureSelectionJob(TrackRecord target_signal,
			List<TrackRecord> signalPool,PooledExecutor Executor ) {
		super();
		executor=Executor;
		this.target_signal = target_signal;
		SignalPool = signalPool;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(SignalPool==null)
		{
			FileInputStream fileIn;
			 ObjectInputStream in ;
			try {
				ClassificationJob IsThereJob2=StateRecovery.CheckClassificationJob(target_signal.FilePrefix+"_IsThere");
				if(IsThereJob2==null)
				{
				fileIn = new FileInputStream(target_signal.FilePrefix+"_IsThere.cj");
				  in = new ObjectInputStream(fileIn);	 
				 ClassificationJob IsThereJob=(ClassificationJob)in.readObject();
				IsThereJob.run();
				}
				else
				{
					logger.info("skip cr:"+target_signal.FilePrefix+"_IsThere");
				}
				
				
				ClassificationJob ValThereJob2=StateRecovery.CheckClassificationJob(target_signal.FilePrefix+"_ValThere");
				if(ValThereJob2==null)
				{
				fileIn = new FileInputStream(target_signal.FilePrefix+"_ValThere.cj");
				  in = new ObjectInputStream(fileIn);
				ClassificationJob ValThereJob=(ClassificationJob)in.readObject();
				 ValThereJob.run();
				}
				else
				{
					logger.info("skip cr:"+target_signal.FilePrefix+"_ValThere");
				}
				 
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		else	
		{
		//get filtered target signal
	  	List<BEDFeature>target_signal_filtered= SignalTransform.fixRegionSize(SignalTransform.extractPositveSignal(target_signal),4000);
	  	if(target_signal_filtered.size()<50)
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
		
		
		logger.debug("number of peaks of "+target_signal.ExperimentId+" :"+target_signal_filtered.size());
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
		        FeatureAUC.put(feature_signal.FilePrefix, maxScore);
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
			        FeatureCorr.put(feature_signal.FilePrefix, maxScore);	
		        	 if(maxScore>0.2)
			        	{
			        		ValThereFeatures.add(valF);
			        	}
		    	logger.debug("valthere: "+valF);
		       
		        }
		    }
		
		//save to file
		toFile();
		 //wrap up to classification job , and put it into queue
		int TopN=20; //selected best TopN features
   //put job to the execution queue
		 try {
			 if(IsThereFeatures.size()>0)
			 {
				    ClassificationJob IsThereJob2=StateRecovery.CheckClassificationJob(target_signal.FilePrefix+"_IsThere");
					if(IsThereJob2==null)
					{
						Collections.sort(IsThereFeatures);
						ClassificationJob IsThereJob=new ClassificationJob(new ArrayList<FeatureSignal>( IsThereFeatures.subList(0, Math.min(TopN,IsThereFeatures.size()))), target_signal.FilePrefix+"_IsThere", targetValue) ;
						//executor.execute(IsThereJob);
						IsThereJob.run();
					}
					else
					{
						logger.info("skip cr:"+target_signal.FilePrefix+"_IsThere");
					}
			 }
			 if(ValThereFeatures.size()>0)
			 {				 

					ClassificationJob ValThereJob2=StateRecovery.CheckClassificationJob(target_signal.FilePrefix+"_ValThere");
					if(ValThereJob2==null)
					{
						Collections.sort(ValThereFeatures);
						ClassificationJob ValThereJob=new ClassificationJob(new ArrayList<FeatureSignal>( ValThereFeatures.subList(0,  Math.min(TopN,ValThereFeatures.size()))), target_signal.FilePrefix+"_ValThere", targetNormValue) ;
						ValThereJob.Regression=true;		
						//executor.execute(ValThereJob);
						ValThereJob.run();
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
	}
	 public void toFile()
	 {
		 try {
        	 FileOutputStream fileOut =
    		         new FileOutputStream(target_signal.FilePrefix+".fsj");
    		         ObjectOutputStream out =
    		                            new ObjectOutputStream(fileOut);
			out.writeObject(this);
	         out.close();
	          fileOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
}
