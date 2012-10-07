import static org.junit.Assert.*;

import java.util.ArrayList;

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
		ArrayList<BEDFeature> peak=SignalTransform.ExtracePositveSignal(temp);
		
		temp=db.getTrackById("wgEncodeBroadHistoneK562Ctcf");
		peak=SignalTransform.ExtracePositveSignal(temp);
		assertEquals(peak.size(), 10000);
		assertTrue(peak.get(0).getScore()>=peak.get(1).getScore());
	}

	@Test
	public void testOverlapBinSignal() {
		fail("Not yet implemented");
	
	}

	@Test
	public void testExtraceNegativeSignal() {
		fail("Not yet implemented");
	}

	@Test
	public void testBedFeatureToValues() {
		fail("Not yet implemented");
	}

}
