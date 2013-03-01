import java.util.Comparator;

import org.broad.tribble.bed.BEDFeature;


public class BEDPositionComparator implements Comparator<BEDFeature> {

	
	public int compare(BEDFeature o1, BEDFeature o2) {
		// TODO Auto-generated method stub
		if(o1.getChr().equalsIgnoreCase(o2.getChr()))
		{
			if(o1.getStart()>o2.getStart())
				return 1;
			else if(o1.getStart()<o2.getStart())
			{
				return -1;
			}
			else //start equal case
			{
				if(o1.getEnd()<o2.getEnd())
				{
					return -2;
				}
				else
					return 2;
			}
		}
		else //different chromosome case
		{
			if(o1.getChr().compareToIgnoreCase(o2.getChr())>0)
				return 3;
			else
				return -3;
		}
	}

}
