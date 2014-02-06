import java.util.Arrays;

/**
 * Simpler interface than Java's standard {@code BitSet} class.
 * There is a default implementation backed by an 32 bit int array,
 * but the interface is simple enough to provide easy implementation
 * on top of memory mapped files or other structures.
 * <p/>
 * One notable difference between this interface and the standard
 * {@link java.util.BitSet} is that a {@code BitField} can not expand
 * dynamically.
 *
 * @author kamstrup
 */
public interface BitField {

    /**
     * Set the bit at a given index to 1.
     * <p/>
     * Note the important difference between {@code BitField} and the standard
     * Java {@link java.util.BitSet} is that that a bit field will not expand
     * dynamically. It is a programming error to access indexes past the
     * allocated bit field capacity.
     *
     * @param index the 0-based index of the bit to set
     */
    public void set(int index);

    /**
     * Get the value of the bit at a given index.
     * @param index the 0-based index to get
     * @return {@code true} iff the bit is 1.
     * @throws java.lang.ArrayIndexOutOfBoundsException if the index is out of
     *                                                  bounds
     */
    public boolean get(int index);

    /**
     * Set the bit at a given index to 0.
     * @param index the 0-based index of the bit to set
     */
    public void clear(int index);

    /**
     * Clear the entire bit set, setting all bits to 0.
     */
    public void clear();

    /**
     * Return the number of bits available in this bit field.
     * @return the number of bits available in this bit field.
     */
    public int capacity();

    /**
     * Create a new {@code BitField} with <i>at least</i> {@code minCapacity}
     * bits available. Implementations are free to allocate more than the
     * requested capacity. It can typically be rounded up to the nearest
     * 32- or 64-bit boundary.
     *
     * @param minCapacity The minimum capacity of the bit field
     * @return a new bit field that can hold at least {@code minCapacity} bits
     */
    public static BitField create(int minCapacity) {
        return new BitFieldImpl(minCapacity);
    }

    /**
     * Private impl class of BitField based on 32 bit ints.
     */
    static class BitFieldImpl implements BitField {

        private final int[] values;

        public BitFieldImpl(int minCapacity) {
            values = new int[valueIndex(minCapacity) + 1];
        }

        @Override
        public void set(int index) {
            // java lshift masks with the lower 5 bits of the int, same as % 32
            values[valueIndex(index)] |= (1 << index);
        }

        @Override
        public boolean get(int index) {
            return (values[valueIndex(index)] & (1 << index)) != 0;
        }

        @Override
        public void clear(int index) {
            values[valueIndex(index)] &= ~(1 << index);
        }

        @Override
        public void clear() {
            Arrays.fill(values, 0);
        }

        @Override
        public int capacity() {
            return values.length * 32;
        }

        private static int valueIndex(int index) {
            // rshift by 5 is same as division by 2^5=32
            return index >> 5;
        }
    }

    static void _assert(boolean b) {
        if (!b) throw new Error();
    }
    public static void main(String[] args) {
        for (int cap : Arrays.asList(1, 2, 31, 32, 33, 50, 63, 64, 65, 100, 127, 128, 129)) {
            BitField b = BitField.create(cap);
            for (int i = 0; i < cap; i++) _assert(!b.get(i));
            for (int i = 0; i < cap; i++) b.set(i);
            for (int i = 0; i < cap; i++) _assert(b.get(i));
            for (int i = 0; i < cap; i++) b.clear(i);
            for (int i = 0; i < cap; i++) _assert(!b.get(i));
            for (int i = 0; i < cap; i++) b.set(i);
            b.clear();
            for (int i = 0; i < cap; i++) _assert(!b.get(i));

            b.clear();
            b.set(0);
            _assert(b.get(0));
            for (int i = 1; i < cap; i++) _assert(!b.get(i));

            b.clear();
            for (int i = 0; i < cap; i += 2) b.set(i);
            for (int i = 0; i < cap; i += 2) _assert(b.get(i));
            for (int i = 1; i < cap; i += 2) _assert(!b.get(i));
        }
    }

}
