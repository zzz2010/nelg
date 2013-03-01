import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
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
		 HashMap<String,String> visited=new  HashMap<String,String>();
		 for (int i = 0; i < FeatureIdBin.size(); i++) {
			 
			 if(!visited.containsKey(FeatureIdBin.get(i).key))
			 {
				 visited.put(FeatureIdBin.get(i).key, FeatureIdBin.get(i).value.toString());
			 }
			 else
			 {
				 String binstr=visited.get(FeatureIdBin.get(i).key)+","+FeatureIdBin.get(i).value;
				 
				 visited.put(FeatureIdBin.get(i).key,binstr);
			 }
		}
		 
		 for (String feat:visited.keySet()) {
			 featStr+="\t"+feat+"|"+visited.get(feat);
		}
		 
		 String outstr=(JobTitle+" can be predicted by"+LearnedModel.getClass().getName()+" with "+featStr);
	
	return outstr;
	}


	public void toFile()
	 {
	        try {
	        	 FileOutputStream fileOut =
	    		         new FileOutputStream(common.outputDir+JobTitle+"_result.cr");
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
