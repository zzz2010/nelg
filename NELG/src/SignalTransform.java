import java.util.ArrayList;
import java.util.List;

import org.broad.tribble.bed.BEDFeature;


public class SignalTransform {

public	static ArrayList<BEDFeature> NormalizedSignal(List<BEDFeature> inputSignal)
{
	ArrayList<BEDFeature> outputSignal=new ArrayList<BEDFeature>(inputSignal.size());
	return outputSignal;
}


public	static ArrayList<BEDFeature> ExtracePositveSignal(TrackRecord target_signal)
{
	ArrayList<BEDFeature> outputSignal=new ArrayList<BEDFeature>();
	return outputSignal;
}
	
public	static ArrayList<ArrayList<Float>> OverlapBinSignal(TrackRecord feature_signal, List<BEDFeature> target_signal,int numbin)
{
	///need to consider strand direction
	ArrayList<ArrayList<Float>>outputSignal=new ArrayList<ArrayList<Float>>(numbin);
	return outputSignal;
}

//compute background set 
public static ArrayList<BEDFeature> ExtraceNegativeSignal(List<BEDFeature> target_signal)
{
	//search in gap and probability
	ArrayList<BEDFeature> outputSignal=new ArrayList<BEDFeature>(2*target_signal.size());
	return outputSignal;
}

public static ArrayList<Float> BedFeatureToValues(List<BEDFeature> signal)
{
	ArrayList<Float> outputvec=new ArrayList<Float>(signal.size());
	for (int i = 0; i < signal.size(); i++) {
		outputvec.add(signal.get(i).getScore());
	}
	
	return outputvec;
}
	
}
