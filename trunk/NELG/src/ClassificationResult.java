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
	
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		 String featStr="";
		
		 if(isRegression)
		 {
			 featStr="Regression Corr:"+Corr;	 
		 }
		 else
		 {
			 featStr="Classification AUC:"+AUC;	 
		 }
		 for (int i = 0; i < FeatureIdBin.size(); i++) {
			 featStr+="\t"+FeatureIdBin.get(i).key+"|"+FeatureIdBin.get(i).value;
		}
		 String outstr=(JobTitle+" can be predicted by"+LearnedModel.getClass().getName()+" with "+featStr);
	
	return outstr;
	}


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
