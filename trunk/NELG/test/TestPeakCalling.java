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
		TrackRecord conrol=db.getTrackById("wgEncodeBroadHistoneK562Control");
		List<BEDFeature> peaks = SignalTransform.extractPositveSignal(temp);
		List<BEDFeature> queryregions=SignalTransform.fixRegionSize(peaks, 100000);
//		
//		temp.hasPeak=false;
//		List<BEDFeature> peaks2 =SignalTransform.extractPositveSignal(temp);
		
		
		List<SparseDoubleMatrix1D> SignalOverRegions = temp.overlapBinSignal_fixStepSize(queryregions, 400);
		List<SparseDoubleMatrix1D> SignalOverRegions_bg = conrol.overlapBinSignal_fixStepSize(queryregions, 400);
		//peak calling
		List<BEDFeature> peaks2=PeakCalling.simple_peak_detection(SignalOverRegions,SignalOverRegions_bg, queryregions);
//		List<BEDFeature> peaks2=PeakCalling.simple_peak_detection(SignalOverRegions, queryregions);
		peaks2=SignalTransform.sortUnique(peaks2);
		
		
		peaks2=peaks2.subList(0, 10000);
		
		for (int i = 0; i < 10; i++) {
			logger.info(peaks2.get(i).getChr()+":"+peaks2.get(i).getStart()+"-"+peaks2.get(i).getEnd());
		}
		
		logger.info("peaks2 number£º"+peaks2.size());
		Collections.sort(peaks,new BEDPositionComparator());
		Collections.sort(peaks2,new BEDPositionComparator());
		List<BEDFeature> overlaps = SignalTransform.intersectSortedRegions(peaks, peaks2);
		double precision=(double)overlaps.size()/peaks2.size();
		
		logger.info("precision£º"+precision);
		assertTrue(precision>0.5);
	}

}
