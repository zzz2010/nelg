import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.PropertyConfigurator;
import org.jppf.utils.FileUtils;

//automatically do what human do, and never end

public class NELGMain {
	 private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
	/**
	 * @param args
	 */
	 public static int max_threadNum=4;
	public static void main(String[] args) {
		PropertyConfigurator.configure( "./log4j.properties" ); 
		// TODO Auto-generated method stub
		Options options = new Options();
		options.addOption("threadnum", true, "maximum thread number");
		options.addOption("ram", false, "use ramdisk data folder");
		options.addOption("debug", false, "use debug folder");
		options.addOption("nfs", false, "assign the feature extraction job to different node, assume they share the same file system");
		options.addOption("target", true, "only predict the dataset containing this string");
		options.addOption("feature", true, "only use the feature dataset containing this string");
		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		
		
		//parsing paramters
		try {
			String appPath=new File(".").getCanonicalPath()+"/";
			cmd = parser.parse( options, args);
			if(cmd.hasOption("threadnum"))
			{
				max_threadNum=Integer.parseInt(cmd.getOptionValue("threadnum"));
				common.threadNum=max_threadNum;
			}
			if(cmd.hasOption("ram"))
			{
				common.dataDir="./data"+"_ram/";
				logger.info("using RAM data");
			}
			if(cmd.hasOption("debug"))
			{
				common.outputDir="./result_debug/";
				common.tempDir="./cache_debug/";
				FileUtils.deletePath(new File(common.outputDir));
				FileUtils.deletePath(new File(common.tempDir));
			}
			if(cmd.hasOption("target"))
			{
				common.predictTarget_debug=cmd.getOptionValue("target");
			}
			if(cmd.hasOption("nfs"))
			{
				common.NFSmode=true;
				common.tempDir=appPath+common.tempDir;
				common.dataDir=appPath+common.dataDir;
				common.outputDir=appPath+common.outputDir;
			
			}
			if(cmd.hasOption("feature"))
			{
				common.selectFeature_debug=cmd.getOptionValue("feature");
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//create directory
		(new File(common.outputDir)).mkdir();
		(new File(common.tempDir)).mkdir();
		
		ArrayList<String> Assembly=new ArrayList<String>(); 
		Assembly.add("hg19");
		StorageAdapter StorageDB =new FileStorageAdapter(common.dataDir);
		
		//phase1 
		for (int i = 0; i < Assembly.size(); i++) {
			
			//load general track
			List<TrackRecord> GeneralTrackList=StorageDB.getTrackId_inCellLine(Assembly.get(i),"All");
			//load cell line
			List<String>cell_lines=StorageDB.getCellLineName(Assembly.get(i));
		for (int j = 0; j < cell_lines.size(); j++) {
			//load track ids in the given cell line
			List<TrackRecord> TrackList=StorageDB.getTrackId_inCellLine(Assembly.get(i),cell_lines.get(j));	
			logger.debug("Signal Pool:"+TrackList);
			MotherModeler MainModelMachine=new MotherModeler(TrackList);
			MainModelMachine.threadNum=max_threadNum;
			MainModelMachine.Run();
			
		}	
			
			
		}
	}

}
