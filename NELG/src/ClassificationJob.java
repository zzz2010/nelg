import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;


public class ClassificationJob implements Runnable  , Serializable {

	/**
	 * 
	 */
	static Logger logger = Logger.getLogger(ClassificationJob.class);
	private static final long serialVersionUID = 1L;
	List<FeatureSignal> FeatureMatrix;
	String JobTitle;
	DoubleMatrix1D targetValue;
	Boolean Regression=false;
	public ClassificationJob(List<FeatureSignal> featureMatrix,
			String jobTitle, DoubleMatrix1D targetValue) {
		
		FeatureMatrix = featureMatrix;
		JobTitle = jobTitle;
		this.targetValue = targetValue;
	}
	
	 public void run() {
		
		 toFile();
		 ChildModeler modeler=new ChildModeler();
		 ClassificationResult result=null;
		 if(Regression)
			 result=modeler.doRegression(this);
		 else
			 result=modeler.doClassification(this);
		 
		 if(result!=null)
		 {
			 result.toFile();
			 if(result.isRegression)
			 {
				 if(result.Corr>0.8)
				 {

					 logger.info(result.toString());
				 }
			 }
			 else
			 {
				 if(result.AUC>0.8)
				 {
					 logger.info(result.toString());
				 }
			 }
		 }
	    }
	 
	 public void toFile()
	 {
	        try {
	        	 FileOutputStream fileOut =
	    		         new FileOutputStream(JobTitle+".cj");
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
