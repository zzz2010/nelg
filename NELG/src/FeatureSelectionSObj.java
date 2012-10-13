import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.broad.tribble.bed.BEDFeature;


public class FeatureSelectionSObj implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2209049590987183438L;
	ArrayList<FeatureSignal> IsThereFeatures;
	ArrayList<FeatureSignal> ValThereFeatures;
	HashMap<String, Float> FeatureAUC;
	HashMap<String, Float> FeatureCorr;
	List<BEDFeature>target_signal_filtered;
	public FeatureSelectionSObj(ArrayList<FeatureSignal> isThereFeatures,
			ArrayList<FeatureSignal> valThereFeatures,
			HashMap<String, Float> featureAUC,
			HashMap<String, Float> featureCorr,
			List<BEDFeature> target_signal_filtered) {
		super();
		IsThereFeatures = isThereFeatures;
		ValThereFeatures = valThereFeatures;
		FeatureAUC = featureAUC;
		FeatureCorr = featureCorr;
		this.target_signal_filtered = target_signal_filtered;
	}
	
}
