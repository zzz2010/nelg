import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

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
	try {
		fileIn = new FileInputStream("HistoneH3k9ac_ValThere.ser");
		 ObjectInputStream in = new ObjectInputStream(fileIn);
			testedJob_R=(ClassificationJob)in.readObject();
			
			fileIn = new FileInputStream("HistoneH3k9ac_IsThere.ser");
			  in = new ObjectInputStream(fileIn);
				testedJob_C=(ClassificationJob)in.readObject();
			
	
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
		double auc=modeler.doClassification(testedJob_C);
		assertTrue(auc>0.6);
	}

	@Test
	public void testDoRegression() {
		double corr=modeler.doRegression(testedJob_R);
		assertTrue(corr>0.2);
	}

	@Test
	public void testGetDatasetFromJob() {
		Instances data=modeler.getDatasetFromJob(testedJob_C);
		assertEquals(data.numInstances(), testedJob_C.targetValue.size());
		assertEquals(data.numClasses(), 2);
		assertEquals(data.numAttributes(), testedJob_C.FeatureMatrix.size()+1);
		assertEquals(data.classIndex(), testedJob_C.FeatureMatrix.size());
	}

}
