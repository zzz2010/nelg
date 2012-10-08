import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.broad.tribble.bed.BEDFeature;
import org.junit.Before;
import org.junit.Test;


public class TestSignalTransform {
	FileStorageAdapter db;
	@Before
	public void setUp() throws Exception {
		db=new FileStorageAdapter("./data");
	}
	
	public TestSignalTransform() {
		super();
		// TODO Auto-generated constructor stub
		try {
			setUp();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testNormalizedSignal() {
		fail("Not yet implemented");
	}

	@Test
	public void testExtracePositveSignal() {
		//signal only
		TrackRecord temp=db.getTrackById("wgEncodeRikenCageK562CellPapPlusClusters");
		ArrayList<BEDFeature> peak=SignalTransform.extractPositveSignal(temp);
		for(BEDFeature p:peak)
		{
			System.out.println(p.getDescription());
		}
		temp=db.getTrackById("wgEncodeBroadHistoneK562Ctcf");
		peak=SignalTransform.extractPositveSignal(temp);
		assertEquals(peak.size(), 10000);
		assertTrue(peak.get(0).getScore()>=peak.get(1).getScore());
	}

	@Test
	public void testOverlapBinSignal() {

		TrackRecord temp=db.getTrackById("wgEncodeRikenCageK562CellPapPlusClusters");
		List<BEDFeature> regions = db.getSignalContigRegion(temp);
		List<List<Float>> BinArray =SignalTransform.OverlapBinSignal(temp, regions, 100);
		assertEquals(regions.size(), BinArray.size());
		assertEquals(100, BinArray.get(0).size());
		Float a=Collections.max(BinArray.get(0));
		assertTrue(a>0 );
	}

	@Test
	public void testExtraceNegativeSignal() {
		TrackRecord temp=db.getTrackById("wgEncodeRikenCageK562CellPapPlusClusters");
		List<BEDFeature> peak=temp.getPeakData();
		List<BEDFeature> peak_bg=SignalTransform.extractNegativeSignal(peak, peak.size());
		assertEquals(peak_bg.size(), peak.size());
	}

	@Test
	public void testBedFeatureToValues() {
		fail("Not yet implemented");
	}

}
