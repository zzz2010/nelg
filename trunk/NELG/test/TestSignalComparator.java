import static org.junit.Assert.*;

import org.junit.Test;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;


public class TestSignalComparator {

	@Test
	public void testGetDiscriminativeCapbaility() {
		DoubleMatrix1D d1 = DoubleFactory1D.dense.random(10000);
		DoubleMatrix1D d2 = DoubleFactory1D.dense.random(10000);
		for (int i = 0; i < d2.size()/2; i++) {
			d2.set(i, -1);
		}
		double aucscore=SignalComparator.getDiscriminativeCapbaility(d1, d2);
		assertTrue(aucscore<0.8);
	}

	@Test
	public void testGetCorrelation() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetPearsonCorrelation() {
		fail("Not yet implemented");
	}

}
