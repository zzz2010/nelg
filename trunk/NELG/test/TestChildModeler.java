import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;

import weka.core.Instances;


public class TestChildModeler {
ClassificationJob testedJob_C;
ClassificationJob testedJob_R;
ChildModeler modeler;
	public TestChildModeler() {
	super();
	   FileInputStream fileIn;

	   modeler=new ChildModeler();
	   ChildModeler.logger.setLevel(Level.DEBUG);
		  ConsoleAppender appender =new ConsoleAppender(new PatternLayout());
		  ChildModeler.logger.addAppender(appender); 
	   
	try {
		fileIn = new FileInputStream("wgEncodeRikenCageK562ChromatinTotalMinusSignal_ValThere.cj");
		 ObjectInputStream in = new ObjectInputStream(fileIn);
			testedJob_R=(ClassificationJob)in.readObject();
			
//			fileIn = new FileInputStream("wgEncodeRikenCageK562ChromatinTotalMinusSignal_ValThere.cj");
//			  in = new ObjectInputStream(fileIn);
//				testedJob_C=(ClassificationJob)in.readObject();
			
	
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

	@Test
	public void testDoClassification() {
//		double auc=modeler.doClassification(testedJob_C).AUC;
//		assertTrue(auc>0.6);
	}

	@Test
	public void testDoRegression() {
//		double corr=modeler.doRegression(testedJob_R).Corr;
//		assertTrue(corr>0.2);
		testedJob_R.run();
	}

	@Test
	public void testGetDatasetFromJob() {
		Instances data=modeler.getDatasetFromJob(testedJob_C);
		assertEquals(data.numInstances(), testedJob_C.targetValue.size());
		assertEquals(data.numClasses(), 2);
		assertEquals(data.numAttributes(), testedJob_C.FeatureMatrix.size()+1);
		assertEquals(data.classIndex(), testedJob_C.FeatureMatrix.size());
	}
	
	@Test
	public void showClassificationResult()
	{
		FileInputStream fileIn;
		try {
				fileIn = new FileInputStream("wgEncodeRikenCageK562ChromatinTotalMinusSignal_ValThere_result.cr");
				ObjectInputStream  in = new ObjectInputStream(fileIn);
				  ClassificationResult minusResult=(ClassificationResult)in.readObject();
				Logger logger = Logger.getLogger("aa");
				 logger.debug(minusResult.toString());
				
		
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

}
