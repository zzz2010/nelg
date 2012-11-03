import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

import org.broad.tribble.bed.BEDCodec;

import org.broad.tribble.index.interval.Interval;
import org.broad.tribble.index.interval.IntervalTree;


public class BEDIntervalIndex {

	LinkedHashMap<String, IntervalTree> indexMap;
	LinkedHashMap<String, TreeMap<Integer, Float>> valMap;
	

	public BEDIntervalIndex(String bedfile) {
		List<SimpleBEDFeature> rawbed=FileStorageAdapter.getBEDData(bedfile);
		indexMap=new LinkedHashMap<String, IntervalTree>();
		valMap=new LinkedHashMap<String, TreeMap<Integer,Float>>();
		for (int i = 0; i < rawbed.size(); i++) {
			SimpleBEDFeature temp=rawbed.get(i);
			if(!indexMap.containsKey(temp.getChr()))
			{
				indexMap.put(temp.getChr(), new IntervalTree());
				valMap.put(temp.getChr(), new TreeMap<Integer, Float>());
			}
			indexMap.get(temp.getChr()).insert(new Interval(temp.getStart(), temp.getEnd(),null));
			int midpos=(temp.getStart()+temp.getEnd())/2;
			valMap.get(temp.getChr()).put(midpos, temp.getScore());
		}
		
	}
	
	public BEDIntervalIndex(List<SimpleBEDFeature> rawbed) {
		indexMap=new LinkedHashMap<String, IntervalTree>();
		valMap=new LinkedHashMap<String, TreeMap<Integer,Float>>();
		for (int i = 0; i < rawbed.size(); i++) {
			SimpleBEDFeature temp=rawbed.get(i);
			if(!indexMap.containsKey(temp.getChr()))
			{
				indexMap.put(temp.getChr(), new IntervalTree());
				valMap.put(temp.getChr(), new TreeMap<Integer, Float>());
			}
			indexMap.get(temp.getChr()).insert(new Interval(temp.getStart(), temp.getEnd(),null));
			int midpos=(temp.getStart()+temp.getEnd())/2;
			valMap.get(temp.getChr()).put(midpos, temp.getScore());
		}
		
	}
	
	public List<SimpleBEDFeature> getBlocks(String chr, int start, int end)
	{
		BEDCodec coder=new BEDCodec();
		List<Interval> iv = indexMap.get(chr).findOverlapping(new Interval(start, end));
		List<SimpleBEDFeature> ret=new ArrayList<SimpleBEDFeature>(iv.size());
		for (int i = 0; i < iv.size(); i++) {			
			SimpleBEDFeature temp = interval2BEDFeature(iv.get(i), chr);
			temp.setScore(valMap.get(chr).get((temp.getStart()+temp.getEnd())/2));
			ret.add(temp);
		}
		
		return ret;
		
	}
	
	public int overlapCount(String chr, int start, int end)
	{
		BEDCodec coder=new BEDCodec();
		List<Interval> iv = indexMap.get(chr).findOverlapping(new Interval(start, end));
		return iv.size();
		
	}
	
	public float overlapSumValue(String chr, int start, int end)
	{
		float sum=0;
		List<SimpleBEDFeature> list = getBlocks(chr,start,end);
		for (SimpleBEDFeature simpleBEDFeature : list) {
			sum+=simpleBEDFeature.getScore();
		}
		
		return sum;
		
	}
	
	static SimpleBEDFeature interval2BEDFeature(Interval inv, String Chr)
	{
		String[] comps=inv.toString().split(",");
		int start=Integer.parseInt(comps[0].substring(9));
		int end=Integer.parseInt(comps[1].substring(1, comps[1].length()-1));
		return new SimpleBEDFeature(start, end, Chr);
	}
	
	
}
