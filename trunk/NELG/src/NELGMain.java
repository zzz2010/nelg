import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.Options;
import org.broad.tribble.bed.BEDFeature;

//automatically do what human do, and never end

public class NELGMain {
	 private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Options options = new Options();
		options.addOption("threadnum", true, "maximum thread number");
		ArrayList<String> Assembly=new ArrayList<String>(); 
		Assembly.add("hg19");
		StorageAdapter StorageDB =new FileStorageAdapter("./data");
		//phase1 
		for (int i = 0; i < Assembly.size(); i++) {
			
			//load general track
			List<TrackRecord> GeneralTrackList=StorageDB.getTrackId_inCellLine(Assembly.get(i),"-");
			//load cell line
			List<String>cell_lines=StorageDB.getCellLineName(Assembly.get(i));
		for (int j = 0; j < cell_lines.size(); j++) {
			//load track ids in the given cell line
			List<TrackRecord> TrackList=StorageDB.getTrackId_inCellLine(Assembly.get(i),cell_lines.get(j));	

			MotherModeler MainModelMachine=new MotherModeler(TrackList);
			MainModelMachine.Run();
			
		}
		
		
		
		
		
			
			
		}
	}

}
