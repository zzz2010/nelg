import java.util.ArrayList;
import java.util.List;


import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class MultiScaleFeatureExtractor implements FeatureExtractor {
/**
	 * 
	 */
	private static final long serialVersionUID = 8219547590510791987L;
	boolean isRowNormalized=false;
int level=8;

	public MultiScaleFeatureExtractor(int level) {
	super();
	this.level = level;
}

	
	public SparseDoubleMatrix2D extractSignalFeature_slow(TrackRecord signaltrack,
			List<SimpleBEDFeature> query) {
		int binNum=(int) (Math.pow(2, level)-1);
		List<SimpleBEDFeature> query2 =new ArrayList<SimpleBEDFeature>(query.size()*level*2);
		for (int i = 0; i < query.size(); i++) {
			int midpoint=(query.get(i).getStart()+query.get(i).getEnd())/2;
			String chrom=query.get(i).getChr();
			for (int j = 0; j < level; j++) {
				int stepsize=(int) (50*Math.pow(2, j));
				int offset=(int) (50*Math.pow(2, j)-50);
				query2.add(new SimpleBEDFeature(midpoint-offset-stepsize,midpoint-offset, chrom));
				query2.add(new SimpleBEDFeature(midpoint+offset,midpoint+offset+stepsize, chrom));
			}
		}
		SparseDoubleMatrix2D feature_BinSignal_raw=SignalTransform.OverlapBinSignal(signaltrack, query2,1);
		SparseDoubleMatrix2D feature_Signal=new SparseDoubleMatrix2D(query.size(), 3*level);
		IntArrayList rowList=new IntArrayList();
		IntArrayList columnList=new IntArrayList();
		DoubleArrayList valueList=new DoubleArrayList();
		feature_BinSignal_raw.getNonZeros(rowList, columnList, valueList);
		for (int i = 0; i < rowList.size(); i++) {
			int row=rowList.getQuick(i);
			int levelid=(row%(level*2))/2;
			int queryId=row/(level*2);
			double left=feature_BinSignal_raw.get(queryId*level*2+levelid*2, 0);
			double right=feature_BinSignal_raw.get(queryId*level*2+levelid*2+1, 0);
			feature_Signal.set(queryId, 3*levelid, left);
			feature_Signal.set(queryId, 3*levelid+1, right);
			feature_Signal.set(queryId, 3*levelid+2, left+right);
		}
		if(isRowNormalized)
			return (SparseDoubleMatrix2D) common.RowNormalizeMatrix(feature_Signal);
		return feature_Signal;
	}
	
	
	public SparseDoubleMatrix2D extractSignalFeature(TrackRecord signaltrack,
			List<SimpleBEDFeature> query) {
		int binNum=(int) (Math.pow(2, level)-1);
		List<SimpleBEDFeature> query2 = SignalTransform.fixRegionSize(query,100*binNum,false);
		SparseDoubleMatrix2D feature_BinSignal_raw=SignalTransform.OverlapBinSignal(signaltrack, query2,binNum*2);
		//50,100,200,400,800,1600,3200,6400
		SparseDoubleMatrix2D feature_Signal=new SparseDoubleMatrix2D(feature_BinSignal_raw.rows(), 3*level);
		int midbinR=binNum;
		int midbinL=binNum-1;
		for (int i = 0; i < level; i++) {
			DoubleMatrix1D left=feature_BinSignal_raw.viewColumn((int) (midbinL-Math.pow(2, i)+1));
			DoubleMatrix1D right=feature_BinSignal_raw.viewColumn((int) (midbinR+Math.pow(2, i)-1));
			for (int j = 0; j < (Math.pow(2, i)-1); j++) {
				left.assign(feature_BinSignal_raw.viewColumn((int) (midbinL-Math.pow(2, i)-j)), cern.jet.math.Functions.plus);
				right.assign(feature_BinSignal_raw.viewColumn((int) (midbinR+Math.pow(2, i)+j)), cern.jet.math.Functions.plus);
			}
			DoubleMatrix1D sum=left.copy();
			sum.assign(right, cern.jet.math.Functions.plus);
			feature_Signal.viewColumn(3*i).assign(left);
			feature_Signal.viewColumn(3*i+1).assign(right);
			feature_Signal.viewColumn(3*i+2).assign(sum);
		}
		if(isRowNormalized)
			return (SparseDoubleMatrix2D) common.RowNormalizeMatrix(feature_Signal);
		return feature_Signal;
	}

}
