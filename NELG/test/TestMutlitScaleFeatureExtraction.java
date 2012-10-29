import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class TestMutlitScaleFeatureExtraction {
	FileStorageAdapter db;
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TestMutlitScaleFeatureExtraction.class);
	@Before
	public void setUp() throws Exception {
		db=new FileStorageAdapter("./data");
	}

	@Test
	public void testExtractSignalFeature() {
		TrackRecord temp=db.getTrackById("wgEncodeRikenCageK562CellPapPlusClusters");
		SimpleBEDFeature testquery=new SimpleBEDFeature(24235047, 24235946, "chr22");
		List<SimpleBEDFeature> querys=new ArrayList<SimpleBEDFeature>();
		for (int i = 0; i < 100; i++) {
			querys.add(testquery);
		}
		
		
		MultiScaleFeatureExtractor extractor=new MultiScaleFeatureExtractor(8);
		SparseDoubleMatrix2D output = extractor.extractSignalFeature_slow(temp, querys);
		logger.debug(output);
	}

	@Test
	public void testExtractSignalFeature_old() {
		TrackRecord temp=db.getTrackById("wgEncodeRikenCageK562CellPapPlusClusters");
		SimpleBEDFeature testquery=new SimpleBEDFeature(24235047, 24235946, "chr22");
		List<SimpleBEDFeature> querys=new ArrayList<SimpleBEDFeature>();
		for (int i = 0; i < 100; i++) {
			querys.add(testquery);
		}
		
		MultiScaleFeatureExtractor extractor=new MultiScaleFeatureExtractor(8);
		SparseDoubleMatrix2D output = extractor.extractSignalFeature(temp, querys);
		logger.debug(output);
	}

}
