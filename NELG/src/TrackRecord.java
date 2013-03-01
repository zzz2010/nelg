import java.io.File;
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
	
	public static TrackRecord createTrackRecord_peak(String peakfile)
	{
		TrackRecord tr=new TrackRecord();
		tr.hasPeak=true;
		tr.hasSignal=false;
		File file = new File(peakfile);
		tr.DBoperator=new FileStorageAdapter(file.getParent());
		String[] tokens = file.getName().split("\\.(?=[^\\.]+$)");
		tr.ExperimentId=tokens[0];
		tr.FilePrefix=tokens[0];
		tr.peakSuffix=tokens[1];
		return tr;
	}
	
	public static TrackRecord createTrackRecord_signal(String signalfile)
	{
		TrackRecord tr=new TrackRecord();
		tr.hasPeak=false;
		tr.hasSignal=true;
		File file = new File(signalfile);
		tr.DBoperator=new FileStorageAdapter(file.getParent());
		String[] tokens = file.getName().split("\\.(?=[^\\.]+$)");
		tr.ExperimentId=tokens[0];
		tr.FilePrefix=tokens[0];
		tr.ReplicateSuffix=new ArrayList<String>();
		tr.ReplicateSuffix.add(tokens[1]);
		return tr;
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

	
	public String toString() {
		// TODO Auto-generated method stub
		return FilePrefix;
	}
	
	
}
