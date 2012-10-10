import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;




public class ChildModeler {

	public double doClassification(ClassificationJob job)
	{
		double auc=0;
		Instances data=getDatasetFromJob(job);
		Classifier modeler = new RandomForest();
		try {
			Evaluation eval = new Evaluation(data);
			eval.crossValidateModel(modeler, data, 5, new Random(1));
			auc=eval.areaUnderROC(1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return auc;
	}
	
	public double doRegression(ClassificationJob job)
	{
		double corr=0;
		Instances data=getDatasetFromJob(job);
		Classifier modeler = new LinearRegression();
		try {
			Evaluation eval = new Evaluation(data);
			eval.crossValidateModel(modeler, data, 5, new Random(1));
			corr=eval.correlationCoefficient();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return corr;
	}
	
	
	public static Instances getDatasetFromJob(ClassificationJob job)
	{
		int featureNum=job.FeatureMatrix.size();
		FastVector attrList=new FastVector(featureNum+1);
		for (int i = 0; i < featureNum; i++) {
			Attribute temp=new Attribute( job.FeatureMatrix.get(i).FeatureId);
			attrList.addElement(temp);
		}
		Attribute label;
		if(job.Regression)//numerical
			label=new Attribute("target_value");
		else//nominal
		{
			FastVector values = new FastVector();
			values.addElement("neg");
			values.addElement("pos");
			label=new Attribute("target_value",values);
		}
			
		
		attrList.addElement(label);
		
		Instances jobdata=new Instances(job.JobTitle, attrList, job.targetValue.size());
		
		int instanceNum=job.targetValue.size();
		for (int i = 0; i < instanceNum; i++) {
			 double[] values = new double[featureNum+1] ;
			 for (int j = 0; j < featureNum; j++) {
				 values[j]=job.FeatureMatrix.get(j).featureValue.get(i);
			}	 
			
			 double labelval=0;
			 if(job.Regression)
				 labelval=job.targetValue.get(i);
			 else
			 {
				 if(job.targetValue.get(i)>0)
					 labelval=1;
				 else
					 labelval=0;
			 }
			 values[featureNum]=labelval;
			 Instance instance = new Instance(1, values);
			 jobdata.add(instance);
		}
		jobdata.setClassIndex(featureNum);
		
		return jobdata;
		
	}
}
