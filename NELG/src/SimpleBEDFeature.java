import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;

import org.broad.tribble.annotation.Strand;

import org.broad.tribble.bed.BEDFeature;
import org.broad.tribble.bed.FullBEDFeature.Exon;


public class SimpleBEDFeature implements BEDFeature, Serializable {
	protected String chr;
    protected int start = -1;
    protected int end = -1;
    protected Strand strand = Strand.NONE;
    private String name = "";
    private float score = Float.NaN;
    private String type = "";
    private String description;//protected float confidence;
    //private String identifier;
    private String link;
	
	@Override
	public List<Exon> getExons() {
		// TODO Auto-generated method stub
		return null;
	}

	 public SimpleBEDFeature(int start, int end, String chr) {
	        this.start = start;
	        this.end = end;
	        this.chr = chr;
	    }

	 public SimpleBEDFeature(BEDFeature feat)
	 {
		 start=feat.getStart();
		 end=feat.getEnd();
		 chr=feat.getChr();
		 score=feat.getScore();
		 if(Float.isNaN(score))
			 score=1;
		 strand=feat.getStrand();
		 name=feat.getName();
		 type=feat.getType();
		 link=feat.getLink();
		 description=feat.getDescription();
	 }
	    @Override
	public String toString() {
		// TODO Auto-generated method stub
		return this.chr+":"+ this.start+"-"+ this.end;
	}
	    
	    
   public static void toFile(List<SimpleBEDFeature> bedlist,String filename)
   {
	   FileWriter outFile;
	try {
		outFile = new FileWriter(filename);
		 PrintWriter out = new PrintWriter(outFile);
		 for(SimpleBEDFeature bed:bedlist)
		 {
			 out.println(bed.getChr()+"\t"+bed.getStart()+"\t"+bed.getEnd()+"\t"+bed.getScore());
		 }
		 out.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
   }

		public String getChr() {
	        return chr;
	    }

	    public int getStart() {
	        return start;
	    }

	    public int getEnd() {
	        return end;
	    }

	    public Strand getStrand() {
	        return strand;
	    }

	    public void setStrand(Strand strand) {
	        this.strand = strand;
	    }

	    public void setChr(String chr) {
	        this.chr = chr;
	    }

	    public void setStart(int start) {
	        this.start = start;
	    }

	    public void setEnd(int end) {
	        this.end = end;
	    }

	    public String getType() {
	        return type;
	    }

	    public void setType(String type) {
	        this.type = type;
	    }

	    public Color getColor() {
	        return null;
	    }

	    public String getDescription() {
	        return description;
	    }

	    public void setDescription(String description) {
	        this.description = description;
	    }

	    public String getName() {
	        return name;
	    }

	    public void setName(String name) {
	        this.name = name;
	    }

	    public float getScore() {
	        return score;
	    }

	    public void setScore(float score) {
	        this.score = score;
	    }

	    public String getLink() {
	        return link;
	    }

	    public void setLink(String link) {
	        this.link = link;
	    }
}
