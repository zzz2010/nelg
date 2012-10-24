import static org.junit.Assert.*;

import java.util.ArrayList;
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
		TrackRecord temp=db.getTrackById("wgEncodeRikenCageK562CytosolPapPlusSignal");
		TrackRecord conrol=db.getTrackById("wgEncodeBroadHistoneK562Control");
		List<BEDFeature> peaks = FileStorageAdapter.getBEDData("./TSS.bed") ;//SignalTransform.extractPositveSignal(temp);
//		List<BEDFeature> queryregions=SignalTransform.fixRegionSize(peaks, 100000,true);
//		
//		temp.hasPeak=false;
//		List<BEDFeature> peaks2 =SignalTransform.extractPositveSignal(temp);
		SimpleBEDFeature testquery=new SimpleBEDFeature(24235047, 24235946, "chr22");
		List<BEDFeature> qr=new ArrayList<BEDFeature>();
		qr.add(testquery);
		List<SparseDoubleMatrix1D> SignalOverRegions = temp.overlapBinSignal_fixStepSize(qr, 200);
//		List<SparseDoubleMatrix1D> SignalOverRegions = temp.overlapBinSignal_fixStepSize(queryregions, 200);
//		List<SparseDoubleMatrix1D> SignalOverRegions_bg = conrol.overlapBinSignal_fixStepSize(queryregions, 400);
		//peak calling
//		List<BEDFeature> peaks2=PeakCalling.simple_peak_detection(SignalOverRegions,SignalOverRegions_bg, queryregions);
		List<BEDFeature> peaks2=temp.getPeakData();//PeakCalling.simple_peak_detection(SignalOverRegions, queryregions);
		peaks2=SignalTransform.sortUnique(peaks2);
		
		
		peaks2=peaks2.subList(0, Math.min(10000,peaks2.size()));
		
		for (int i = 0; i < 10; i++) {
			logger.info(peaks2.get(i).getChr()+":"+peaks2.get(i).getStart()+"-"+peaks2.get(i).getEnd());
		}
		
		logger.info("peaks2 number��"+peaks2.size());
		List<BEDFeature> peaks3 = SignalTransform.fixRegionSize(peaks2, 800,true);
		Collections.sort(peaks,new BEDPositionComparator());
		Collections.sort(peaks3,new BEDPositionComparator());
		List<BEDFeature> overlaps = SignalTransform.intersectSortedRegions(peaks, peaks3);
		double precision=(double)overlaps.size()/peaks3.size();
		SimpleBEDFeature.toFile(peaks2, "cagepeak.bed");
		logger.info("precision��"+precision);
		assertTrue(precision>0.5);
		
	}

}
