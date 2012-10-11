import java.io.Serializable;


public class Pair<T1, T2>  implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4370862214620058909L;
	T1 key;
	T2 value;
	public Pair(T1 key, T2 value) {
		super();
		this.key = key;
		this.value = value;
	}
	
}
