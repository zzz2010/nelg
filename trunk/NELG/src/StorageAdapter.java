import java.util.List;


public interface StorageAdapter {
	
	List<String> getCellLineName(String assemble);
	List<String> getTrackId_inCellLine(String assemble,String CellLineName);

}
