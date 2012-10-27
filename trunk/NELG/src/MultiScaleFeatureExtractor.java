import java.util.List;

import org.broad.tribble.bed.BEDFeature;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;


public class MultiScaleFeatureExtractor implements FeatureExtractor {
/**
	 * 
	 */
	private static final long serialVersionUID = 8219547590510791987L;
int level=8;

	public MultiScaleFeatureExtractor(int level) {
	super();
	this.level = level;
}

	@Override
	public SparseDoubleMatrix2D extractSignalFeature(TrackRecord signaltrack,
			List<BEDFeature> query) {
		int binNum=(int) (Math.pow(2, level)-1);
		List<BEDFeature> query2 = SignalTransform.fixRegionSize(query,100*binNum,false);
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
		
		return feature_Signal;
	}

}
