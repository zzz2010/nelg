import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.broad.tribble.bed.BEDFeature;
import org.junit.Before;
import org.junit.Test;

import cern.colt.matrix.impl.SparseDoubleMatrix1D;


public class TestPeakCalling {
	FileStorageAdapter db;
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TestPeakCalling.class);
	@Before
	public void setUp() throws Exception {
		db=new FileStorageAdapter("./data");
	}

	@Test
	public void testSimple_peak_detection() {
		TrackRecord temp=db.getTrackById("wgEncodeBroadHistoneK562H3k4me3");
		List<BEDFeature> peaks = SignalTransform.extractPositveSignal(temp);
		List<BEDFeature> queryregions=SignalTransform.fixRegionSize(peaks, 100000);
		
		temp.hasPeak=false;
		List<BEDFeature> peaks2 =SignalTransform.extractPositveSignal(temp);
		
		
//		List<SparseDoubleMatrix1D> SignalOverRegions = temp.overlapBinSignal_fixStepSize(queryregions, 400);
//		//peak calling
//		List<BEDFeature> peaks2=PeakCalling.random_peak_detection(SignalOverRegions, queryregions);
//		
		
		
		logger.info("peaks2 number£º"+peaks2.size());
		Collections.sort(peaks,new BEDPositionComparator());
		Collections.sort(peaks2,new BEDPositionComparator());
		List<BEDFeature> overlaps = SignalTransform.intersectSortedRegions(peaks, peaks2);
		double precision=(double)overlaps.size()/peaks2.size();
		
		logger.info("precision£º"+precision);
		assertTrue(precision>0.5);
	}

}
