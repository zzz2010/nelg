import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.broad.tribble.bed.BEDFeature;
import org.broad.tribble.bed.SimpleBEDFeature;


public class SignalTransform {

public	static ArrayList<BEDFeature> NormalizedSignal(List<BEDFeature> inputSignal)
{
	ArrayList<BEDFeature> outputSignal=new ArrayList<BEDFeature>(inputSignal.size());
	return outputSignal;
}


public	static ArrayList<BEDFeature> ExtracePositveSignal(TrackRecord target_signal)
{
	int maxExtract=10000;
	ArrayList<BEDFeature> outputSignal=new ArrayList<BEDFeature>();
	List<BEDFeature> peaklist=null;
	//if peak file exist, directly use peak file
	if(target_signal.hasPeak)
	{
		peaklist=target_signal.getPeakData();	
	}
	else //peak calling from signal file
	{
		peaklist=target_signal.getSignalContigRegion();
	}
	//sort by score, take only top ones
	Collections.sort(peaklist, new BEDScoreComparator());
	for (int i = 0; i < Math.min(maxExtract, peaklist.size()); i++) {
		outputSignal.add(peaklist.get(i));
	}
	return outputSignal;
}
	
public	static List<List<Float>> OverlapBinSignal(TrackRecord feature_signal, List<BEDFeature> query_regions,int numbin)
{
	return feature_signal.OverlapBinSignal(query_regions, numbin);
}

//compute background set 
public static ArrayList<BEDFeature> ExtraceNegativeSignal(List<BEDFeature> target_signal)
{
	//search in gap and probability
	ArrayList<BEDFeature> outputSignal=new ArrayList<BEDFeature>(2*target_signal.size());
	return outputSignal;
}

public static ArrayList<Float> BedFeatureToValues(List<BEDFeature> signal)
{
	ArrayList<Float> outputvec=new ArrayList<Float>(signal.size());
	for (int i = 0; i < signal.size(); i++) {
		outputvec.add(signal.get(i).getScore());
	}
	
	return outputvec;
}

public static List<BEDFeature> IntersectSortedRegions(List<BEDFeature> list1,List<BEDFeature> list2)
{
	List<BEDFeature> intersectList=new ArrayList<BEDFeature>();
	if(list1.size()>0&&list2.size()>0)
	{
	Iterator<BEDFeature> it1 = list1.iterator();
	Iterator<BEDFeature> it2 = list2.iterator();
	BEDFeature el1=it1.next();
	BEDFeature el2=it2.next();
	BEDPositionComparator comparator=new BEDPositionComparator();
	do
	{
		int compareCode=comparator.compare(el1, el2);
		
		switch (compareCode) {
		//different start point, the same chromosome
		case 1:
			if(el1.getStart()<el2.getEnd())
			{
				//overlap
				int endpos=Math.min(el1.getEnd(), el2.getEnd());
				if(endpos-el1.getStart()>100)
				{
				SimpleBEDFeature temp=new SimpleBEDFeature(el1.getStart(), endpos, el1.getChr());
				intersectList.add(temp);
				}
			}
			el2=it2.next();
			break;
		case -1:
			if(el2.getStart()<el1.getEnd())
			{
				//overlap
				int endpos=Math.min(el1.getEnd(), el2.getEnd());
				if(endpos-el1.getStart()>100)
				{
				SimpleBEDFeature temp=new SimpleBEDFeature(el2.getStart(), endpos, el1.getChr());
				intersectList.add(temp);
				}
			}
			el1=it1.next();
			break;
		//with same starting point, so must overlap
		case 2:
		{
			SimpleBEDFeature temp=new SimpleBEDFeature(el1.getStart(), el2.getEnd(), el1.getChr());
			intersectList.add(temp);
		}
			el2=it2.next();
			break;
		case -2:
		{			
			SimpleBEDFeature temp=new SimpleBEDFeature(el1.getStart(), el1.getEnd(), el1.getChr());
			intersectList.add(temp);
		}
			el1=it1.next();
			break;
		//different chromosome
		case 3:
			el2=it2.next();
			break;
		case -3:
			el1=it1.next();
			break;
		
		default://complete equal 
			intersectList.add(new SimpleBEDFeature(el1.getStart(), el1.getEnd(), el1.getChr()));
			el2=it2.next();
			el1=it1.next();
			break;
		}
		
		
	}while(it1.hasNext()&&it2.hasNext());
	}
	return intersectList;
}	

}
