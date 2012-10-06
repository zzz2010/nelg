import java.util.List;

import org.broad.tribble.bed.BEDFeature;



public interface StorageAdapter {
	
	List<String> getCellLineName(String assemble);
	List<TrackRecord> getTrackId_inCellLine(String assemble,String CellLineName);
	TrackRecord getTrackById(String trackId);

}
