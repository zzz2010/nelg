import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.plaf.metal.MetalIconFactory.FileIcon16;

import org.broad.tribble.bed.BEDFeature;

import com.thoughtworks.qdox.directorywalker.DirectoryScanner;
import com.thoughtworks.qdox.directorywalker.Filter;


public class FileStorageAdapter implements StorageAdapter {

	String dataDir;
	HashMap<String, TrackRecord> DataBase;
	HashMap<String, HashSet<String>> Assembly2CellLine;
	HashMap<String, List<String>> CellLine2TrackId;//may mix different Assembly
	public FileStorageAdapter(String dataDir) {
		super();
		this.dataDir = dataDir;
		DataBase=new HashMap<String, TrackRecord>();
		Assembly2CellLine=new HashMap<String, HashSet<String>>();
		CellLine2TrackId=new HashMap<String, List<String>>();
		//load the description file
		try {
			BufferedReader readbuffer = new BufferedReader(new FileReader(dataDir+"/tracklist.txt"));
			String strRead=readbuffer.readLine();//skip first line
			while ((strRead=readbuffer.readLine())!=null){
				TrackRecord temp=parseLine(strRead);
				DataBase.put(temp.getTrackId(), temp);
				if(!Assembly2CellLine.containsKey(temp.Assembly))
				{
					Assembly2CellLine.put(temp.Assembly, new HashSet<String>());
				}
				Assembly2CellLine.get(temp.Assembly).add(temp.Cell_Line);
				if(!CellLine2TrackId.containsKey(temp.Cell_Line))
				{
					CellLine2TrackId.put(temp.Cell_Line, new ArrayList());
				}
				CellLine2TrackId.get(temp.Cell_Line).add(temp.getTrackId());
				}
	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//parse a text line to TrackRecord object
	TrackRecord parseLine(String line)
	{
		TrackRecord temp=new TrackRecord();
		String splitarray[] = line.trim().split("\t");
		temp.FilePrefix=splitarray[0];
		temp.Assembly=splitarray[1];
		temp.Cell_Line=splitarray[2];
		temp.DBoperator=this;
		temp.ExperimentId=splitarray[3];
		temp.ExperimentType=splitarray[4];
		temp.Producer=splitarray[5];
		if(splitarray[6].contains("signal"))
			temp.hasSignal=true;
		if(splitarray[6].contains("peak"))
			temp.hasPeak=true;
		
		File fake_file=new File(dataDir+"/"+temp.FilePrefix);
		File dir = new File(fake_file.getParent());
		String[] children = dir.list();
		//check replicate and peak suffix
		temp.ReplicateSuffix=new ArrayList<String>();
		for(String file1:children)
		{
			if(file1.contains(fake_file.getName()))
			{
				String[] comps=file1.split(fake_file.getName());
				String Suffix=comps[comps.length-1];
				if(Suffix.contains("Peak"))
				{
					temp.peakSuffix=Suffix;
				}
				else
				{
					temp.ReplicateSuffix.add(Suffix);
				}
			}
		}
		
		return temp;
	}
	
	@Override
	public List<String> getCellLineName(String assembly) {
		// TODO Auto-generated method stub
		return new ArrayList<String>(Assembly2CellLine.get(assembly));
	}

	@Override
	public List<TrackRecord> getTrackId_inCellLine(String assemble,
			String CellLineName) {
		// TODO Auto-generated method stub
		 List<String> unfiltered=CellLine2TrackId.get(CellLineName);
		 if(unfiltered==null)
			 return new  ArrayList<TrackRecord>() ;
		 List<TrackRecord> filteredRecord=new  ArrayList<TrackRecord>(unfiltered.size());
		 for(String trackId:unfiltered)
		 {
			 TrackRecord  temp=getTrackById(trackId);
			 if(temp.Assembly.contentEquals(assemble))
			 {
				 filteredRecord.add(temp);
			 }
			 
		 }
		return filteredRecord;
	}

	@Override
	public TrackRecord getTrackById(String trackId) {
		// TODO Auto-generated method stub
		return DataBase.get(trackId);
	}

}
