import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.log4j.Logger;



import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class SignalTransform {
	static Logger logger = Logger.getLogger(SignalTransform.class);
	
//find the best to make the raw value follow the normal distribution
public	static ArrayList<SimpleBEDFeature> normalizeSignal(List<SimpleBEDFeature> inputSignal)
{
	//do simple log2 normalized
	ArrayList<SimpleBEDFeature> outputSignal=new ArrayList<SimpleBEDFeature>(inputSignal.size());
	DoubleMatrix1D scores = BedFeatureToValues(inputSignal);
	scores=normalizeSignal(scores);
	for (int i = 0; i < inputSignal.size(); i++) {
		SimpleBEDFeature temp=new SimpleBEDFeature(inputSignal.get(i).getStart(),inputSignal.get(i).getEnd(), inputSignal.get(i).getChr());
		temp.setScore((float)scores.get(i) );//( Math.log(inputSignal.get(i).getScore()+pseudocount)-logm));
		outputSignal.add(temp);
	}
	
	return outputSignal;
}

public	static  DoubleMatrix1D normalizeSignal(DoubleMatrix1D inputSignal)
{
	double bestPCC=0;
	double[] sortedValue=inputSignal.toArray();
	Arrays.sort(sortedValue);
	PearsonsCorrelation corr=new PearsonsCorrelation();
	
	
	double[] rank=new double[sortedValue.length];
	for (int i = 0; i < rank.length; i++) {
		rank[i]=i;
	}
	int bestType=0;
	for (int i = 0; i < 7; i++) {
		double[] normValues=tryDifferentTransform(sortedValue, i);
		double pearson=corr.correlation(rank,normValues);
		if(bestPCC<pearson)
		{
			bestPCC=pearson;
			bestType=i;
		}
	}
	logger.debug("normalization type: "+bestType);
	DenseDoubleMatrix1D outputSignal=new DenseDoubleMatrix1D(tryDifferentTransform(inputSignal.toArray(), bestType));
return outputSignal;	
}

//static double testNormality(double[] values)
//{
//
//}

public	static ArrayList<SimpleBEDFeature> extractPositveSignal(TrackRecord target_signal)
{
	int maxExtract=10000;
	ArrayList<SimpleBEDFeature> outputSignal=new ArrayList<SimpleBEDFeature>();
	List<SimpleBEDFeature> peaklist=null;
	//if peak file exist, directly use peak file
	if(target_signal.hasPeak)
	{
		peaklist=target_signal.getPeakData();	
	}
	else //peak calling from signal file
	{
		List<SimpleBEDFeature> SignalRegions=target_signal.getSignalContigRegion();
		List<SimpleBEDFeature> SignalRegions2=new ArrayList<SimpleBEDFeature>();
		//filter short region <400
		double sumscore=0;
		double sumscoreSQ=0;
	
		for(SimpleBEDFeature region:SignalRegions)
		{
//			logger.info(region.getEnd()-region.getStart());
			if(region.getEnd()-region.getStart()>400)
			{
				SignalRegions2.add(region);
				sumscore+=region.getScore();
				sumscoreSQ+=region.getScore()*region.getScore();
			}
			//need to consider case they have peak property
			else
			{
				//if score significantly higher than previous one
				double mscore=sumscore/SignalRegions2.size();
				double stdscore=Math.sqrt(sumscoreSQ/SignalRegions2.size()-mscore*mscore)+1;
				double zscore=(region.getScore()-mscore)/stdscore;
				if(zscore>1)
				{
					SimpleBEDFeature region2=new SimpleBEDFeature(region.getStart()-200, region.getEnd()+200, region.getChr());
					region2.setScore(region.getScore());
					region2.setStrand(region.getStrand());
					SignalRegions2.add(region2);
					sumscore+=region.getScore();
					sumscoreSQ+=region.getScore()*region.getScore();
				}
			}
		}
		List<SparseDoubleMatrix1D> SignalOverRegions = target_signal.overlapBinSignal_fixStepSize(SignalRegions2, 200);//200bp per bin
		//peak calling
		peaklist=PeakCalling.simple_peak_detection(SignalOverRegions, SignalRegions2);
	}
	//sort by score, take only top ones
	Collections.sort(peaklist, new BEDScoreComparator());
	if(!target_signal.hasPeak)
		SimpleBEDFeature.toFile(peaklist, target_signal.FilePrefix+".bed");
	for (int i = 0; i < Math.min(maxExtract, peaklist.size()); i++) {
		outputSignal.add(peaklist.get(i));
	}
	logger.info(target_signal.ExperimentId+": call "+outputSignal.size()+" peaks");
	return outputSignal;
}
	
public	static SparseDoubleMatrix2D OverlapBinSignal(TrackRecord feature_signal, List<SimpleBEDFeature> query_regions,int numbin)
{
	return feature_signal.overlapBinSignal_fixBinNum(query_regions, numbin);
}

//compute background set 
public static ArrayList<SimpleBEDFeature> extractNegativeSignal_Gauss(List<SimpleBEDFeature> target_signal,int num)
{
	//search in gap and probability
	ArrayList<SimpleBEDFeature> outputSignal=new ArrayList<SimpleBEDFeature>(2*target_signal.size());
	List<SimpleBEDFeature>  target_signal_sorted=new ArrayList<SimpleBEDFeature>(target_signal);
	Collections.sort(target_signal_sorted, new BEDPositionComparator());
	List<SimpleBEDFeature> gaplist=new ArrayList<SimpleBEDFeature>();
	double sumCoverage=0;
	for (int i = 0; i < target_signal_sorted.size()-1; i++) {
		SimpleBEDFeature bed1=target_signal_sorted.get(i);
		SimpleBEDFeature bed2=target_signal_sorted.get(i+1);
		if(bed1.getChr().equalsIgnoreCase(bed2.getChr()))
		{
			int mid1=(bed1.getEnd()+bed1.getStart())/2;
			int mid2=(bed2.getEnd()+bed2.getStart())/2;
			
			if(mid1+10000<mid2) //ensure have enough gap
			{
				//make bg peak at least 500bp away to the real peak
				SimpleBEDFeature temp=new SimpleBEDFeature(mid1+500,mid2-500, bed1.getChr());
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
		
		int selregion=rand.nextInt(target_signal_sorted.size());

		int bgregion_size=target_signal_sorted.get(selregion).getEnd()-target_signal_sorted.get(selregion).getStart();
		double pointer=rand.nextDouble();
		int selgap=-Collections.binarySearch(cumprob,pointer )-2 ;
		String chrom=gaplist.get(selgap).getChr();
		int regionlen=gaplist.get(selgap).getEnd()-gaplist.get(selgap).getStart();
		int start=0;//(int) (gaplist.get(selgap).getStart()+regionlen*(pointer-cumprob.get(selgap)));
		double deviate=rand.nextGaussian();
		if(deviate<0)
		{
			start=(int) (gaplist.get(selgap).getStart()+regionlen*(-deviate/4));
		}
		else
		{
			start=(int) (gaplist.get(selgap).getEnd()+regionlen*(-deviate/4));
		}
		SimpleBEDFeature temp = new SimpleBEDFeature(start-bgregion_size/2, start+bgregion_size/2, chrom);
		temp.setScore(-1);
		outputSignal.add(temp);
	}
	return outputSignal;
}

//compute background set 
public static ArrayList<SimpleBEDFeature> extractNegativeSignal_Uniform(List<SimpleBEDFeature> target_signal,int num)
{
	//search in gap and probability
	ArrayList<SimpleBEDFeature> outputSignal=new ArrayList<SimpleBEDFeature>(2*target_signal.size());
	List<SimpleBEDFeature>  target_signal_sorted=new ArrayList<SimpleBEDFeature>(target_signal);
	Collections.sort(target_signal_sorted, new BEDPositionComparator());
	List<SimpleBEDFeature> gaplist=new ArrayList<SimpleBEDFeature>();
	double sumCoverage=0;
	for (int i = 0; i < target_signal_sorted.size()-1; i++) {
		SimpleBEDFeature bed1=target_signal_sorted.get(i);
		SimpleBEDFeature bed2=target_signal_sorted.get(i+1);
		if(bed1.getChr().equalsIgnoreCase(bed2.getChr()))
		{
			int mid1=(bed1.getEnd()+bed1.getStart())/2;
			int mid2=(bed2.getEnd()+bed2.getStart())/2;
			
			if(mid1+10000<mid2) //ensure have enough gap
			{
				SimpleBEDFeature temp=new SimpleBEDFeature(mid1, mid2, bed1.getChr());
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
		
		int selregion=rand.nextInt(target_signal_sorted.size());

		int bgregion_size=target_signal_sorted.get(selregion).getEnd()-target_signal_sorted.get(selregion).getStart();
		double pointer=rand.nextDouble();
		int selgap=-Collections.binarySearch(cumprob,pointer )-2 ;
		String chrom=gaplist.get(selgap).getChr();
		int regionlen=gaplist.get(selgap).getEnd()-gaplist.get(selgap).getStart();
		int start=(int) (gaplist.get(selgap).getStart()+regionlen*(pointer-cumprob.get(selgap)));
		SimpleBEDFeature temp = new SimpleBEDFeature(start-bgregion_size/2, start+bgregion_size/2, chrom);
		temp.setScore(-1);
		outputSignal.add(temp);
	}
	return outputSignal;
}

//compute background set 
public static ArrayList<SimpleBEDFeature> extractNegativeSignal(List<SimpleBEDFeature> target_signal,int num)
{
	//search in gap and probability
	ArrayList<SimpleBEDFeature> outputSignal=extractNegativeSignal_Gauss(target_signal,num);

	return outputSignal;
}
public static DoubleMatrix1D BedFeatureToValues(List<SimpleBEDFeature> signal)
{
	DoubleMatrix1D outputvec=new SparseDoubleMatrix1D(signal.size());
	for (int i = 0; i < signal.size(); i++) {
		outputvec.set(i, signal.get(i).getScore());
	}
	
	return outputvec;
}

//get the window region around the center of input regions
public static List<SimpleBEDFeature> fixRegionSize(List<SimpleBEDFeature> list1, int regionsize, boolean filterNegPos)
{
	List<SimpleBEDFeature> fixlist=new ArrayList<SimpleBEDFeature>(list1.size());  
	for (int i = 0; i < list1.size(); i++) {
		int midpoint=(list1.get(i).getStart()+list1.get(i).getEnd())/2;
		if(midpoint-regionsize/2<0&&filterNegPos)//ignore negative position
			continue;
		SimpleBEDFeature sbed=new SimpleBEDFeature(midpoint-regionsize/2, midpoint+regionsize/2, list1.get(i).getChr());
		sbed.setDescription(list1.get(i).getDescription());
		sbed.setStrand(list1.get(i).getStrand());
		sbed.setScore(list1.get(i).getScore());
		fixlist.add(sbed);
	}
	
	return fixlist;
}

public static List<SimpleBEDFeature> sortUnique(List<SimpleBEDFeature> list1)
{
	List<SimpleBEDFeature> sortlist=new ArrayList<SimpleBEDFeature>(list1.size());
	BEDPositionComparator comparator = new BEDPositionComparator();
	Collections.sort(list1, comparator);
	
	SimpleBEDFeature p1=list1.get(0);
	sortlist.add(p1);
	int lastpos=(p1.getEnd()+p1.getStart())/2;
	for (int i = 1; i < list1.size(); i++) {
		 p1 = list1.get(i-1);
		SimpleBEDFeature p2 = list1.get(i);
		int currpos=(p2.getEnd()+p2.getStart())/2;
		if(p1.getChr().contentEquals(p2.getChr())&&Math.abs(currpos-lastpos)<200)
			continue;
		else
		{
			sortlist.add(list1.get(i));
			lastpos=currpos;
		}
		
	}
	Collections.sort(sortlist,new BEDScoreComparator());
	return sortlist;
}

public static List<SimpleBEDFeature> intersectSortedRegions(List<SimpleBEDFeature> list1,List<SimpleBEDFeature> list2)
{
	List<SimpleBEDFeature> intersectList=new ArrayList<SimpleBEDFeature>();
	if(list1.size()>0&&list2.size()>0)
	{
	Iterator<SimpleBEDFeature> it1 = list1.iterator();
	Iterator<SimpleBEDFeature> it2 = list2.iterator();
	SimpleBEDFeature el1=it1.next();
	SimpleBEDFeature el2=it2.next();
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
//				if(endpos-el1.getStart()>1)
				{
				SimpleBEDFeature temp=new SimpleBEDFeature(el1.getStart(), endpos, el1.getChr());
				temp.setScore(Math.max(el1.getScore(), el2.getScore()));
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
//				if(endpos-el1.getStart()>1)
				{
				SimpleBEDFeature temp=new SimpleBEDFeature(el2.getStart(), endpos, el1.getChr());
				temp.setScore(Math.max(el1.getScore(), el2.getScore()));
				intersectList.add(temp);
				}
			}
			el1=it1.next();
			break;
		//with same starting point, so must overlap
		case 2:
		{
			SimpleBEDFeature temp=new SimpleBEDFeature(el1.getStart(), el2.getEnd(), el1.getChr());
			temp.setScore(Math.max(el1.getScore(), el2.getScore()));
			intersectList.add(temp);
		}
			el2=it2.next();
			break;
		case -2:
		{			
			SimpleBEDFeature temp=new SimpleBEDFeature(el1.getStart(), el1.getEnd(), el1.getChr());
			temp.setScore(Math.max(el1.getScore(), el2.getScore()));
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
			intersectList.add(el1);
			el2=it2.next();
			el1=it1.next();
			break;
		}
		
		
	}while(it1.hasNext()&&it2.hasNext());
	}
	return intersectList;
}	

public static double tryDifferentTransform(double raw, int method)
{
	double temp=0;
	switch (method) {
	case 1:
		 temp=Math.log(1+raw);
		if(Double.isInfinite(temp)||Double.isNaN(temp) )
			temp=raw;
		return temp;
	case 2:
		 temp=Math.exp(raw/1000);
		if(Double.isInfinite(temp)||Double.isNaN(temp) )
			temp=raw;
		return temp;
	case 3:
		temp=raw*raw;
		if(Double.isInfinite(temp)||Double.isNaN(temp) )
			temp=raw;
		return temp;
	case 4:
		temp=raw*raw*raw;
		if(Double.isInfinite(temp)||Double.isNaN(temp) )
			temp=raw;
		return temp;
	case 5:
		temp=1/(raw+1);
		if(Double.isInfinite(temp)||Double.isNaN(temp) )
			temp=raw;
		return temp;
	case 6:
		temp=Math.sqrt(raw);
		if(Double.isInfinite(temp)||Double.isNaN(temp) )
			temp=raw;
		return temp;
	default:
		return raw;
	}
}

public static double[]  tryDifferentTransform(double[] raw, int method)
{ 
	double[]  retVal=new double[raw.length];
	for (int i = 0; i < retVal.length; i++) {
		retVal[i]=tryDifferentTransform(raw[i],method);
	}
   return retVal;
}

}
