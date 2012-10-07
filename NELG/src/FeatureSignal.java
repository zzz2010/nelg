import java.util.ArrayList;
import java.util.List;


public class FeatureSignal implements Comparable  {

	public List<Float> featureValue;
	public String FeatureId;
	public float featureSelectScore;
	public int binId=-1;
	public FeatureSignal(ArrayList<Float> featureValue, String featureId,
			float featureSelectScore) {
		super();
		this.featureValue = featureValue;
		FeatureId = featureId;
		this.featureSelectScore = featureSelectScore;
		
	}
	public FeatureSignal(List<Float> featureBestBinValue, String featureId,
			float featureSelectScore, int binId) {
		super();
		this.featureValue = featureBestBinValue;
		FeatureId = featureId;
		this.featureSelectScore = featureSelectScore;
		this.binId = binId;
	}
	@Override
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		return (int) -(this.featureSelectScore - ((FeatureSignal) arg0).featureSelectScore); //decrease order
	}
	
}
