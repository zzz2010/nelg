import static org.junit.Assert.*;

import org.junit.Test;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;


public class commonTest {

	@Test
	public void testRowNormalizeMatrix() {
		DoubleMatrix2D matrix1 = DoubleFactory2D.dense.random(3, 3);
		DoubleMatrix2D matrix2 = common.RowNormalizeMatrix(matrix1);
		assertEquals(1.0, matrix2.viewRow(0).zSum(),0.0000001);
	}

}
