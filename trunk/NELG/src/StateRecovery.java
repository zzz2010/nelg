import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.broad.igv.bbfile.BedFeature;
import org.broad.tribble.bed.BEDFeature;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class StateRecovery {

	//if not finish return null
	static ClassificationResult CheckClassificationJob(String JobId)
	{
		File f1=new File(common.outputDir+JobId+"_result.cr");
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
	
	static void saveCache_BEDFeatureList(List<BEDFeature> obj, String key)
	{
		SimpleBEDFeature.toFile(obj, common.tempDir+key);
	}
	static List<BEDFeature> loadCache_BEDFeatureList(String key)
	{
		return FileStorageAdapter.getBEDData(key);
	}
	
	static void saveCache_SparseDoubleMatrix2D(SparseDoubleMatrix2D obj, String key)
	{
        try {
       	 FileOutputStream fileOut =
   		         new FileOutputStream(common.tempDir+key);
   		         ObjectOutputStream out =
   		                            new ObjectOutputStream(fileOut);
			out.writeObject(obj);
	         out.close();
	          fileOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static SparseDoubleMatrix2D loadCache_SparseDoubleMatrix2D(String key)
	{
		File f1=new File(common.tempDir+key);
		if(f1.exists())
		{
			 FileInputStream fileIn;
			 try {
				fileIn = new FileInputStream(f1.getAbsolutePath());
				 ObjectInputStream in = new ObjectInputStream(fileIn);
				 SparseDoubleMatrix2D temp=(SparseDoubleMatrix2D)in.readObject();
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
		File f1=new File(common.outputDir+target_signal.FilePrefix+".fsj");
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
