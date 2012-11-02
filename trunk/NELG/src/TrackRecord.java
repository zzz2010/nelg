import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


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
	private List<SimpleBEDFeature> SignalRegionCache=null;
	public List<SimpleBEDFeature> PeakCache=null;
	public String getTrackId()
	{
		return FilePrefix;
	}
	
	public List<SimpleBEDFeature> getPeakData()
	{
		if(PeakCache==null)
		{
			PeakCache= DBoperator.getPeakData(this);
		}
		return PeakCache;
	}
	
	public List<SimpleBEDFeature> getSignalContigRegion()
	{
		if(SignalRegionCache==null)
			SignalRegionCache=DBoperator.getSignalContigRegion(this);
		
		return SignalRegionCache;
	}
	public SparseDoubleMatrix2D overlapBinSignal_fixBinNum( List<SimpleBEDFeature> query_regions,int numbin)
	{
		return DBoperator.overlapBinSignal_fixBinNum(this, query_regions, numbin);
	}
	public List<SparseDoubleMatrix1D> overlapBinSignal_fixStepSize( List<SimpleBEDFeature> query_regions,int stepsize)
	{
		return DBoperator.overlapBinSignal_fixStepSize(this, query_regions, stepsize);
	}
}
