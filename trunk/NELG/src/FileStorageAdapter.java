import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.plaf.metal.MetalIconFactory.FileIcon16;


import org.apache.log4j.Logger;
import org.broad.igv.bbfile.BBFileHeader;
import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BigWigIterator;
import org.broad.igv.bbfile.RPChromosomeRegion;
import org.broad.igv.bbfile.WigItem;
import org.broad.igv.bbfile.ZoomDataRecord;
import org.broad.igv.bbfile.ZoomLevelIterator;
import org.broad.tribble.AbstractFeatureReader;
import org.broad.tribble.Feature;
import org.broad.tribble.TabixFeatureReader;
import org.broad.tribble.annotation.Strand;
import org.broad.tribble.bed.BEDCodec;


import org.broad.tribble.index.Index;
import org.broad.tribble.index.IndexCreator;
import org.broad.tribble.index.IndexFactory;
import org.broad.tribble.index.IndexFactory.IndexType;
import org.broad.tribble.index.interval.IntervalIndexCreator;
import org.broad.tribble.index.interval.IntervalTree;

import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import com.thoughtworks.qdox.directorywalker.DirectoryScanner;
import com.thoughtworks.qdox.directorywalker.Filter;


public class FileStorageAdapter implements StorageAdapter{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2409762312221401158L;
	static Logger log = Logger.getLogger(FileStorageAdapter.class);
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
			BufferedReader readbuffer = new BufferedReader(new FileReader(dataDir+"/alltracklist.txt"));
			String strRead=readbuffer.readLine();//skip first line
			while ((strRead=readbuffer.readLine())!=null){
				TrackRecord temp=parseLine(strRead);
				if(temp==null)
					continue; //no file exist
				DataBase.put(temp.getTrackId(), temp);
				if(!Assembly2CellLine.containsKey(temp.Assembly))
				{
					Assembly2CellLine.put(temp.Assembly, new HashSet<String>());
				}
				Assembly2CellLine.get(temp.Assembly).add(temp.Cell_Line);
				if(!CellLine2TrackId.containsKey(temp.Cell_Line))
				{
					CellLine2TrackId.put(temp.Cell_Line, new ArrayList<String>());
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
		
			temp.hasSignal=false;
		
			temp.hasPeak=false;
		
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
					temp.hasPeak=true;
				}
				else
				{
					temp.ReplicateSuffix.add(Suffix);
					temp.hasSignal=true;
				}
			}
		}
		if(temp.hasSignal==false&&temp.hasPeak==false)
			return null;
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

	public static List<SimpleBEDFeature> getBEDData(String bedfile)
	{
		ArrayList<SimpleBEDFeature> peakList=new ArrayList<SimpleBEDFeature>();
		try {
		BEDCodec codec = new BEDCodec();
		AbstractFeatureReader<SimpleBEDFeature> reader =AbstractFeatureReader.getFeatureReader(bedfile, codec,false);
		Iterable<SimpleBEDFeature> iter;

			iter = reader.iterator();
	        for (SimpleBEDFeature feat : iter) {
	        	peakList.add(feat);    
			}
	        reader.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			
			return null;
		}
		return peakList;

	}
	@Override
	public List<SimpleBEDFeature> getPeakData(TrackRecord tr) {
		// TODO Auto-generated method stub
		List<SimpleBEDFeature>  peakList=new ArrayList<SimpleBEDFeature>();
		if(tr.hasPeak)
		{
		String bedFile = dataDir+"/"+tr.FilePrefix+tr.peakSuffix;
		peakList=getBEDData(bedFile);

		
        }
		else
		{
			peakList=SignalTransform.extractPositveSignal(tr);
		}
		
		return peakList;
	}

	@Override
	public List<SimpleBEDFeature> getSignalContigRegion(TrackRecord tr) {
		// TODO Auto-generated method stub
		List<SimpleBEDFeature> SignalRegion=new ArrayList<SimpleBEDFeature>();
		if(tr.hasSignal)
		{	
			for (int i = 0; i < tr.ReplicateSuffix.size(); i++) {
				String filename=dataDir+"/"+tr.FilePrefix+tr.ReplicateSuffix.get(i);
				List<SimpleBEDFeature> ContigRegions=new ArrayList<SimpleBEDFeature>();
				try {
					BBFileReader bbReader = new BBFileReader(filename);
					BBFileHeader bbFileHdr=bbReader.getBBFileHeader();
			       
			        // get zoom level data
			        int zoomLevels = bbReader.getZoomLevelCount();
			        ZoomLevelIterator zoomIterator = null;
			        boolean contained=true;
			        int level=3; //Arbitrary select the middle level
			        contained = true;
			        // get all zoom level chromosome regions
			        RPChromosomeRegion chromosomeBounds = bbReader.getZoomLevelBounds(level);
			        zoomIterator = bbReader.getZoomLevelIterator(level, chromosomeBounds, contained);
			        // read out the all zoom data records and compare
			        // against zoom level format Table O itemCount
			        int zoomRecordCount = bbReader.getZoomLevelRecordCount(level);
			        // read out the zoom data records
			        ZoomDataRecord nextRecord = null;
			        int recordReadCount=0;
			        while (zoomIterator.hasNext()) {
			            nextRecord = zoomIterator.next();
			            if (nextRecord == null)
			                break;
			            if(nextRecord.getMaxVal()>0)
			            {
			            SimpleBEDFeature temp=new SimpleBEDFeature(nextRecord.getChromStart(), nextRecord.getChromEnd(), nextRecord.getChromName());
			           temp.setScore(nextRecord.getMeanVal());
			            ContigRegions.add(temp);
			            ++recordReadCount;
			            }
			        }
			        if(SignalRegion.size()==0)
			        {
			        	SignalRegion.addAll(ContigRegions);
			        }
			        else
			        {
			        	SignalRegion=SignalTransform.intersectSortedRegions(SignalRegion, ContigRegions);
			        }
			       
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return SignalRegion;
	}
	
	
public SparseDoubleMatrix2D overlapBinSignal_fixBinNum(TrackRecord tr, List<SimpleBEDFeature> query_regions,int numbin)
{
	///need to consider strand direction
	SparseDoubleMatrix2D outputSignal=new SparseDoubleMatrix2D(query_regions.size(), numbin);
	//initialize

	if(tr.hasSignal)
	{
		for (int i = 0; i < tr.ReplicateSuffix.size(); i++) {
			String filename=dataDir+"/"+tr.FilePrefix+tr.ReplicateSuffix.get(i);		
				BBFileReader bbReader;
				try {
					bbReader = new BBFileReader(filename);
				      // get zoom level data
			        int zoomLevels = bbReader.getZoomLevelCount();
			        int queryid=-1;
			       HashSet<String> chromNames=new HashSet<String>(bbReader.getChromosomeNames()) ;
					for(SimpleBEDFeature query:query_regions)
					{
						queryid+=1;
						//filter the chrM,..,random
						if(!chromNames.contains(query.getChr()))
							continue;
						BigWigIterator iter = bbReader.getBigWigIterator(query.getChr(), query.getStart(), query.getChr(), query.getEnd(), false);
//						int chromId=bbReader.getChromosomeID(query.getChr());
//						 RPChromosomeRegion chromosomeBounds = new RPChromosomeRegion(chromId, query.getStart(), chromId, query.getEnd());
//					    //arbitary use zoomlevel-1 
//						 ZoomLevelIterator zoomIterator = bbReader.getZoomLevelIterator(8, chromosomeBounds, false);
					    int start=query.getStart();
					    float sumValues=0;
					    int stepWidth=(query.getEnd()-start)/numbin;
					    int binId=0;
					    if(stepWidth<1)
					    	continue;
						 while(iter.hasNext())
					    {
					    	WigItem nextRecord = iter.next();	
//					    	float recordlen=nextRecord.getEndBase()-nextRecord.getStartBase()+1;
					    	if((nextRecord.getStartBase()-stepWidth)>=start)
					    	{
					    		
					    		 //add the previous one
								if(query.getStrand()== Strand.NEGATIVE)
					    		{
								outputSignal.set(queryid, numbin-binId-1, sumValues+outputSignal.get(queryid, numbin-binId-1));
					    			
					    		}
					    		else
					    			outputSignal.set(queryid, binId, sumValues+outputSignal.get(queryid, binId));
						sumValues=0;
					    		//check whether jump several bins
					    		int jumpNum=(nextRecord.getStartBase()-start)/stepWidth;
					    		for (int j = 0; j < jumpNum; j++) {
					    				binId+=1;
					    				start+=stepWidth;
									}				    		
					    	}
				    		if(binId>=numbin)
				    			break;
					    	if((nextRecord.getEndBase()-stepWidth)>=start)
					    	{
					    		int rstart=nextRecord.getStartBase();
					    		while(start<=nextRecord.getEndBase()-stepWidth)
					    		{
						    		sumValues+=nextRecord.getWigValue()*(start+stepWidth-rstart);
								if(query.getStrand()== Strand.NEGATIVE)
						    		{
						    			outputSignal.set(queryid,numbin-binId-1, sumValues+outputSignal.get(queryid,numbin-binId-1));
						    		}
						    		else
						    			outputSignal.set(queryid,binId, sumValues+outputSignal.get(queryid,binId));
						    		binId+=1;
						    		if(binId>=numbin)
						    			break;
						    		start=start+stepWidth;
						    		sumValues=0;
						    		rstart=start;
 
					    		}
					    		//contribute to the next bin
					    		sumValues=nextRecord.getWigValue()*(nextRecord.getEndBase()-start); 
					    	}
					    	else
					    		sumValues+=nextRecord.getWigValue()*(nextRecord.getEndBase()-nextRecord.getStartBase());
					    	
					    }
						 if(binId<numbin)
						 {
						 //add the final one
								if(query.getStrand()== Strand.NEGATIVE)
					    		{
								outputSignal.set(queryid, numbin-binId-1, sumValues+outputSignal.get(queryid, numbin-binId-1));
					    			
					    		}
					    		else
					    			outputSignal.set(queryid, binId, sumValues+outputSignal.get(queryid, binId));
						 }
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			
		}
	}
	else
	{
//		Index intervalTree = getIntervalTree(dataDir+"/"+tr.FilePrefix+tr.peakSuffix);
		
		BEDIntervalIndex  intervalTree=new BEDIntervalIndex(dataDir+"/"+tr.FilePrefix+tr.peakSuffix);
		int queryid=-1;
		 
		 
		for(SimpleBEDFeature query:query_regions)
		{
			queryid+=1;
			//filter the chrM,..,random
			if(!intervalTree.indexMap.containsKey(query.getChr()))
				continue;
			 int stepWidth=(int) Math.ceil((query.getEnd()-query.getStart())/numbin);
			    int binId=0;
			    if(stepWidth<1)
			    	continue;
			for (int binId1 = 0; binId1 < numbin; binId1++) {
				float sumValues=intervalTree.overlapCount(query.getChr(), query.getStart()+binId1*stepWidth, query.getStart()+(binId1+1)*stepWidth);
				if(query.getStrand()== Strand.NEGATIVE)
	    		{
				outputSignal.set(queryid, numbin-binId-1, sumValues+outputSignal.get(queryid, numbin-binId-1));
	    			
	    		}
	    		else
	    			outputSignal.set(queryid, binId, sumValues+outputSignal.get(queryid, binId));
					 
			}	 
			
		}
		
	}
	return outputSignal;
}


public List<SparseDoubleMatrix1D> overlapBinSignal_fixStepSize(TrackRecord tr, List<SimpleBEDFeature> query_regions,int StepSize)
{
	///need to consider strand direction
	List<SparseDoubleMatrix1D>outputSignal=new ArrayList<SparseDoubleMatrix1D>(query_regions.size());
	//initialize

	for (int i = 0; i < query_regions.size(); i++) {
		int numbin=(int) Math.ceil((query_regions.get(i).getEnd()-query_regions.get(i).getStart())/(double)StepSize);
		outputSignal.add( new SparseDoubleMatrix1D(numbin));
	}
	if(tr.hasSignal)
	{
		for (int i = 0; i < tr.ReplicateSuffix.size(); i++) {
			String filename=dataDir+"/"+tr.FilePrefix+tr.ReplicateSuffix.get(i);		
				BBFileReader bbReader;
				try {
					bbReader = new BBFileReader(filename);
				      // get zoom level data
			        int zoomLevels = bbReader.getZoomLevelCount();
			        int queryid=-1;
			        HashSet<String> chromNames=new HashSet<String>(bbReader.getChromosomeNames()) ;
					for(SimpleBEDFeature query:query_regions)
					{
						queryid+=1;
						//filter the chrM,..,random
						if(!chromNames.contains(query.getChr()))
							continue;
						BigWigIterator iter = bbReader.getBigWigIterator(query.getChr(), query.getStart(), query.getChr(), query.getEnd(), false);

						int numbin=(int) Math.ceil((query.getEnd()-query.getStart())/(double)StepSize);
						
					    int start=query.getStart();
					    float sumValues=0;
					    int stepWidth=StepSize;
					    int binId=0;
					    if(stepWidth<1)
					    	continue;
						 while(iter.hasNext())
					    {
					    	WigItem nextRecord = iter.next();	

//					    	float recordlen=nextRecord.getEndBase()-nextRecord.getStartBase()+1;
					    	if((nextRecord.getStartBase()-stepWidth)>=start)
					    	{

								 //add the previous one
								 if(query.getStrand()== Strand.NEGATIVE)
						    		{
						    			outputSignal.get(queryid).set(numbin-binId-1, sumValues+outputSignal.get(queryid).get(numbin-binId-1));
						    		}
						    		else
						    			outputSignal.get(queryid).set(binId, sumValues+outputSignal.get(queryid).get(binId));
						sumValues=0;
					    		//check whether jump several bins
					    		int jumpNum=(nextRecord.getStartBase()-start)/stepWidth;
					    		for (int j = 0; j < jumpNum; j++) {
					    				binId+=1;
					    				start+=stepWidth;
									}				    		
					    	}
				    		if(binId>=numbin)
				    			break;
				    		//must overlap more than one bin
					    	if((nextRecord.getEndBase()-stepWidth)>=start)
					    	{
					    		int rstart=nextRecord.getStartBase();
					    		while(start<=nextRecord.getEndBase()-stepWidth)
					    		{
						    		sumValues+=nextRecord.getWigValue()*(start+stepWidth-rstart);

								if(query.getStrand()== Strand.NEGATIVE)
						    		{
						    			outputSignal.get(queryid).set(numbin-binId-1, sumValues+outputSignal.get(queryid).get(numbin-binId-1));
						    		}
						    		else
						    			outputSignal.get(queryid).set(binId, sumValues+outputSignal.get(queryid).get(binId));
						    		binId+=1;
						    		if(binId>=numbin)
						    			break;
						    		start=start+stepWidth;
						    		sumValues=0;
						    		rstart=start;
 
					    		}
					    		//contribute to the next bin
					    		sumValues=nextRecord.getWigValue()*(nextRecord.getEndBase()-start); 
					    	}
					    	else//just overlap  one bin
					    		sumValues+=nextRecord.getWigValue()*(nextRecord.getEndBase()-nextRecord.getStartBase());
					    	
					    }
						 if(binId<numbin)
						 {
						 //add the final one
						 if(query.getStrand()== Strand.NEGATIVE)
				    		{
				    			outputSignal.get(queryid).set(numbin-binId-1, sumValues+outputSignal.get(queryid).get(numbin-binId-1));
				    		}
				    		else
				    			outputSignal.get(queryid).set(binId, sumValues+outputSignal.get(queryid).get(binId));
						 }
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			
		}
	}
	else //bed data
	{
//		Index intervalTree = getIntervalTree(dataDir+"/"+tr.FilePrefix+tr.peakSuffix);
		BEDIntervalIndex  intervalTree=new BEDIntervalIndex(dataDir+"/"+tr.FilePrefix+tr.peakSuffix);
		int queryid=-1;
		for(SimpleBEDFeature query:query_regions)
		{
			queryid+=1;
			//filter the chrM,..,random
			if(!intervalTree.indexMap.containsKey(query.getChr()))
				continue;
			int numbin=(int) Math.ceil((query.getEnd()-query.getStart())/(double)StepSize);
			 int stepWidth=StepSize;
			    int binId=0;
			    if(stepWidth<1)
			    	continue;
			for (int binId1 = 0; binId1 < numbin; binId1++) {
				float sumValues=intervalTree.overlapCount(query.getChr(), query.getStart()+binId1*stepWidth, query.getStart()+(binId1+1)*stepWidth);
				if(query.getStrand()== Strand.NEGATIVE)
	    		{
	    			outputSignal.get(queryid).set(numbin-binId1-1, sumValues+outputSignal.get(queryid).get(numbin-binId1-1));
	    		}
	    		else
	    			outputSignal.get(queryid).set(binId1, sumValues+outputSignal.get(queryid).get(binId1));
					 
			}	 
			
		}
		
	}
	return outputSignal;
}
	 //**************************** helper functions *************************

public static Index getIntervalTree(String bedfile)
{
	List<SimpleBEDFeature> rawbed=getBEDData(bedfile);
	Collections.sort(rawbed, new BEDPositionComparator());
	IndexCreator idx=new IntervalIndexCreator();
	idx.initialize(new File(bedfile), IndexType.INTERVAL_TREE.getDefaultBinSize());
	for (int i = 0; i < rawbed.size(); i++) {
		idx.addFeature(rawbed.get(i), i);
	}
	
	return idx.finalizeIndex(rawbed.size()-1);
}

    private void printRegion(String name, RPChromosomeRegion region) {
        String regionValues = String.format(name + " StartChromID =  %d, StartBase = %d,"
                + " EndChromID =  %d, EndBase = %d", region.getStartChromID(),
                region.getStartBase(), region.getEndChromID(), region.getEndBase());

        System.out.println(regionValues);
    }

   
    /*
    *   Method rus a ZoomLevelIterator which traverses all zoom data for that level.
    *
    *   Note: BBFileReader method getChromosomeIDMap can be used to find all chromosomes in the file
    *   and the method getChromosomeBounds can be used to provide a selection region for an ID range.
    * */
    private int runZoomIterator(String methodType, ZoomLevelIterator zoomIterator, int zoomRecordCount) {

        ZoomDataRecord nextRecord = null;
        int recordReadCount = 0;
        int level = zoomIterator.getZoomLevel();

        // time reading selected zoom level data
        long time = System.currentTimeMillis(), time_prev = time;

        // read out the zoom data records
        while (zoomIterator.hasNext()) {
            nextRecord = zoomIterator.next();
            if (nextRecord == null)
                break;
            ++recordReadCount;
        }

        // get the time mark and record results
        time = System.currentTimeMillis();

        int zoomLevel = zoomIterator.getZoomLevel();
        RPChromosomeRegion region = zoomIterator.getSelectionRegion();
        String name = String.format(methodType
                + " zoom level %d selected %d items out of %d\nFor region:",
                zoomLevel, recordReadCount, zoomRecordCount);

        printRegion(name, region);
        System.out.println("with read time  = " + (time - time_prev) + " ms");
        printZoomRecord(nextRecord);

        return recordReadCount;
    }

    public void printZoomRecord(ZoomDataRecord zoomDataRecord) {
        if (zoomDataRecord == null) {
            System.out.println("Last zoom record was - null!");
            return;
        }
        // print zoom record
        String record;
        record = String.format("zoom data last record %d:\n", zoomDataRecord.getRecordNumber());
        record += String.format("ChromId = %d, ", zoomDataRecord.getChromId());
        record += String.format("ChromStart = %d, ", zoomDataRecord.getChromStart());
        record += String.format("ChromEnd = %d, ", zoomDataRecord.getChromEnd());
        record += String.format("ValidCount = %d\n", zoomDataRecord.getBasesCovered());
        record += String.format("MinVal = %f, ", zoomDataRecord.getMinVal());
        record += String.format("MaxVal = %f, ", zoomDataRecord.getMaxVal());
        record += String.format("Sum values = %f, ", zoomDataRecord.getSumData());
        record += String.format("Sum squares = %f\n", zoomDataRecord.getSumSquares());
        System.out.println(record);
    }


}
