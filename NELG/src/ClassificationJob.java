import java.io.Serializable;
import java.util.ArrayList;


public class ClassificationJob extends Thread  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ArrayList<FeatureSignal> FeatureMatrix;
	String JobTitle;
	ArrayList<Float> targetValue;
	Boolean Regression=false;
	public ClassificationJob(ArrayList<FeatureSignal> featureMatrix,
			String jobTitle, ArrayList<Float> targetValue) {
		super();
		FeatureMatrix = featureMatrix;
		JobTitle = jobTitle;
		this.targetValue = targetValue;
	}
	
	
}
