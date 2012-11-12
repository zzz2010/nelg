import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.log4j.Logger;
import org.jppf.client.JPPFClient;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class StateRecovery {
	static Logger logger = Logger.getLogger(StateRecovery.class);
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
				 fileIn.close();
				 return temp;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				
			} 
		
		}
		return null;
	}
	
	static ClassificationJob LoadClassificationJob(String JobId)
	{
		File f1=new File(common.tempDir+JobId+".cj");
		if(f1.exists())
		{
			 FileInputStream fileIn;
			 try {
				fileIn = new FileInputStream(f1.getAbsolutePath());
				 ObjectInputStream in = new ObjectInputStream(fileIn);
				 ClassificationJob temp=(ClassificationJob)in.readObject();
				 fileIn.close();
				 return temp;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				
			} 
		
		}
		return null;
	}
	
	static String key2Path(String key)
	{
		String path=key;
		File subdir=new File(common.tempDir+path);
		if(!subdir.getParentFile().exists())
		{
			logger.debug("mkdir: "+subdir.getAbsolutePath());
			subdir.getParentFile().mkdirs();
		}
		
		return path;
	}
	
	static void saveCache_BEDFeatureList(List<SimpleBEDFeature> obj, String key)
	{
		key=key2Path(key);
		SimpleBEDFeature.toFile(obj, common.tempDir+key);
	}
	static List<SimpleBEDFeature> loadCache_BEDFeatureList(String key)
	{
		key=key2Path(key);
		return FileStorageAdapter.getBEDData(key);
	}
	
	static void saveCache_SparseDoubleMatrix2D(SparseDoubleMatrix2D obj, String key)
	{
		key=key2Path(key);
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
		key=key2Path(key);
		File f1=new File(common.tempDir+key);
		if(f1.exists())
		{
			 FileInputStream fileIn;
			 try {
				fileIn = new FileInputStream(f1.getAbsolutePath());
				 ObjectInputStream in = new ObjectInputStream(fileIn);
				 SparseDoubleMatrix2D temp=(SparseDoubleMatrix2D)in.readObject();
				 fileIn.close();
				 return temp;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				
			} 
		
		}
		return null;
	}
	
	
	//if not finish return null
	static FeatureSelectionJob CheckFeatureSelectionJob(TrackRecord target_signal )
	{
		File f1=new File(common.tempDir+target_signal.FilePrefix+".fsj");
		if(f1.exists())
		{
			 FileInputStream fileIn;
			 try {
				fileIn = new FileInputStream(f1.getAbsolutePath());
				 ObjectInputStream in = new ObjectInputStream(fileIn);
				 FeatureSelectionSObj temp=(FeatureSelectionSObj)in.readObject();
				 fileIn.close();
				 FeatureSelectionJob fsjob=new FeatureSelectionJob(target_signal, null, null);
				 fsjob.IsThereFeatures=temp.IsThereFeatures;
				 fsjob.target_signal_filtered=temp.target_signal_filtered;
				 fsjob.ValThereFeatures=temp.ValThereFeatures;
//				 fsjob.executor=new JPPFClient();
				 
				 return fsjob;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				
			} 
	
		}
		
		return null;
	}
}
