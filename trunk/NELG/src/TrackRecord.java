import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.broad.tribble.bed.BEDFeature;

import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class TrackRecord implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8359690396320177886L;
	public String FilePrefix;
	public String Cell_Line;
	public String Assembly;
	public String ExperimentId;
	public String ExperimentType;
	public String Producer;
	public boolean hasSignal;
	public boolean hasPeak;
	public StorageAdapter DBoperator=null;
	public ArrayList<String> ReplicateSuffix;
	public String peakSuffix;
	private List<BEDFeature> SignalRegionCache=null;
	private List<BEDFeature> PeakCache=null;
	public String getTrackId()
	{
		return FilePrefix;
	}
	
	public List<BEDFeature> getPeakData()
	{
		if(PeakCache==null)
		{
			PeakCache= DBoperator.getPeakData(this);
		}
		return PeakCache;
	}
	
	public List<BEDFeature> getSignalContigRegion()
	{
		if(SignalRegionCache==null)
			SignalRegionCache=DBoperator.getSignalContigRegion(this);
		
		return SignalRegionCache;
	}
	public SparseDoubleMatrix2D overlapBinSignal_fixBinNum( List<BEDFeature> query_regions,int numbin)
	{
		return DBoperator.overlapBinSignal_fixBinNum(this, query_regions, numbin);
	}
	public List<SparseDoubleMatrix1D> overlapBinSignal_fixStepSize( List<BEDFeature> query_regions,int stepsize)
	{
		return DBoperator.overlapBinSignal_fixStepSize(this, query_regions, stepsize);
	}
}
