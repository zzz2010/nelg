import java.io.Serializable;
import java.util.List;


import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public interface FeatureExtractor extends Serializable{

	public SparseDoubleMatrix2D extractSignalFeature(TrackRecord signaltrack,List<SimpleBEDFeature> query);
}
