import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;



import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.log4j.Logger;

import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.ClassifierSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.attributeSelection.LinearForwardSelection;
import weka.attributeSelection.PrincipalComponents;
import weka.attributeSelection.RaceSearch;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.functions.LeastMedSq;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.PaceRegression;
import weka.classifiers.functions.RBFNetwork;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.functions.VotedPerceptron;
import weka.classifiers.lazy.KStar;
import weka.classifiers.lazy.LWL;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.rules.JRip;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.J48graft;
import weka.classifiers.trees.M5P;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.NumericCleaner;





public class ChildModeler {
	static Logger logger = Logger.getLogger(ChildModeler.class);
	
	
	public ClassificationResult doClassification(ClassificationJob job)
	{
		double auc=0;
		ArrayList<Integer> selecedAttributes=new ArrayList<Integer>();
		Instances data=getDatasetFromJob(job);
		 System.out.println("ClassificationJob:"+job.JobTitle);
		NumericCleaner numCleaner=new NumericCleaner();
		try {
			numCleaner.setInputFormat(data);
			numCleaner.setDecimals(6);
			data = Filter.useFilter(data, numCleaner);
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		Instances data2 =null;//filtered dataset
		if(common.filterFeature&&(data.numAttributes()>Math.log(job.targetValue.size())/Math.log(2)+1))
		{
			 System.out.println("Feature Selection, number of input features:"+data.numAttributes());
		HashMap<String,Integer> FeatureNameMap=new HashMap<String, Integer>();
		for (int i = 0; i < data.numAttributes(); i++) {
			FeatureNameMap.put(data.attribute(i).name(),i);
		}
		//feature selection
		weka.filters.supervised.attribute.AttributeSelection filter = new weka.filters.supervised.attribute.AttributeSelection();
//		ClassifierSubsetEval eval = new ClassifierSubsetEval();
//		eval.setClassifier(new J48());
//		eval.setUseTraining(true);
//		LinearForwardSelection search = new LinearForwardSelection();
	    
		CfsSubsetEval eval=new CfsSubsetEval();
		GreedyStepwise search =new GreedyStepwise();
		search.setSearchBackwards(true);
//	    try {
//			search.setNumUsedAttributes((int) (Math.log(job.targetValue.size())/Math.log(2)+1));
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	    filter.setEvaluator(eval);
	    filter.setSearch(search);
	    try {
			filter.setInputFormat(data);
			 data2 = Filter.useFilter(data, filter);
			for (int i = 0; i < data2.numAttributes()-1; i++) {
				selecedAttributes.add(FeatureNameMap.get(data2.attribute(i).name()));
				FeatureNameMap.remove(data2.attribute(i).name());
			} 
			
			//logging
				logger.debug("filter features:"+FeatureNameMap.keySet());
	
			 
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		}
		else
		{
			 data2 = data;
			for (int i = 0; i < data.numAttributes()-1; i++) {
				selecedAttributes.add(i);
			}
		}
	  
		 System.out.println("Number of input features:"+data2.numAttributes());
		//classifier selection
		Classifier bestModeler=null;
		double bestscore=-1;
		ArrayList<Classifier> modelerSet=new ArrayList<Classifier>();
		modelerSet.add(new J48graft());
		if(!common.printMode)
		{
		if(data2.numAttributes()>2&&data2.numAttributes()<10)
			modelerSet.add(new KStar());
		if(data2.numAttributes()>3)
			modelerSet.add(new Logistic());
		if(data2.numAttributes()>4)
			modelerSet.add(new RandomForest());
		if(data2.numAttributes()>5)
			modelerSet.add(new SMO());
		if(data2.numAttributes()>6)
			modelerSet.add(new AdaBoostM1());
		}
			
		
		int crossValNum=5;

		for (int i = 0; i < modelerSet.size(); i++) {
			
		
		Classifier modeler = modelerSet.get(i);
		try {
			Evaluation eval1 = new Evaluation(data2);
			if(common.printMode)
				eval1.evaluateModel(modeler, data2);
			else
				eval1.crossValidateModel(modeler, data2, crossValNum, new Random(1));
			auc=eval1.areaUnderROC(1);
			logger.debug(modeler.getClass().getName()+" auc:"+auc);
			 System.out.println(modeler.getClass().getName()+" auc:"+auc);
			if(bestscore<auc)
			{
				bestscore=auc;
				bestModeler=modeler;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		}
			
		//get final classifier
		try {
			bestModeler.buildClassifier(data2);
			Evaluation eval = new Evaluation(data2);
			eval.evaluateModel(bestModeler, data2);
			System.out.println(eval.toSummaryString("\nResults\n======\n", false));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ClassificationResult result=new ClassificationResult();
		result.AUC=bestscore;
		result.LearnedModel=bestModeler;
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
		
		NumericCleaner numCleaner=new NumericCleaner();
		try {
			numCleaner.setDecimals(6);
			numCleaner.setInputFormat(data);
			data = Filter.useFilter(data, numCleaner);
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		ArrayList<Integer> selecedAttributes=new ArrayList<Integer>();
		ArrayList<String> finalFeatureSel=new ArrayList<String>();
		Instances data2 =null;//filtered dataset
		if(common.filterFeature&&(data.numAttributes()>Math.log(job.targetValue.size())/Math.log(2)+1))
		{
		
		HashMap<String,Integer> FeatureNameMap=new HashMap<String, Integer>();
		for (int i = 0; i < data.numAttributes()-1; i++) {
			FeatureNameMap.put(data.attribute(i).name(),i);
		}
		//feature selection
		weka.filters.supervised.attribute.AttributeSelection filter = new weka.filters.supervised.attribute.AttributeSelection();
		ClassifierSubsetEval eval = new ClassifierSubsetEval();
		eval.setClassifier(new LinearRegression());
		LinearForwardSelection search = new LinearForwardSelection();
//	    search.setSearchBackwards(true);
	    try {
			search.setNumUsedAttributes((int) (Math.log(job.targetValue.size())/Math.log(2)+1));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    filter.setEvaluator(eval);
	    filter.setSearch(search);
	    
	    try {
			filter.setInputFormat(data);
			 data2 = Filter.useFilter(data, filter);
			for (int i = 0; i < data2.numAttributes()-1; i++) {
				selecedAttributes.add(FeatureNameMap.get(data2.attribute(i).name()));
				finalFeatureSel.add(data2.attribute(i).name());
				FeatureNameMap.remove(data2.attribute(i).name());
			} 
			
			//logging
			logger.debug("filter features:"+FeatureNameMap.keySet());
				logger.info("final features:"+finalFeatureSel);
//				PrincipalComponents pca=new PrincipalComponents();
//				pca.buildEvaluator(data);
//				pca.setMaximumAttributeNames((int) (Math.log(job.targetValue.size())/Math.log(2)+1));
//			 data2=pca.transformedData(data);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		}
		else
		{
			 data2 = data;
			for (int i = 0; i < data.numAttributes()-1; i++) {
				selecedAttributes.add(i);
			}
		}
		
		
		
		
		
		//classifier selection
		Classifier bestModeler=null;
		double bestscore=-1;
		ArrayList<Classifier> modelerSet=new ArrayList<Classifier>();
		modelerSet.add(new M5P());
		if(!common.printMode)
		{
	   if(data2.numAttributes()>2)
			modelerSet.add(new LeastMedSq());
	   if(data2.numAttributes()>3)
			modelerSet.add(new LinearRegression());
		if(data2.numAttributes()>4)
			modelerSet.add(new RBFNetwork());//GaussianProcesses
		if(data2.numAttributes()>5)
			modelerSet.add(new PaceRegression());
//		if(data2.numAttributes()>6)
//			modelerSet.add(new SMOreg());
		}
			
		
		int crossValNum=5;

		for (int i = 0; i < modelerSet.size(); i++) {
			

		Classifier modeler = modelerSet.get(i);
		try {
			Evaluation eval = new Evaluation(data2);
			if(common.printMode)
				eval.evaluateModel(modeler, data2);
			else
				eval.crossValidateModel(modeler, data2, crossValNum, new Random(1));
			corr=eval.correlationCoefficient();
			logger.debug(modeler.getClass().getName()+" corr:"+corr);
			if(bestscore<corr)
			{
				bestscore=corr;
				bestModeler=modeler;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		
		}
		//get final classifier
		try {
			bestModeler.buildClassifier(data2);
			for (int i = 0; i < 10; i++) {
				System.out.println(bestModeler.classifyInstance(data2.instance(i)));
			}
			
			Evaluation eval = new Evaluation(data2);
			eval.evaluateModel(bestModeler, data2);
			System.out.println(eval.toSummaryString("\nResults\n======\n", false));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ClassificationResult result=new ClassificationResult();
		result.Corr=bestscore;
		result.LearnedModel=bestModeler;
		result.isRegression=true;
		result.JobTitle=job.JobTitle;
		result.FeatureIdBin=new ArrayList<Pair<String,Integer>>();
		for (int i = 0; i < selecedAttributes.size(); i++) {
		FeatureSignal temp=	job.FeatureMatrix.get(selecedAttributes.get(i));
		result.FeatureIdBin.add(new Pair<String, Integer>(temp.FeatureId,temp.binId));
		}
		return result;
		
	}
	
	
	public static Instances getDatasetFromJob(ClassificationJob job)
	{
		int featureNum=job.FeatureMatrix.size();
		FastVector attrList=new FastVector(featureNum+1);
		for (int i = 0; i < featureNum; i++) {
			Attribute temp=new Attribute( job.FeatureMatrix.get(i).FeatureId+"|"+job.FeatureMatrix.get(i).binId);
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
		 if(job.Regression)
		 {
			 job.targetValue=SignalTransform.normalizeSignal(job.targetValue);
			 for (int j = 0; j < featureNum; j++) {
				 job.FeatureMatrix.get(j).featureValue=SignalTransform.normalizeSignal(job.FeatureMatrix.get(j).featureValue);
			 }
		 }
		
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
		  Normalize m_normalize = new Normalize();
		  try {
			m_normalize.setInputFormat(jobdata);
			 jobdata = Filter.useFilter(jobdata, m_normalize);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
		return jobdata;
		
	}
}
