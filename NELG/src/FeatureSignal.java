import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;


public class FeatureSignal implements Comparable,Serializable  {

	public DoubleMatrix1D featureValue;
	public String FeatureId;
	public float featureSelectScore;
	public int binId=-1;
	public FeatureSignal(DoubleMatrix1D featureValue, String featureId,
			float featureSelectScore) {
		super();
		this.featureValue = featureValue;
		FeatureId = featureId;
		this.featureSelectScore = featureSelectScore;
		
	}
	public FeatureSignal(DoubleMatrix1D featureBestBinValue, String featureId,
			float featureSelectScore, int binId) {
		super();
		this.featureValue = featureBestBinValue;
		FeatureId = featureId;	
		this.featureSelectScore = featureSelectScore;
		this.binId = binId;
	}
	
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		return (int) -(this.featureSelectScore - ((FeatureSignal) arg0).featureSelectScore); //decrease order
	}
	
	public String toString() {
		// TODO Auto-generated method stub
		return FeatureId+" at bin "+binId+", score:"+featureSelectScore;
	}
	
	
	
}
