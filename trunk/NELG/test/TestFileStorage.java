import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.broad.tribble.bed.BEDFeature;
import org.junit.Before;
import org.junit.Test;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;


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
		TrackRecord temp=db.parseLine("wgEncodeBroadHistoneK562Cbx3sc101004	hg19	K562	Cbx3sc101004	Histone	wgEncodeBroad");
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
		TrackRecord temp=db.getTrackById("wgEncodeRikenCageK562CytosolPapPlusSignal");
//		List<BEDFeature> regions = db.getSignalContigRegion(temp);
		List<BEDFeature> regions=new ArrayList<BEDFeature>();
		regions.add(new SimpleBEDFeature(44084707, 44085107, "chr15"));
		SparseDoubleMatrix2D BinArray = db.overlapBinSignal_fixBinNum(temp, regions, 1);
		assertEquals(regions.size(), BinArray.size());
		assertEquals(100, BinArray.viewColumn(0).size());
		Float a=(float) BinArray.viewColumn(0).zSum();
		assertTrue(a>0 );
	}
}
