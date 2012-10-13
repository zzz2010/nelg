import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;


public class StateRecovery {

	//if not finish return null
	static ClassificationResult CheckClassificationJob(String JobId)
	{
		File f1=new File(JobId+"_result.cr");
		if(f1.exists())
		{
			 FileInputStream fileIn;
			 try {
				fileIn = new FileInputStream(f1.getAbsolutePath());
				 ObjectInputStream in = new ObjectInputStream(fileIn);
				 ClassificationResult temp=(ClassificationResult)in.readObject();
				 return temp;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
		return null;
	}
	
	//if not finish return null
	static FeatureSelectionJob CheckFeatureSelectionJob(TrackRecord target_signal )
	{
		File f1=new File(target_signal.FilePrefix+".fsj");
		if(f1.exists())
		{
			 FileInputStream fileIn;
			 try {
				fileIn = new FileInputStream(f1.getAbsolutePath());
				 ObjectInputStream in = new ObjectInputStream(fileIn);
				 FeatureSelectionSObj temp=(FeatureSelectionSObj)in.readObject();
				 FeatureSelectionJob fsjob=new FeatureSelectionJob(target_signal, null, null);
				 fsjob.FeatureAUC=temp.FeatureAUC;
				 fsjob.FeatureCorr=temp.FeatureCorr;
				 fsjob.IsThereFeatures=temp.IsThereFeatures;
				 fsjob.target_signal_filtered=temp.target_signal_filtered;
				 fsjob.ValThereFeatures=temp.ValThereFeatures;
				 return fsjob;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
		}
		
		return null;
	}
}
