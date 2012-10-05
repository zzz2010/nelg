import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.Options;
import org.broad.tribble.bed.BEDFeature;

//automatically do what human do, and never end

public class NELGMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Options options = new Options();
		options.addOption("threadnum", true, "maximum thread number");
		ArrayList<String> Assembly=new ArrayList<String>(); 
		Assembly.add("hg19");
		StorageAdapter StorageDB = null;
		//phase1 
		for (int i = 0; i < Assembly.size(); i++) {
			
			//load general track
			List<String> GeneralTrackList=StorageDB.getTrackId_inCellLine(Assembly.get(i),"");
			//load cell line
			List<String>cell_lines=StorageDB.getCellLineName(Assembly.get(i));
		for (int j = 0; j < cell_lines.size(); j++) {
			//load track ids in the given cell line
			List<String> TrackList=StorageDB.getTrackId_inCellLine(Assembly.get(i),cell_lines.get(j));	
			HashMap<String, List<BEDFeature>> SignalPool=new HashMap<String, List<BEDFeature>>();
			for (int k = 0; k < TrackList.size(); k++) {
				String TargetTrackId=TrackList.get(k);
				//Library Loading
				List<BEDFeature> TrackData=StorageDB.getTrackById(TargetTrackId);
				SignalPool.put(TargetTrackId, TrackData);
			}	
			
	
			MotherModeler MainModelMachine=new MotherModeler(SignalPool);
			
		}
		
		
		
		
		
			
			
		}
	}

}
