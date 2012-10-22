
public class SynonymCheck {

	static boolean isSynonym(TrackRecord t1, TrackRecord t2)
	{
		if(t1.ExperimentId==t2.ExperimentId)
			return true;
		if(t1.ExperimentType.contains("Cage")&&t2.ExperimentType.contains("Cage"))
			return true;
		return false;
	}
}
