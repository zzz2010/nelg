import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



public class FeatureSelectionSObj implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2209049590987183438L;
	ArrayList<FeatureSignal> IsThereFeatures;
	ArrayList<FeatureSignal> ValThereFeatures;

	List<SimpleBEDFeature>target_signal_filtered;
	public FeatureSelectionSObj(ArrayList<FeatureSignal> isThereFeatures,
			ArrayList<FeatureSignal> valThereFeatures,
			List<SimpleBEDFeature> target_signal_filtered) {
		super();
		IsThereFeatures = isThereFeatures;
		ValThereFeatures = valThereFeatures;

		this.target_signal_filtered = target_signal_filtered;
	}
	
}
