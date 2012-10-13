import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;


public class TestStateRecovery {
	FileStorageAdapter db;
	@Before
	public void setUp() throws Exception {
		db=new FileStorageAdapter("./data");
	}

	@Test
	public void testCheckClassificationJob() {
		TrackRecord temp=db.getTrackById("wgEncodeBroadHistoneK562H3k4me3");
		ClassificationResult IsThereJob2=StateRecovery.CheckClassificationJob(temp.FilePrefix+"_IsThere");
		assertNotNull(IsThereJob2);
	}

	@Test
	public void testCheckFeatureSelectionJob() {
		TrackRecord temp=db.getTrackById("wgEncodeBroadHistoneK562H3k4me3");
		FeatureSelectionJob FSJob2=StateRecovery.CheckFeatureSelectionJob(temp);
		assertNotNull(FSJob2);
	}

}
