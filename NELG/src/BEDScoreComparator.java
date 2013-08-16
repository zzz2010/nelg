import java.util.Comparator;

import org.broad.tribble.bed.BEDFeature;


public class BEDScoreComparator implements Comparator<BEDFeature> {

	
	public int compare(BEDFeature o1, BEDFeature o2) {
		// TODO Auto-generated method stub
		//return (int) -(o1.getScore()-o2.getScore());//decrease order
		if(o1.getScore()>o2.getScore())
			return -1;
		else if (o1.getScore()<o2.getScore())
			return 1;
		else
			return 0;
					
	}

}
