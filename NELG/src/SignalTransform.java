import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.broad.tribble.bed.BEDFeature;
import org.broad.tribble.bed.SimpleBEDFeature;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;


public class SignalTransform {

public	static ArrayList<BEDFeature> normalizeSignal(List<BEDFeature> inputSignal)
{
	//do simple log2 normalized
	ArrayList<BEDFeature> outputSignal=new ArrayList<BEDFeature>(inputSignal.size());
	DoubleMatrix1D scores = BedFeatureToValues(inputSignal);
	double m=scores.zSum()/scores.size();
	double pseudocount= 1;
	for (int i = 0; i < outputSignal.size(); i++) {
		SimpleBEDFeature temp=new SimpleBEDFeature(inputSignal.get(i).getStart(),inputSignal.get(i).getEnd(), inputSignal.get(i).getChr());
		temp.setScore((float) Math.log(inputSignal.get(i).getScore()/m+pseudocount));
		outputSignal.add(temp);
	}
	
	return outputSignal;
}


public	static ArrayList<BEDFeature> extractPositveSignal(TrackRecord target_signal)
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
		List<BEDFeature> SignalRegions=target_signal.getSignalContigRegion();
		List<BEDFeature> SignalRegions2=new ArrayList<BEDFeature>();
		//filter short region <400
		for(BEDFeature region:SignalRegions)
		{
			if(region.getEnd()-region.getStart()>400)
				SignalRegions2.add(region);
		}
		List<SparseDoubleMatrix1D> SignalOverRegions = target_signal.OverlapBinSignal(SignalRegions2, 100);
		//peak calling
		peaklist=PeakCalling.simple_peak_detection(SignalOverRegions, SignalRegions2);	
	}
	//sort by score, take only top ones
	Collections.sort(peaklist, new BEDScoreComparator());
	for (int i = 0; i < Math.min(maxExtract, peaklist.size()); i++) {
		outputSignal.add(peaklist.get(i));
	}
	return outputSignal;
}
	
public	static List<SparseDoubleMatrix1D> OverlapBinSignal(TrackRecord feature_signal, List<BEDFeature> query_regions,int numbin)
{
	return feature_signal.OverlapBinSignal(query_regions, numbin);
}

//compute background set 
public static ArrayList<BEDFeature> extractNegativeSignal(List<BEDFeature> target_signal,int num)
{
	//search in gap and probability
	ArrayList<BEDFeature> outputSignal=new ArrayList<BEDFeature>(2*target_signal.size());
	Collections.sort(target_signal, new BEDPositionComparator());
	List<BEDFeature> gaplist=new ArrayList<BEDFeature>();
	double sumCoverage=0;
	for (int i = 0; i < target_signal.size()-1; i++) {
		BEDFeature bed1=target_signal.get(i);
		BEDFeature bed2=target_signal.get(i+1);
		if(bed1.getChr().equalsIgnoreCase(bed2.getChr()))
		{
			if(bed1.getEnd()<bed2.getStart())
			{
				SimpleBEDFeature temp=new SimpleBEDFeature(bed1.getEnd(), bed2.getStart(), bed1.getChr());
				temp.setScore(-1); //negative sample
				sumCoverage+=temp.getScore();
				gaplist.add(temp);
			}
		}
	}
	//cumulative probablity
	ArrayList<Double> cumprob=new ArrayList<Double>();
	double sum=0;
	for (int i = 0; i < gaplist.size(); i++) {
		cumprob.add(sum);
		sum+=gaplist.get(i).getScore()/sumCoverage;
	}
	Random rand=new Random(12345);
	for (int i = 0; i < num; i++) {
		int selregion=rand.nextInt(target_signal.size());
		int bgregion_size=target_signal.get(selregion).getEnd()-target_signal.get(selregion).getStart();
		double pointer=rand.nextDouble();
		int selgap=-Collections.binarySearch(cumprob,pointer )-2 ;
		String chrom=gaplist.get(selgap).getChr();
		int regionlen=gaplist.get(selgap).getEnd()-gaplist.get(selgap).getStart();
		int start=(int) (gaplist.get(selgap).getStart()+regionlen*(pointer-cumprob.get(selgap)));
		outputSignal.add(new SimpleBEDFeature(start-bgregion_size/2, start+bgregion_size/2, chrom));
	}
	return outputSignal;
}

public static DoubleMatrix1D BedFeatureToValues(List<BEDFeature> signal)
{
	DoubleMatrix1D outputvec=new SparseDoubleMatrix1D(signal.size());
	for (int i = 0; i < signal.size(); i++) {
		if(signal.get(i).getScore()>0)
		outputvec.set(i, signal.get(i).getScore());
	}
	
	return outputvec;
}


public static List<BEDFeature> intersectSortedRegions(List<BEDFeature> list1,List<BEDFeature> list2)
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
