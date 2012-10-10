import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.math3.util.Pair;

import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;




public class ChildModeler {

	public ClassificationResult doClassification(ClassificationJob job)
	{
		double auc=0;
		ArrayList<Integer> selecedAttributes=new ArrayList<Integer>();
		Instances data=getDatasetFromJob(job);
		Instances data2 = data;//filtered dataset
		HashMap<String,Integer> FeatureNameMap=new HashMap<String, Integer>();
		for (int i = 0; i < data.numAttributes(); i++) {
			FeatureNameMap.put(data.attribute(i).name(),i);
		}
		//feature selection
		weka.filters.supervised.attribute.AttributeSelection filter = new weka.filters.supervised.attribute.AttributeSelection();
	    CfsSubsetEval eval = new CfsSubsetEval();
	    GreedyStepwise search = new GreedyStepwise();
	    search.setSearchBackwards(true);
	    filter.setEvaluator(eval);
	    filter.setSearch(search);
	    try {
			filter.setInputFormat(data);
			 data2 = Filter.useFilter(data, filter);
			for (int i = 0; i < data2.numAttributes(); i++) {
				selecedAttributes.add(FeatureNameMap.get(data2.attribute(i).name()));
			} 
			 
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	  
		
		//classifier selection
		Classifier modeler = new RandomForest();
		try {
			Evaluation eval1 = new Evaluation(data2);
			eval1.crossValidateModel(modeler, data2, 5, new Random(1));
			auc=eval1.areaUnderROC(1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		//get final classifier
		try {
			modeler.buildClassifier(data2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ClassificationResult result=new ClassificationResult();
		result.AUC=auc;
		result.LearnedModel=modeler;
		result.JobTitle=job.JobTitle;
		result.FeatureIdBin=new ArrayList<Pair<String,Integer>>();
		for (int i = 0; i < selecedAttributes.size(); i++) {
		FeatureSignal temp=	job.FeatureMatrix.get(selecedAttributes.get(i));
		result.FeatureIdBin.add(new Pair<String, Integer>(temp.FeatureId,temp.binId));
		}
		return result;
	}
	
	public ClassificationResult doRegression(ClassificationJob job)
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
				
		//get final classifier
		try {
			modeler.buildClassifier(data);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ClassificationResult result=new ClassificationResult();
		result.Corr=corr;
		result.LearnedModel=modeler;
		result.isRegression=true;
		result.JobTitle=job.JobTitle;
		return result;
		
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
