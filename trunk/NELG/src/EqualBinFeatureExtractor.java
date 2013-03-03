import java.util.List;


import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class EqualBinFeatureExtractor implements FeatureExtractor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 80062067409246792L;
	int numBin;
	boolean isRowNormalized=false;
	public EqualBinFeatureExtractor(int numBin) {
		super();
		this.numBin = numBin;
	}

	
	public SparseDoubleMatrix2D extractSignalFeature(TrackRecord signaltrack,
			List<SimpleBEDFeature> query) {
		SparseDoubleMatrix2D feature_BinSignal=SignalTransform.OverlapBinSignal(signaltrack, query,numBin);
		
		if(isRowNormalized)
			return (SparseDoubleMatrix2D) common.RowNormalizeMatrix(feature_BinSignal);
		return feature_BinSignal;
	}

}
