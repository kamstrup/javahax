import java.util.Arrays;

/**
 * Simpler interface than Java's standard {@code BitSet} class.
 * There is a default implementation backed by an 32 bit int array,
 * but the interface is simple enough to provide easy implementation
 * on top of memory mapped files or other structures.
 *
 * @author kamstrup
 */
public interface BitField {

    public void set(int index);

    public boolean get(int index);

    public void clear(int index);

    public void clear();

    public int capacity();

    public static BitField create(int capacity) {
        return new BitFieldImpl(capacity);
    }

    static class BitFieldImpl implements BitField {

        private final int[] values;

        public BitFieldImpl(int capacity) {
            values = new int[valueIndex(capacity) + 1];
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
