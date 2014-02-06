import java.util.*;

/**
 * High performance implementation of a Bloom Filter.
 * <p/>
 * The Bloom filter implementation provided by this class is about as fast
 * as you get without implementing everything in native code or hardware.
 * <p/>
 * The default implementations of this interface guarantee that no operation
 * allocates or frees memory. This mean that they guarantee zero GC overhead
 * and are suitable for real-time systems.
 *
 * @author kamstrup
 */
public abstract class BloomFilter<T> {

    private static final float LN2 = (float) Math.log(2);

    /**
     * A function that returns a 32 bit hash code for an object of type {@code T}.
     * @param <T> the type of objects this hash function applies to.
     */
    public interface HashFunc<T> {
        public int hash(T val);
    }

    /**
     * A {@link HashFunc} that simply uses the default Java {@link #hashCode()}
     * of the object.
     */
    public static final HashFunc<Object> JAVA_HASH = new HashFunc<Object>() {
        @Override
        public int hash(Object val) {
            return val.hashCode();
        }
    };

    /**
     * Add a value to the Bloom filter.
     * <p/>
     * This operation has running time O(1).
     *
     * @param val the value to add
     */
    public abstract void add (T val);

    /**
     * Check if an item matches the filter. Note that Bloom filter matching
     * bears the risk of false positives.
     * <p/>
     * This operation has running time O(1).
     * <p/>
     * You can can get the false-positive probability by calling FIXME.
     *
     * @param val the value to check for.
     * @return {@code true} if the item maybe is in the filter. {@code false}
     *         implies that the item <i>definitely not</i> is in the filter.
     */
    public abstract boolean maybeContains(T val);

    /**
     * Remove all items from the filter.
     * <p/>
     * This operation has running time O(m).
     */
    public abstract void clear();

    /**
     * The number of times the {@link #add} method was called on this filter.
     * Will be reset when {@link #clear} is called.
     */
    public abstract int size();

    /**
     * Create a new Bloom filter.
     * <p/>
     * If both {@code capacity} and {@code bitsPerValue} are powers of 2
     * this factory function will create a faster Bloom filter.
     *
     * @param capacity the number of values the filter should be able to store
     * @param bitsPerValue number of bits to use per value. 10 is suitable for
     *                     most applications.
     * @param func the {@link BloomFilter.HashFunc} to use. Fx. {@link #JAVA_HASH}
     * @param <T> the type of values that will be added to the filter
     * @return a new filter
     */
    public static <T> BloomFilter<T> create(int capacity, int bitsPerValue, HashFunc<? super T> func) {
        int k = (int)(LN2 * bitsPerValue);
        int m = capacity * bitsPerValue;

        // Choose the slightly faster power-of-two filter if we can
        if (POTBloomFilter.isPowerOfTwo(m)) {
            return new POTBloomFilter<T>(m, k, func);
        } else {
            return new DynamicBloomFilter<T>(m, k, func);
        }
    }

    // http://stackoverflow.com/questions/664014/what-integer-hash-function-are-good-that-accepts-an-integer-hash-key
    private static int shuffleInt(int x) {
        x = ((x >>> 16) ^ x) * 0x45d9f3b;
        x = ((x >>> 16) ^ x) * 0x45d9f3b;
        x = ((x >>> 16) ^ x);
        return x;
    }

    // NOT tested https://code.google.com/p/fast-hash/
    private static int shuffleInt2(int x) {
        long h = x;
        h ^= h >>> 23;
        h *= 0x2127599bf4325c37L;
        h ^= h >>> 47;
        return (int)h;
    }

    /**
     * Private impl class that can provide a Bloom filter for any (m,k).
     */
    private static class DynamicBloomFilter<T> extends BloomFilter<T> {

        protected final BitField bits;
        protected final int m;
        protected final int k;
        protected final HashFunc<? super T> func;
        protected int count;

        public DynamicBloomFilter(int m, int k, HashFunc<? super T> func) {
            this.m = m;
            this.k = k;
            this.func = func;

            bits = BitField.create(m);
        }

        @Override
        public void add(T val) {
            if (count >= m) {
                throw new IllegalStateException("Filter capacity reached");
            }

            int hashCode = func.hash(val);
            for (int i = 0; i < k; i++) {
                bits.set(Math.abs(hashCode % m));
                hashCode = shuffleInt(hashCode);
            }
            count++;
        }

        @Override
        public boolean maybeContains(T val) {
            int hashCode = func.hash(val);
            for (int i = 0; i < k; i++) {
                if (!bits.get(Math.abs(hashCode % m))) {
                    return false;
                }
                hashCode = shuffleInt(hashCode);
            }
            return true;
        }

        @Override
        public void clear() {
            bits.clear();
            count = 0;
        }

        @Override
        public int size() {
            return count;
        }

        public BitField getRawBits() {
            return bits;
        }
    }

    /**
     * If m is a power of two we can optimize the truncation of the hash value
     * from being a modulo with m, to simply masking with m-1.
     */
    private static class POTBloomFilter<T> extends DynamicBloomFilter<T> {

        public POTBloomFilter(int m_POT, int k, HashFunc<? super T> func) {
            super(m_POT, k, func);

            if (!isPowerOfTwo(m_POT)) {
                throw new IllegalArgumentException("Number of bits must be a power-of-two: " + m_POT);
            }
            System.out.println("POT");
        }

        @Override
        public void add(T val) {
            if (count >= m) {
                throw new IllegalStateException("Filter capacity reached");
            }

            int hashCode = func.hash(val);
            for (int i = 0; i < k; i++) {
                bits.set(hashCode & (m - 1));
                hashCode = shuffleInt(hashCode);
            }
            count++;
        }

        @Override
        public boolean maybeContains(T val) {
            int hashCode = func.hash(val);
            for (int i = 0; i < k; i++) {
                if (!bits.get(hashCode & (m - 1))) {
                    return false;
                }
                hashCode = shuffleInt(hashCode);
            }
            return true;
        }

        public static boolean isPowerOfTwo(int i) {
            if (i <= 0) {
                throw new IllegalArgumentException("Input must be positive: " + i);
            }

            return ((i & (i - 1)) == 0);
        }
    }

    public static void main (String[] args) {
        BloomFilter<String> f = BloomFilter.create(128, 16, JAVA_HASH);
        for (int i = 0; i < 100; i++) {
            f.add(i + "");
        }

        for (int i = 0; i < 100; i++) {
            if (!f.maybeContains(i+"")) {
                throw new Error(i + "");
            }
        }

        List<Integer> bad = new ArrayList<>();
        List<Integer> good = new ArrayList<>();
        for (int i = 100; i < 1000; i++) {
            if (f.maybeContains(i+"")) {
                bad.add(i);
            } else {
                good.add(i);
            }
        }

        System.out.println("BAD " + bad);
        System.out.println("GOOD " + good);
    }
}
