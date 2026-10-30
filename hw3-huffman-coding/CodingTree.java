import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * James Fischer
 */
public class CodingTree {

    /** Map from characters to their Huffman codes (strings of '0' and '1'). */
    public Map<Character, String> codes;

    /** Packed bits of the encoded message. */
    public List<Byte> bits;

    /** Root of the Huffman tree. */
    private Node myRoot;

    /** Length of the original message in characters. */
    private int myOriginalLength;

    /**
     * Internal node class for the Huffman tree.
     */
    private static class Node implements Comparable<Node> {
        char ch;
        int freq;
        Node left;
        Node right;

        Node(char theChar, int theFreq) {
            ch = theChar;
            freq = theFreq;
        }

        Node(char theChar, int theFreq, Node theLeft, Node theRight) {
            ch = theChar;
            freq = theFreq;
            left = theLeft;
            right = theRight;
        }

        boolean isLeaf() {
            return left == null && right == null;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.freq, other.freq);
        }
    }

    /**
     * Constructor
     */
    public CodingTree(String message) {
        codes = new HashMap<Character, String>();
        bits = new ArrayList<Byte>();
        myOriginalLength = (message == null) ? 0 : message.length();

        if (message == null || message.length() == 0) {
            myRoot = null;
            return;
        }

        Map<Character, Integer> freq = countFrequencies(message);
        MyPriorityQueue<Node> pq = new MyPriorityQueue<Node>();

        for (Map.Entry<Character, Integer> entry : freq.entrySet()) {
            pq.offer(new Node(entry.getKey(), entry.getValue()));
        }

        if (pq.size() == 1) {
            // Special case: only one distinct character in message
            myRoot = pq.poll();
            buildCodes(myRoot, "");
        } else {
            while (pq.size() > 1) {
                Node a = pq.poll();
                Node b = pq.poll();
                Node parent = new Node('\0', a.freq + b.freq, a, b);
                pq.offer(parent);
            }
            myRoot = pq.poll();
            buildCodes(myRoot, "");
        }

        encodeMessage(message);
    }

    /**
     * Count the frequency of each character in the message.
     */
    private Map<Character, Integer> countFrequencies(String message) {
        Map<Character, Integer> freq = new HashMap<Character, Integer>();
        for (int i = 0; i < message.length(); i++) {
            char ch = message.charAt(i);
            Integer count = freq.get(ch);
            if (count == null) {
                freq.put(ch, 1);
            } else {
                freq.put(ch, count + 1);
            }
        }
        return freq;
    }

    /**
     * Recursively assign Huffman codes to leaf nodes.
     */
    private void buildCodes(Node node, String prefix) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            // Single-character message edge case: ensure at least one bit
            if (prefix.length() == 0) {
                codes.put(node.ch, "0");
            } else {
                codes.put(node.ch, prefix);
            }
        } else {
            buildCodes(node.left, prefix + "0");
            buildCodes(node.right, prefix + "1");
        }
    }

    /**
     * Encode the message into the bits list, packing bits into bytes.
     */
    private void encodeMessage(String message) {
        StringBuilder bitString = new StringBuilder();

        for (int i = 0; i < message.length(); i++) {
            char ch = message.charAt(i);
            String code = codes.get(ch);
            if (code == null) {
                throw new IllegalStateException("No code for character: " + ch);
            }
            bitString.append(code);
        }

        bits.clear();
        int index = 0;
        while (index < bitString.length()) {
            byte b = 0;
            for (int bit = 0; bit < 8; bit++) {
                b <<= 1;
                if (index < bitString.length() && bitString.charAt(index) == '1') {
                    b |= 1;
                }
                index++;
            }
            bits.add(b);
        }
    }

    /**
     * Optional decode method:
     * Reconstructs the original text from encoded bits and code map.
     */
    public String decode(List<Byte> encodedBits,
                         Map<Character, String> theCodes,
                         int stopsize) {

        if (encodedBits == null || theCodes == null) {
            throw new IllegalArgumentException("Arguments must not be null.");
        }

        Map<String, Character> reverse = new HashMap<String, Character>();
        for (Map.Entry<Character, String> entry : theCodes.entrySet()) {
            reverse.put(entry.getValue(), entry.getKey());
        }

        StringBuilder result = new StringBuilder(stopsize);
        StringBuilder current = new StringBuilder();

        outer:
        for (Byte boxed : encodedBits) {
            int value = boxed & 0xFF;
            for (int i = 7; i >= 0; i--) {
                int bit = (value >> i) & 1;
                current.append(bit == 1 ? '1' : '0');
                Character ch = reverse.get(current.toString());
                if (ch != null) {
                    result.append(ch.charValue());
                    if (result.length() == stopsize) {
                        break outer;
                    }
                    current.setLength(0);
                }
            }
        }

        return result.toString();
    }

   
    public int getOriginalLength() {
        return myOriginalLength;
    }
}
