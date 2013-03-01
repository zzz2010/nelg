import java.util.Comparator;

import org.apache.commons.math3.util.Pair;


public class PairComparator<K,V extends Comparable<V>> implements Comparator<Pair<K,V>> {

	
	public int compare(Pair<K, V> o1, Pair<K, V> o2) {
		return o1.getValue().compareTo(o2.getValue());
	}

}
