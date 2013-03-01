
public class SynonymCheck {

	static boolean isSynonym(TrackRecord t1, TrackRecord t2)
	{

		if(!common.SynonymCheck)
			return false;
		
		if(t1.ExperimentType.contains("Cage")&&t2.ExperimentType.contains("Cage"))
			return true;
		
		if(firstCapital(t1.ExperimentId).equalsIgnoreCase(firstCapital(t2.ExperimentId)))
			return true;
		
		
		return false;
	}
	
	static String firstCapital(String input)
	{
		if(input!=null&&input.length()>0)
		{
		String out=input.substring(0,1);
		int i=1;
		for ( i = 1; i < input.length(); i++) {
			if(Character.isUpperCase(input.charAt(i)))
				break;
		}
		out+=input.substring(1,i);
		return out;
		}
		return "";
	}
}
