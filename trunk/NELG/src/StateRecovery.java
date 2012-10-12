import java.io.File;


public class StateRecovery {

	//if not finish return null
	static ClassificationJob CheckClassificationJob(String JobId)
	{
		File f1=new File(JobId+"_result.cr");
		if(f1.exists())
		{
			return new ClassificationJob(null,JobId , null);
		}
		return null;
	}
	
	//if not finish return null
	static FeatureSelectionJob CheckFeatureSelectionJob(TrackRecord target_signal )
	{
		File f1=new File(target_signal.FilePrefix+"_IsThere.cj");
		File f2=new File(target_signal.FilePrefix+"_ValThere.cj");
		if(f1.exists()&&f2.exists())
		return new FeatureSelectionJob(target_signal, null, null);
		
		return null;
	}
}
