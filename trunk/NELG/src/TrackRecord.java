import java.util.ArrayList;


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
	
	public String getTrackId()
	{
		return FilePrefix;
	}
}
