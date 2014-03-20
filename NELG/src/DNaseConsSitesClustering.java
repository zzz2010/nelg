import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.broad.tribble.annotation.Strand;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import cern.colt.function.DoubleDoubleFunction;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.PlusMult;
import weka.clusterers.DBScan;
import weka.clusterers.EM;
import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.SimpleKMeans;
import weka.clusterers.XMeans;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


public class DNaseConsSitesClustering {

	public static ArrayList<Float> convertStrArray(List<String> strlist)
	{
		ArrayList<Float> retlist=new ArrayList<Float>();
		for (String s : strlist) {
			retlist.add(Float.parseFloat(s));
		}
		
		return retlist;
	}
	
	public static void drawSignalAroundClust_headless(String targetName,List<XYSeries> dnaseDataList,List<XYSeries> consDataList,int[] clustCount){
		XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
		renderer1.setShapesVisible(false);
		CombinedRangeXYPlot plot0 = new CombinedRangeXYPlot(new NumberAxis("Signal"));
		
			for (int i=0;i<dnaseDataList.size();i++){
				XYSeriesCollection dataset = new XYSeriesCollection();
				XYSeries posData=dnaseDataList.get(i);
				XYSeries negData=consDataList.get(i);

				dataset.addSeries(posData);
				dataset.addSeries(negData);
				plot0.add(new XYPlot(dataset,  new NumberAxis((i)+": "+clustCount[i]),null, renderer1));
			}
			JFreeChart chart=new JFreeChart(targetName,JFreeChart.DEFAULT_TITLE_FONT, plot0, true);
			//chart.removeLegend();
			ChartPanel chartPanel = new ChartPanel(chart);
	        chartPanel.setSize(new java.awt.Dimension(150*common.ClusterNum, 200)); 
	        try {
	        	String pngfile= common.outputDir+targetName+"_clust"+".png";
				ChartUtilities.saveChartAsPNG(new File(pngfile), chart, 150*common.ClusterNum, 300);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}
//	public static Instances lists2instances(List<ArrayList<Float>> Dnase, List<ArrayList<Float>> Cons)
//	{
//		int num_col=Dnase.get(0).size()+Cons.get(0).size();
//		FastVector attrList=new FastVector(num_col);
//		for (int i = 0; i < num_col; i++) {
//			Attribute temp=new Attribute( String.valueOf(i));
//			attrList.addElement(temp);
//		}
//		Instances insts=new Instances("", attrList, Dnase.size());
//		for (int i = 0; i < Dnase.size(); i++) {
//			ArrayList<Float> tmp = new ArrayList<Float>(num_col);
//			tmp.addAll(Dnase.get(i));
//			tmp.addAll(Cons.get(i));
//			insts.add(new Instance(1, tmp.toArray(new double[])));
//		}
//		return insts;
//		
//	}
	
	public static Instances matrix2instances(DoubleMatrix2D matrix)
	{
		FastVector attrList=new FastVector(matrix.columns());
		for (int i = 0; i < matrix.columns(); i++) {
			Attribute temp=new Attribute( String.valueOf(i));
			attrList.addElement(temp);
		}
		Instances insts=new Instances("", attrList, matrix.rows());
		for (int i = 0; i < matrix.rows(); i++) {
			insts.add(new Instance(1, matrix.viewRow(i).toArray()));
		}
		return insts;
		
	}
	
	public static double getMaxHeight(DenseDoubleMatrix1D vec)
	{
		DoubleMatrix1D vec_sort = vec.viewSorted();
		return vec_sort.getQuick(vec.size()-1)- vec_sort.getQuick(0);
	}
	
	public static ArrayList<Float> MedianNormalization(List<Float> vec)
	{
		ArrayList<Float> vec_sorted = new ArrayList<Float>(vec);
		Collections.sort(vec_sorted);

		Float maxv = vec_sorted.get(vec.size()-1);
		Float minv = vec_sorted.get(0);
		Float pseudoCount=(float) 0.01;
		
		double sum=0;
		for (int i = 0; i < vec_sorted.size(); i++) {
			sum+=vec_sorted.get(i)+pseudoCount-minv;
		}
//		
		Float median = (float) sum;//vec_sorted.get(vec.size()/2)-minv+pseudoCount;
		ArrayList<Float> ret=new ArrayList<Float>();

		float allmax=0;
		float halfmax=0;
		for (int i = 0; i < vec.size(); i++) {
			float t=1;
			if(median>0)
				t=(vec.get(i)-minv+pseudoCount)/median;
			
			ret.add(t);

			allmax=Math.max(allmax, t);
			if(i<vec.size()/2)
				halfmax=Math.max(halfmax, t);
		}
		if(mirror)
		{
			if(halfmax<allmax)
			Collections.reverse(ret);
		}
		return ret;
	}
	public static ArrayList get_Loci_DNase_Cons(String inputfile) throws FileNotFoundException
	{
		ArrayList ret=new ArrayList();
		Scanner s = new Scanner(new File(inputfile));
		ArrayList< ArrayList<Float>> DnaseVec=new ArrayList<ArrayList<Float>>();
		ArrayList< ArrayList<Float>> ConsVec=new ArrayList<ArrayList<Float>>();
		ArrayList<String> lociList=new ArrayList<String>();
		while(s.hasNextLine()) {
	        String line=s.nextLine();
	        ArrayList<String> comps=new ArrayList<String>(Arrays.asList(line.trim().split("\t")));
	        List<String> locus = comps.subList(0, 4);
	        lociList.add(StringUtils.join(locus,'\t'));
	        int breakI=4+(comps.size()-4)/2;
	      //normalize the row by median
	        ArrayList<Float> dnaseVec = MedianNormalization(convertStrArray(comps.subList(4,breakI)));
	        ArrayList<Float> consVec = MedianNormalization(convertStrArray(comps.subList(breakI, comps.size())));
	        if(locus.get(3).equalsIgnoreCase("-")&&!mirror)
	        {
	        	Collections.reverse(dnaseVec);
	        	Collections.reverse(consVec);
	        }
	        DnaseVec.add(dnaseVec);
	        ConsVec.add(consVec);
	    }
		
		ret.add(lociList);
		ret.add(DnaseVec);
		ret.add(ConsVec);
		return ret;
	}
	
	static ArrayList<Float> getMeanVec(ArrayList<ArrayList<Float>> input )
	{
		ArrayList<Float> meanVec=new ArrayList<Float>(input.get(0));

		for (int i = 1; i < input.size(); i++) {
			for (int j = 0; j <meanVec.size(); j++) {
				meanVec.set(j, meanVec.get(j)+input.get(i).get(j));
			}
		}
		for (int j = 0; j <meanVec.size(); j++) {
			double t=meanVec.get(j)/input.size();
			meanVec.set(j,(float) t );

		}

		return meanVec;

	}
	
	
	static ArrayList<NormalDistribution> getBinNormalDists(ArrayList<ArrayList<Float>> input )
	{
		int binNum=input.get(0).size();
		ArrayList<NormalDistribution> normVec=new ArrayList<NormalDistribution>(binNum);

		for (int j = 0; j <binNum; j++) {
			double m=0;
			double m2=0;
		for (int i = 0; i < input.size(); i++) {	
			double t=input.get(i).get(j);
				m+=t;
				m2+=t*t;
			}
				m=m/input.size();
				m2=m2/input.size();
				NormalDistribution normD=new NormalDistribution(m, Math.sqrt(m2-m*m));
				normVec.add(normD);
		}

		
		return normVec;
	}
	
	static double getPValFromBinNormalVec(ArrayList<Float> test,ArrayList<NormalDistribution> model)
	{
		double pval=1;
		double logPVal=0;
		for (int i = 0; i < test.size(); i++) {	
			double t=model.get(i).cumulativeProbability(test.get(i));
			t=Math.min(t, 1-t);
			pval=Math.min(t*2, pval);
//			logPVal+=Math.log(t*2);
		}
//		pval=Math.exp(logPVal/test.size());
		return pval;//*test.size(); //multi-test correction
	}
	
	static NormalDistribution getNullDistribution(ArrayList<Float> mean, ArrayList<ArrayList<Float>> input)
	{
		
		PearsonsCorrelation pcorr = new PearsonsCorrelation();
		double[] meanD=new double[mean.size()];
		for (int i = 0; i < meanD.length; i++) {
			meanD[i]=mean.get(i);
		}
		double sum=0;
		double sumsq=0;
		for (int i = 0; i <input.size(); i++) {
			double[] inputD=new double[mean.size()];
			for (int j = 0; j < meanD.length; j++) {
				inputD[j]=input.get(i).get(j);
			}
			double c=pcorr.correlation(meanD, inputD);
			if(Double.isNaN(c))
				c=1;
			sum+=c;
			sumsq+=c*c;
		}
		double m=sum/input.size();
		NormalDistribution dist=new NormalDistribution(m, Math.sqrt(sumsq/input.size()-m*m));
		
		return dist;		
	}
	static double PearsonCorrelation(ArrayList<Float> p1, ArrayList<Float> p2)
	{
		PearsonsCorrelation pcorr = new PearsonsCorrelation();
		double[] meanD=new double[p1.size()];
		for (int i = 0; i < meanD.length; i++) {
			meanD[i]=p1.get(i);
		}
		double[] meanD2=new double[p2.size()];
		for (int i = 0; i < meanD.length; i++) {
			meanD2[i]=p2.get(i);
		}
		double r=pcorr.correlation(meanD, meanD2);
		if(Double.isNaN(r))
			r=0;
		return r;
	}
	
	static boolean mirror=false;
	static boolean noClustering=false;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Options options = new Options();
		options.addOption("inputfile", true, "first 4 column: chr,st,ed,strand (bed format), + DNase feature + Cons feature");
		options.addOption("ctrlfile", true, "first 4 column: chr,st,ed,strand (bed format), + DNase feature + Cons feature");
		options.addOption("outdir", true, "output directory");
		options.addOption("mirror", false, "combine mirror cluster");
		CommandLineParser parser = new GnuParser();
		String inputFile="";
		String controlFile="";
		CommandLine cmd;
		try {
			String appPath=new File(".").getCanonicalPath()+"/";
			cmd = parser.parse( options, args);
			if(cmd.hasOption("mirror"))
			{
				mirror=true;
			}
			if(cmd.hasOption("inputfile"))
			{
				inputFile=cmd.getOptionValue("inputfile");;
			}
			else
			{
				throw new ParseException("must provide inputFile !");
			}
			if(cmd.hasOption("outdir"))
			{
				common.outputDir=cmd.getOptionValue("outdir");;
			}
			if(cmd.hasOption("ctrlfile"))
			{
				controlFile=cmd.getOptionValue("ctrlfile");;
			}
		}catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "PeakClassifier", options );
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		 try {
			 
///////////////// process the input file to weka instances /////////////////
			 System.out.println("processing input file:"+inputFile);
			 ArrayList InputLoci_DNase_Cons = get_Loci_DNase_Cons(inputFile);
			ArrayList< ArrayList<Float>> DnaseVec=(ArrayList<ArrayList<Float>>) InputLoci_DNase_Cons.get(1);
			ArrayList< ArrayList<Float>> ConsVec=(ArrayList<ArrayList<Float>>) InputLoci_DNase_Cons.get(2);
			ArrayList<String> lociList=(ArrayList<String>) InputLoci_DNase_Cons.get(0);

			/////////////////process control file/////////////////
			HashSet<Integer> filterByCtrl=new HashSet<Integer>();
			if(controlFile!="")
			{
				double cutoff=0.05;
				 System.out.println("processing control file:"+controlFile);
				 ArrayList CtrlLoci_DNase_Cons = get_Loci_DNase_Cons(controlFile);
					ArrayList< ArrayList<Float>> ctrlDnaseVec=(ArrayList<ArrayList<Float>>) CtrlLoci_DNase_Cons.get(1);
					ArrayList< ArrayList<Float>> ctrlConsVec=(ArrayList<ArrayList<Float>>) CtrlLoci_DNase_Cons.get(2);
					ArrayList<String> ctrllociList=(ArrayList<String>) CtrlLoci_DNase_Cons.get(0);
					
					//get mean vector
					ArrayList<NormalDistribution> ctrlDnaseMean = getBinNormalDists(ctrlDnaseVec);
					ArrayList<NormalDistribution> ctrlConsMean = getBinNormalDists(ctrlConsVec);
					
					 ArrayList<Float> dummy=new ArrayList<Float>();
					 for (int i = 0; i < 20; i++) {
						 dummy.add((float) 1/20);
					}

					double Pval=getPValFromBinNormalVec(dummy, ctrlDnaseMean);

					for (int i = 0; i < lociList.size(); i++) {

						double dPval=getPValFromBinNormalVec(DnaseVec.get(i), ctrlDnaseMean);

						double cPval=getPValFromBinNormalVec(ConsVec.get(i), ctrlConsMean);

						double finalPval=Math.min(dPval, cPval);
						if(finalPval>cutoff)
						{
							filterByCtrl.add(i);
						}
						
					}
					
			}
			
			/////////////////clustering //////////////////////////
			 System.out.println("clustering...");
			int num_feature=DnaseVec.get(0).size()+ConsVec.get(0).size();
			 DenseDoubleMatrix2D matrix=new DenseDoubleMatrix2D(DnaseVec.size(), DnaseVec.get(0).size()+ConsVec.get(0).size());
			 for (int i = 0; i < DnaseVec.size(); i++) {
				 ArrayList<Float> dnaseVec = DnaseVec.get(i);
				 ArrayList<Float> consVec = ConsVec.get(i);
				for (int j = 0; j <dnaseVec.size(); j++) {
					matrix.setQuick(i, j, dnaseVec.get(j));
				}
				for (int j = 0; j <consVec.size(); j++) {
					matrix.setQuick(i, dnaseVec.size()+j, consVec.get(j));
				}
			 }
			 
			 Instances data = matrix2instances(matrix);
			 //vanish the contrl filter set
			 if(filterByCtrl.size()>0)
			 {
				for (Integer ii : filterByCtrl) {
					data.instance(ii).setWeight(0);
				}	 
			 }
			
			
    			 XMeans xmean=new XMeans();
//				xmean.setMinNumClusters(2);
//				xmean.setMaxNumClusters(5);	
//    			 xmean.setMaxNumClusters(4);
    			 if(!noClustering)
    			 {
				xmean.buildClusterer(data);
    			 }
				
				
			/////////////////determine the which cluster is background set ////////////////

			 System.out.println("Identifying background cluster...");
			int num_class=1;
			if(!noClustering)
				num_class=xmean.numberOfClusters();
			 if(filterByCtrl.size()>0)
				 num_class+=1; //the last cluster is bgclass
			 
			ArrayList< DenseDoubleMatrix1D> Dnase_clust=new ArrayList< DenseDoubleMatrix1D>(num_class);
			ArrayList< DenseDoubleMatrix1D> Cons_clust=new ArrayList< DenseDoubleMatrix1D>(num_class);
			int[] clustCount=new int[num_class];
			for (int i = 0; i < num_class; i++) {
				Dnase_clust.add(new DenseDoubleMatrix1D(new double[num_feature/2]));
				Cons_clust.add(new DenseDoubleMatrix1D(new double[num_feature/2]));
			}
			
			ArrayList<Integer> classLabel=new ArrayList<Integer>();
				for (int i = 0; i < matrix.rows(); i++) {
					int clsid=0;
					if(!noClustering)
						clsid=xmean.clusterInstance(data.instance(i));		
					if(filterByCtrl.contains(i))
					{
						clsid=num_class-1;//the last cluster is bgclass
					}
					classLabel.add(clsid);
					clustCount[clsid]+=1;
					Dnase_clust.set(clsid, (DenseDoubleMatrix1D) Dnase_clust.get(clsid).assign(matrix.viewRow(i).viewPart(0, num_feature/2),PlusMult.plusMult(1)));
					Cons_clust.set(clsid, (DenseDoubleMatrix1D) Cons_clust.get(clsid).assign(matrix.viewRow(i).viewPart(num_feature/2, num_feature/2),PlusMult.plusMult(1)));
				}
				
				int bgCls=-1;
		
		
				double minMaxHeight=Double.MAX_VALUE;
				for (int i = 0; i < num_class; i++) {
					for (int j = 0; j <num_feature/2; j++) {
						Dnase_clust.get(i).set(j, Dnase_clust.get(i).getQuick(j)/clustCount[i]);
						Cons_clust.get(i).set(j, Cons_clust.get(i).getQuick(j)/clustCount[i]);
					}
					if(controlFile=="") 
					{
					double height=getMaxHeight(Dnase_clust.get(i));
					if(height<minMaxHeight)
					{
						bgCls=i;
						minMaxHeight=height;
					}
					}
				}
				 
		 
	    if(filterByCtrl.size()>0)
		{
			bgCls=num_class-1;
		}
		System.out.println("Background Class is "+bgCls);
			/////////////////output clustering result////////////////////
				 System.out.println("Outputing Result...");
				 if(! new File(common.outputDir).exists())
				 {
					 (new File(common.outputDir)).mkdirs();
				 }
				 String outname=new File(inputFile).getName();
				//output clustBed
				 PrintWriter out = new PrintWriter(common.outputDir+"/"+outname+".clust");
				 for (int i = 0; i < lociList.size(); i++) {
					 if(classLabel.get(i)!=bgCls)
						 out.println(lociList.get(i)+"\t"+classLabel.get(i));
				}
				 out.close();
				
				//output figure
				 ArrayList<XYSeries> dnaseDataList=new ArrayList<XYSeries>();
				 ArrayList<XYSeries> consDataList=new ArrayList<XYSeries>();
				 for (int i = 0; i < num_class; i++) {
					 XYSeries dnaseData=new XYSeries("DNase");
					 XYSeries consData=new XYSeries("Cons");
					 for (int j = 0; j <num_feature/2; j++) {
						 dnaseData.add(j,Dnase_clust.get(i).get(j));
						 consData.add(j,Cons_clust.get(i).get(j));
					 }
					 dnaseDataList.add(dnaseData);
					 consDataList.add(consData);
				 }
				 drawSignalAroundClust_headless(outname,dnaseDataList,consDataList,clustCount);
				
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		

		
		
	}

}
