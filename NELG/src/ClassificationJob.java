import java.io.Serializable;
import java.util.ArrayList;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;


public class ClassificationJob extends Thread  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ArrayList<FeatureSignal> FeatureMatrix;
	String JobTitle;
	DoubleMatrix1D targetValue;
	Boolean Regression=false;
	public ClassificationJob(ArrayList<FeatureSignal> featureMatrix,
			String jobTitle, DoubleMatrix1D targetValue) {
		super();
		FeatureMatrix = featureMatrix;
		JobTitle = jobTitle;
		this.targetValue = targetValue;
	}
	
	
}
