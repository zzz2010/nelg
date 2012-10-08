import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.broad.tribble.bed.BEDFeature;
import org.junit.Before;
import org.junit.Test;


public class TestFileStorage {
FileStorageAdapter db;
	@Before
	public void setUp() throws Exception {
		db=new FileStorageAdapter("./data");
	}
	
	public TestFileStorage() {
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
	public void testFileStorageAdapter() {
		
		assertEquals(db.DataBase.size(),5);
	}

	@Test
	public void testParseLine() {
		TrackRecord temp=db.parseLine("wgEncodeRikenCageK562CellPapPlusClusters	hg19	K562	PapPlusClusters	Cage	wgEncodeRiken	signal");
		assertEquals(temp.ReplicateSuffix.size(),2);
		assertEquals(temp.Assembly,"hg19");
		assertEquals(temp.Cell_Line,"K562");
		assertFalse(temp.hasPeak);
	}

	@Test
	public void testGetCellLineName() {
		assertEquals(db.getCellLineName("hg19").size(),1);
		assertEquals(db.getCellLineName("hg19").get(0), "K562");
	}

	@Test
	public void testGetTrackId_inCellLine() {
		assertEquals(db.getTrackId_inCellLine("hg19", "K562").size(),5);
		assertEquals(db.getTrackId_inCellLine("hg18", "K562").size(),0);
		assertEquals(db.getTrackId_inCellLine("hg19", "K").size(),0);
	}

	@Test
	public void testGetTrackById() {
		TrackRecord temp=db.getTrackById("wgEncodeRikenCageK562CellPapPlusClusters");
		assertEquals(temp.ReplicateSuffix.size(),2);
		assertEquals(temp.Assembly,"hg19");
		assertEquals(temp.Cell_Line,"K562");
		
	}
	
	@Test
	public void testgetPeakData()
	{
		TrackRecord temp=db.getTrackById("wgEncodeBroadHistoneK562Ctcf");
		List<BEDFeature> d=db.getPeakData(temp);
		assertEquals(d.get(0).getChr(),"chr22");
		assertEquals(d.get(0).getEnd(),16166753);
		assertEquals(d.get(0).getScore(),649,0.000001);
		
	}

	@Test
	public void testgetSignalContigRegion()
	{
		TrackRecord temp=db.getTrackById("wgEncodeRikenCageK562CellPapPlusClusters");
		List<BEDFeature> regions = db.getSignalContigRegion(temp);
		assertTrue(regions.size()>1000);
	}
	
	@Test
	public void testOverlapBinSignal()
	{
		TrackRecord temp=db.getTrackById("wgEncodeRikenCageK562CellPapPlusClusters");
		List<BEDFeature> regions = db.getSignalContigRegion(temp);
		List<List<Float>> BinArray = db.OverlapBinSignal(temp, regions, 100);
		assertEquals(regions.size(), BinArray.size());
		assertEquals(100, BinArray.get(0).size());
		Float a=Collections.max(BinArray.get(0));
		assertTrue(a>0 );
	}
}
