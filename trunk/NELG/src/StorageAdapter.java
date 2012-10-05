import java.util.List;

import org.broad.tribble.bed.BEDFeature;



public interface StorageAdapter {
	
	List<String> getCellLineName(String assemble);
	List<String> getTrackId_inCellLine(String assemble,String CellLineName);
	List<BEDFeature> getTrackById(String trackId);

}
