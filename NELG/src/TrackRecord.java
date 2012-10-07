import java.util.ArrayList;
import java.util.List;

import org.broad.tribble.bed.BEDFeature;


public class TrackRecord {
	
	public String FilePrefix;
	public String Cell_Line;
	public String Assembly;
	public String ExperimentId;
	public String ExperimentType;
	public String Producer;
	public boolean hasSignal;
	public boolean hasPeak;
	public StorageAdapter DBoperator;
	public ArrayList<String> ReplicateSuffix;
	public String peakSuffix;
	private List<BEDFeature> SignalRegionCache=null;
	public String getTrackId()
	{
		return FilePrefix;
	}
	
	public List<BEDFeature> getPeakData()
	{
		return DBoperator.getPeakData(this);
	}
	
	public List<BEDFeature> getSignalContigRegion()
	{
		if(SignalRegionCache==null)
			SignalRegionCache=DBoperator.getSignalContigRegion(this);
		
		return SignalRegionCache;
	}
	public List<List<Float>> OverlapBinSignal( List<BEDFeature> query_regions,int numbin)
	{
		return DBoperator.OverlapBinSignal(this, query_regions, numbin);
	}
}
