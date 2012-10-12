import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;


import weka.classifiers.Classifier;


public class ClassificationResult implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6883829574479472815L;
	Classifier LearnedModel=null;
	double AUC=-1;
	double Corr=-1;
	boolean isRegression=false;
	List<Pair<String,Integer>> FeatureIdBin=null;
	String JobTitle="";
	
	
	public void toFile()
	 {
	        try {
	        	 FileOutputStream fileOut =
	    		         new FileOutputStream(JobTitle+"_result.cr");
	    		         ObjectOutputStream out =
	    		                            new ObjectOutputStream(fileOut);
				out.writeObject(this);
		         out.close();
		          fileOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	 }
	
}
