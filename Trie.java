/**
 * A trie. Probably fast, but haven't tested it.
 * Uses an optimized POT hashing strategy to walk nodes.
 * <p/>
 * TODO:
 *  - Code cleanup. This is really just a messy code dump.
 *  - deletion
 *  - javadocs
 *  - size()
 *  - trim() returning a compacted, readdonly,  version that does not use POT hashing but just modulo.
 */
public class Trie    {

    private static final class Node {
        boolean term = false;
        final char c;
        Node[][] buckets;

        private Node(char c) {
            this.c = c;
        }

        private Node(char c, Node[][] buckets) {
            this(c);
            this.buckets = buckets;
        }
    }

    private static final int BUCKET_SIZE = 3;

    private Node root = new Node('\0', new Node[4][BUCKET_SIZE]);
    private long nodeCount = 1;

    public boolean add(CharSequence chars) {
        Node node = root;
        long startNodeCount = nodeCount;
        int depth;
        for (depth = 0; depth < chars.length(); depth++) {
            char c = chars.charAt(depth);
            Node[] bucket = ensureBucket(node, c);
            assert bucket != null;

            Node nextNode = tryAddToBucket(bucket, c);
            while (nextNode == null) {
                // No room in bucket, grow it
                Node[][] newBuckets = new Node[bucket.length*2][BUCKET_SIZE];
                System.arraycopy(node.buckets, 0, newBuckets, 0, node.buckets.length);
                node.buckets = newBuckets;

                bucket = ensureBucket(node, c);
                assert bucket != null;

                nextNode = tryAddToBucket(bucket, c);
            }

            node = nextNode;
        }

        node.term = true;

        return nodeCount != startNodeCount;
    }

    public boolean contains(CharSequence chars) {
        Node node = root;
        int depth;
        for (depth = 0; depth < chars.length() && node != null; depth++) {
            char c = chars.charAt(depth);
            if (node.buckets == null) return false;
            Node[] bucket = peekBucket(node.buckets, c);
            if (bucket == null) return false;

            node = findInBucket(bucket, c);
        }

        return node != null && node.term && depth == chars.length();
    }


    private static Node[] ensureBucket(Node node, char c) {
        if (node.buckets == null) {
            node.buckets = new Node[4][BUCKET_SIZE];
        }

        Node[][] buckets = node.buckets;
        int idx = c & (buckets.length-1);
        Node[] b = buckets[idx];
        if (b == null) {
            b = new Node[BUCKET_SIZE];
            buckets[idx] = b;
        }
        return b;
    }

    private static Node[] peekBucket(Node[][] buckets, char c) {
        return buckets[c & (buckets.length-1)];
    }

    private Node tryAddToBucket(Node[] bucket, char c) {
        for (int i = 0; i < BUCKET_SIZE; i++) {
            Node node = bucket[i];
            if (bucket[i] == null) {
                nodeCount++;
                return (bucket[i] = new Node(c));
            } else if (node.c == c) {
                return node;
            }
        }
        return null;
    }

    private Node findInBucket(Node[] bucket, char c) {
        for (int i = 0; i < BUCKET_SIZE; i++) {
            Node node = bucket[i];
            if (node == null) return null;
            if (node.c == c) {
                return node;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        Trie trie = new Trie();
        String[] words = "hej,med,dig,din,abe,dingo,dinosaur".split(",");
        for (String word: words) {
            trie.add(word);
        }

        for (String word : words) {
            System.out.println(word + ": " + trie.contains(word));
        }

        for (String word : "he,me,digga,dinn,abb,ål,øl".split(",")) {
            System.out.println(word + ": " + trie.contains(word));
        }
    }
}
