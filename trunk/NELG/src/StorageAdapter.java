import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.broad.tribble.bed.BEDFeature;

import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;



public interface StorageAdapter extends Serializable{
	
	List<String> getCellLineName(String assemble);
	List<TrackRecord> getTrackId_inCellLine(String assemble,String CellLineName);
	TrackRecord getTrackById(String trackId);
	List<BEDFeature> getPeakData(TrackRecord tr);
	List<BEDFeature> getSignalContigRegion(TrackRecord tr);
	SparseDoubleMatrix2D overlapBinSignal_fixBinNum(TrackRecord feature_signal, List<BEDFeature> query_regions,int numbin);
	List<SparseDoubleMatrix1D> overlapBinSignal_fixStepSize(
			TrackRecord trackRecord, List<BEDFeature> query_regions,
			int stepsize);

}
