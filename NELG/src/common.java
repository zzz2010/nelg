import org.jppf.task.storage.MemoryMapDataProvider;

import cern.colt.matrix.DoubleMatrix2D;


public class common {

	static String outputDir="./result/";
	static String tempDir="./cache/";
	static String dataDir="./data/";
	static int SignalRange=10000;
	static int MaxSampleNum=10000;
	static int MinimumPeakNum=50;
	static String predictTarget_debug="";
	static String selectFeature_debug="";
	static boolean onlyBestBin=false;
	static boolean printMode=false;
	static double AUC_cutoff=0.6;
	static double Corr_cutoff=0.2;
	static int ClusterNum=10;
	static int threadNum=12;
	static boolean NFSmode=false;
	static boolean Localmode=false;
	static boolean SynonymCheck=true;
	static int topNfeatures=100;
	static boolean filterFeature=true;
	static boolean normalizedSignalSum=false;
	
	
	static DoubleMatrix2D RowNormalizeMatrix(DoubleMatrix2D matrix)
	{
		double smallnum=1.0/SignalRange;
		DoubleMatrix2D matrix2=matrix.copy();
		for (int i = 0; i < matrix.rows(); i++) {
			double sum=matrix.viewRow(i).zSum()+matrix2.columns()*smallnum;
			for (int j = 0; j < matrix2.columns(); j++) {
				matrix2.setQuick(i, j, (matrix.getQuick(i, j)+smallnum)/sum);
			}
		}
		return matrix2;
	}
	
	static void loadDataProvider(MemoryMapDataProvider dataProvider)
	{
		common.NFSmode=(Boolean) dataProvider.getValue("NFSmode");
		common.tempDir=(String) dataProvider.getValue("tempDir");
		common.dataDir=(String) dataProvider.getValue("dataDir");
		common.outputDir=(String) dataProvider.getValue("outputDir");
		common.onlyBestBin= (Boolean) dataProvider.getValue("onlyBestBin");
		common.predictTarget_debug=(String) dataProvider.getValue("predictTarget_debug");
		common.selectFeature_debug= (String) dataProvider.getValue("selectFeature_debug");
		common.SignalRange= (Integer) dataProvider.getValue("SignalRange");
		common.MinimumPeakNum= (Integer) dataProvider.getValue("MinimumPeakNum");
		common.AUC_cutoff=  (Double) dataProvider.getValue("AUC_cutoff");
		common.Corr_cutoff=  (Double) dataProvider.getValue("Corr_cutoff");
		common.threadNum= (Integer) dataProvider.getValue("threadNum");
	}
	
	static MemoryMapDataProvider getDataProvider()
	{
		MemoryMapDataProvider config=new MemoryMapDataProvider();
		config.setValue("outputDir", outputDir);
		config.setValue("tempDir", tempDir);
		config.setValue("dataDir", dataDir);
		config.setValue("SignalRange", SignalRange);
		config.setValue("MinimumPeakNum", MinimumPeakNum);
		config.setValue("predictTarget_debug", predictTarget_debug);
		config.setValue("selectFeature_debug", selectFeature_debug);
		config.setValue("onlyBestBin", onlyBestBin);
		config.setValue("AUC_cutoff", AUC_cutoff);
		config.setValue("Corr_cutoff", Corr_cutoff);
		config.setValue("threadNum", threadNum);
		config.setValue("NFSmode", NFSmode);

		
		return config;
	}
	
}
