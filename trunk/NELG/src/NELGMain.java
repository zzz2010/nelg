import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.PropertyConfigurator;
import org.broad.tribble.bed.BEDFeature;

//automatically do what human do, and never end

public class NELGMain {
	 private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
	/**
	 * @param args
	 */
	 public static int max_threadNum=4;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Options options = new Options();
		options.addOption("threadnum", true, "maximum thread number");
		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		//parsing paramters
		try {
			cmd = parser.parse( options, args);
			if(cmd.hasOption("threadnum"))
			{
				max_threadNum=Integer.parseInt(cmd.getOptionValue("threadnum"));
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		
		
		
		
		
		
		ArrayList<String> Assembly=new ArrayList<String>(); 
		Assembly.add("hg19");
		StorageAdapter StorageDB =new FileStorageAdapter("./data");
		PropertyConfigurator.configure( "./log4j.properties" ); 
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
			MainModelMachine.threadNum=max_threadNum;
			MainModelMachine.Run();
			
		}
		
		
		
		
		
			
			
		}
	}

}
