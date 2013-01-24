/**
 * Efficient sparse set for <i>positive</i>integers, as described by
 * Briggs and Torczon's 1993 paper "An Efficient Representation for Sparse Sets".
 * See also
 * <a href="http://research.swtch.com/sparse">research.swtch.com/sparse</a>
 * <p/>
 * The primary purpose of this class is to store a sparse set of low-value
 * positive integers - such as typical enum values or a bounded set of
 * identifiers.
 * <p/>
 * One potentially useful quirk of this data structure, besides being
 * mind-numbingly fast and always O(1), is that you can add elements to
 * it while iterating over it via the {@link #get} method.
 * This works because, internally, new values are always appended. Example:
 * <p/>
 * <pre>
 *     for (int i = 0; i < s.size(); i++) {
 *         int val = s.get(i);
 *         if (someCondition(val)) {
 *             s.add (newVal);
 *         }
 *     }
 * </pre>
 * <p/>
 * Likewise - you could safely remove the last added element by calling
 * {@link #pop()} from inside the for-loop.
 * <p/>
 * A notable caveat is that the memory consumption is proportional to the
 * element with the largest value. Explicitly - this implementation will
 * allocate <i>at least</i> twice the as many ints as the largest value
 * you store.
 *
 * @author kamstrup
 */
public final class SparseSet {

	private int[] dense;
	private int[] sparse;
	private int n;

	/**
	 * Create a new set with an initial capacity of 10.
	 */
	public SparseSet () {
		this (10);
	}
	
	/**
	 * Create a new set with a custom initial capacity.
	 */
	public SparseSet (int initialCapacity) {
		if (initialCapacity <= 0) {
			throw new IllegalArgumentException(
			    "Initial capacity must be > 0: " + initialCapacity);
		}
		dense = new int[initialCapacity];
		sparse = new int[initialCapacity];
		n = 0;
	}
	
	/**
	 * Add an element to the set. Running time O(1).
	 */
	public void add (int i) {
		if (i >= dense.length) {
			growBuffers (i);
		}
		
		dense[n] = i;
		sparse[i] = n;
		n++;
	}
	
	/**
	 * Remove and return the last added element. Running time O(1).
	 */
	public int pop () {
		n--;
		return dense[n];
	}
	
	/**
	 * Look at the last added element without removing it.
	 * Running time O(1).
	 */
	public int peek () {
		return dense[n-1];
	}
	
	/**
	 * Returns true if the element is in the set.
	 * Running time O(1).
	 */
	public boolean contains (int i) {
		return sparse[i] < n && dense[sparse[i]] == i;
	}
	
	/**
	 * Remove an elementfrom the set. Running time O(1). Returns true
	 * iff the element was removed.
	 * <p/>
	 * Warning: Using this method moves the last inserted element
	 * into the position of the removed element. Breaking the insertion
	 * order invariant - if you rely on that.
	 */
	public boolean remove (int i) {
		if (!contains (i)) {
			return false;
		}
		
		int j = dense[n-1];
		dense[sparse[i]] = j;
		sparse[j] = sparse[i];
		n--;
		
		return true;
	}
	
	/**
	 * Remove all elements from the set. Running time O(1).
	 */
	public void clear () {
		n = 0;
	}
	
	/**
	 * Number of elements in the set. Running time O(1).
	 */
	public int size () {
		return n;
	}
	
	/**
	 * True iff the set is empty. Running time O(1).
	 */
	public boolean empty () {
		return n == 0;
	}
	
	/**
	 * Get an element by insertion order. Running time O(1). You can use
	 * this method to implement {@code O(n)} iteration over the set, where
	 * {@code n} is the number of elements.
	 */
	public int get (int offset) {
		return dense[offset];
	}
	
	private void growBuffers (int minSize) {
		int newLength = Math.max (dense.length, minSize) * 2;
		int[] newDense = new int[newLength];
		int[] newSparse = new int[newLength];
		
		System.arraycopy (dense, 0, newDense, 0, dense.length);
		System.arraycopy (sparse, 0, newSparse, 0, dense.length);
		
		dense = newDense;
		sparse = newSparse;
	}

	public static void main (String[] args) {
		SparseSet s = new SparseSet ();	
		System.out.println ("Empty. Contains 7? " + s.contains(7));
		System.out.println ("Empty. Size " + s.size ());
		
		s.add (5);
		s.add (7);
		s.add (1);
		s.add (127);
		System.out.println ("Full. Contains 7? " + s.contains(7));
		System.out.println ("Full. Size " + s.size ());
		
		System.out.println ("Contents:");
		for (int i = 0; i < s.size (); i++) {
			System.out.println (s.get (i));
		}
		
		if (!s.remove (7)) {
			throw new RuntimeException ();
		}
		System.out.println ("Removed. Contains 7? " + s.contains(7));
		System.out.println ("Full. Size " + s.size ());
		
		System.out.println ("Contents:");
		for (int i = 0; i < s.size (); i++) {
			System.out.println (s.get (i));
		}
	}
}
