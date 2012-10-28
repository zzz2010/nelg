import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.broad.tribble.bed.BEDCodec;
import org.broad.tribble.bed.BEDFeature;
import org.broad.tribble.index.interval.Interval;
import org.broad.tribble.index.interval.IntervalTree;


public class BEDIntervalIndex {

	LinkedHashMap<String, IntervalTree> indexMap;

	public BEDIntervalIndex(String bedfile) {
		List<BEDFeature> rawbed=FileStorageAdapter.getBEDData(bedfile);
		indexMap=new LinkedHashMap<String, IntervalTree>();
		for (int i = 0; i < rawbed.size(); i++) {
			BEDFeature temp=rawbed.get(i);
			if(!indexMap.containsKey(temp.getChr()))
			{
				indexMap.put(temp.getChr(), new IntervalTree());
			}
			indexMap.get(temp.getChr()).insert(new Interval(temp.getStart(), temp.getEnd(),null));
		}
		
	}
	
	public List<BEDFeature> getBlocks(String chr, int start, int end)
	{
		BEDCodec coder=new BEDCodec();
		List<Interval> iv = indexMap.get(chr).findOverlapping(new Interval(start, end));
		List<BEDFeature> ret=new ArrayList<BEDFeature>(iv.size());
		for (int i = 0; i < iv.size(); i++) {			
			ret.add(interval2BEDFeature(iv.get(i), chr));
		}
		
		return ret;
		
	}
	
	public int overlapCount(String chr, int start, int end)
	{
		BEDCodec coder=new BEDCodec();
		List<Interval> iv = indexMap.get(chr).findOverlapping(new Interval(start, end));
		return iv.size();
		
	}
	
	static SimpleBEDFeature interval2BEDFeature(Interval inv, String Chr)
	{
		String[] comps=inv.toString().split(",");
		int start=Integer.parseInt(comps[0].substring(9));
		int end=Integer.parseInt(comps[1].substring(1, comps[1].length()-1));
		return new SimpleBEDFeature(start, end, Chr);
	}
	
	
}
