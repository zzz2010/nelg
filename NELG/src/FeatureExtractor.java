import java.util.List;

import org.broad.tribble.bed.BEDFeature;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public interface FeatureExtractor {

	public SparseDoubleMatrix2D extractSignalFeature(TrackRecord signaltrack,List<BEDFeature> query);
}
