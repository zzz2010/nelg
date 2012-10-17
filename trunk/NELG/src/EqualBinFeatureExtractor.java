import java.util.List;

import org.broad.tribble.bed.BEDFeature;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class EqualBinFeatureExtractor implements FeatureExtractor {

	int numBin;
	
	public EqualBinFeatureExtractor(int numBin) {
		super();
		this.numBin = numBin;
	}

	@Override
	public SparseDoubleMatrix2D extractSignalFeature(TrackRecord signaltrack,
			List<BEDFeature> query) {
		SparseDoubleMatrix2D feature_BinSignal=SignalTransform.OverlapBinSignal(signaltrack, query,numBin);
		return feature_BinSignal;
	}

}
