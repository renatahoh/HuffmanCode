/**
 * Class to run Huffman encoding method.
 * Methods create a map with frequency of each characteres, a Binary tree with priority order by higher frequency,
 * and compute codes for each character in order to compress and decompress files
 *
 * CS10, Feb 9 2023
 * @author Emiko Rohn and Renata Edaes Hoh
 */

import java.io.*;
import java.util.*;

public class HuffmanEncoding implements Huffman{
    Map<Character, Long> frequencies;
    Map<Character, String> charCode;

    /**
     * class to implement comparator based on frequency of elements, will be implemented when making tree
     */
    static class TreeComparator implements Comparator<BinaryTree<CodeTreeElement>> {
        public int compare(BinaryTree<CodeTreeElement> ele1, BinaryTree<CodeTreeElement> ele2) {
            if (ele1.getData().getFrequency() < ele2.getData().getFrequency()) return -1;
            else if (ele1.getData().getFrequency() > ele2.getData().getFrequency()) return 1;
            else return 0;
        }
    }
    @Override
    public Map<Character, Long> countFrequencies(String pathName) throws IOException {
        frequencies = new HashMap<Character, Long>();
        BufferedReader input = new BufferedReader(new FileReader(pathName));
        try{
            int curr = input.read();
            // loop over characters of file
            while(curr != -1) {
                Character currentChar = (char) curr;
                // if character is already listed, just increase frequency number by 1. otherwise add character with freq of 1
                if(frequencies.containsKey(currentChar)) frequencies.put(currentChar, frequencies.get(currentChar) + 1);
                else frequencies.put(currentChar, 1L);
                curr = input.read();
            }
        }
        finally {
            input.close();
        }
        if(frequencies.isEmpty()){
            return null;
        }
        return frequencies;
    }

    @Override
    public BinaryTree<CodeTreeElement> makeCodeTree(Map<Character, Long> frequencies) {
        if(frequencies == null) return null;

        // initialize comparator and queue
        Comparator<BinaryTree<CodeTreeElement>> comparator = new TreeComparator();
        PriorityQueue<BinaryTree<CodeTreeElement>> queue = new PriorityQueue<>(comparator);

        // add all the charactrs in frequency to the tree as new trees containing CodeTreeElements (containing char and freq)
        for (Character c : frequencies.keySet()) {
            BinaryTree<CodeTreeElement> tree = new BinaryTree<>(new CodeTreeElement(frequencies.get(c), c));
            queue.add(tree);
        }
        // there's only one element, then remove tree added as leaf and add it as child of a root with frequency and null char
        if(queue.size() == 1){
            BinaryTree<CodeTreeElement> T = queue.remove();
            BinaryTree<CodeTreeElement> newTree = new BinaryTree<>(new CodeTreeElement(T.getData().getFrequency(), null), T, null);
            queue.add(newTree);
        }
        // otherwise loop over queue to build the tree, getting two trees with smaller frequencies and adding them as children
        // of a node with null char and frequency equal to sum of their frequencies
        while (queue.size() > 1) {
            BinaryTree<CodeTreeElement> T1 = queue.remove();
            BinaryTree<CodeTreeElement> T2 = queue.remove();
            Long totalFrequency = T1.getData().getFrequency() + T2.getData().getFrequency();
            BinaryTree<CodeTreeElement> newTree = new BinaryTree<>(new CodeTreeElement(totalFrequency, null), T1, T2);
            queue.add(newTree);
        }
        return queue.remove();
    }

    @Override
    public Map<Character, String> computeCodes(BinaryTree<CodeTreeElement> codeTree) {
        if (codeTree == null) return null;

        charCode = new HashMap<>();
        // string of path
        String code = "";
        // call recursive helper function
        traverse(codeTree, code);
        return charCode;
    }

    public void traverse(BinaryTree<CodeTreeElement> tree, String code) {
        // if is lead, add code to the map of codes
        if (tree.isLeaf()) charCode.put(tree.getData().getChar(), code);
        else {
            // else recurse through tree
            if (tree.hasLeft()) traverse(tree.getLeft(), code + 0);
            if (tree.hasRight()) traverse(tree.getRight(), code + 1);
        }
    }

    @Override
    public void compressFile(Map<Character, String> codeMap, String pathName, String compressedPathName) throws IOException {

        BufferedReader input = new BufferedReader(new FileReader(pathName));
        BufferedBitWriter output = new BufferedBitWriter(compressedPathName);

        try{
            int curr;
            while ((curr = input.read()) != -1) {
                // change to char type
                Character currChar = (char) curr;
                // get code from path map
                String code = codeMap.get(currChar);
                // loop over string of code/path
                for (int i = 0; i < code.length(); i++) {
                    // get each char
                    Character c = code.charAt(i);
                    if (c == '0') output.writeBit(false);
                    if (c == '1') output.writeBit(true);
                }
            }
        }finally{
            input.close();
            output.close();
        }
    }

    @Override
    public void decompressFile(String compressedPathName, String decompressedPathName, BinaryTree<CodeTreeElement> codeTree) throws IOException {

        BufferedBitReader bitInput = new BufferedBitReader(compressedPathName);
        BufferedWriter charOutput = new BufferedWriter(new FileWriter(decompressedPathName));

        // temporary path of each character
        BinaryTree<CodeTreeElement> tempTree = codeTree;

        try{
            // loop over encoding
            while (bitInput.hasNext()) {
                boolean bit = bitInput.readBit();
                // if bit == true, go down the tree to the right
                if (bit) {
                    tempTree = tempTree.getRight();
                }
                // else go down to the left
                else if (!bit) {
                    tempTree = tempTree.getLeft();
                }
                // if is leaf, write in decompressed file and reset path tree to the original tree
                if (tempTree.isLeaf()) {
                    Character c = tempTree.getData().getChar();
                    charOutput.write(c);
                    tempTree = codeTree;
                }
            }
        }finally{
            charOutput.close();
            bitInput.close();
        }
    }

    public static void main(String[] args) throws IOException {
        //Test: Testing with a few characters
        HuffmanEncoding file = new HuffmanEncoding();
        Map<Character, Long> freq = file.countFrequencies("inputs/test0.txt");
        BinaryTree<CodeTreeElement> codeTree = file.makeCodeTree(freq);
        Map<Character, String> codes = file.computeCodes(codeTree);
        file.compressFile(codes, "inputs/test0.txt", "test0_compressed.txt");
        file.decompressFile("test0_compressed.txt","test0_decompressed.txt", codeTree);

        //Test: with multiple of one character
        freq = file.countFrequencies("inputs/test1.txt");
        codeTree = file.makeCodeTree(freq);
        codes = file.computeCodes(codeTree);
        file.compressFile(codes, "inputs/test1.txt", "test1_compressed.txt");
        file.decompressFile("test1_compressed.txt","test1_decompressed.txt", codeTree);

        //Test: Single char
        freq = file.countFrequencies("inputs/test3.txt");
        codeTree = file.makeCodeTree(freq);
        codes = file.computeCodes(codeTree);
        file.compressFile(codes, "inputs/test3.txt", "test3_compressed.txt");
        file.decompressFile("test3_compressed.txt","test3_decompressed.txt", codeTree);

        //US Constitution
        freq = file.countFrequencies("inputs/USConstitution.txt");
        codeTree = file.makeCodeTree(freq);
        codes = file.computeCodes(codeTree);
        file.compressFile(codes, "inputs/USConstitution.txt", "USConstitution_compressed.txt");
        file.decompressFile("USConstitution_compressed.txt","USConstitution_decompressed.txt", codeTree);

        //War and Peace
        // 1.8 MB
        freq = file.countFrequencies("inputs/WarAndPeace.txt");
        codeTree = file.makeCodeTree(freq);
        codes = file.computeCodes(codeTree);
        file.compressFile(codes, "inputs/WarAndPeace.txt", "WarAndPeace_compressed.txt");
        file.decompressFile("WarAndPeace_compressed.txt","WarAndPeace_decompressed.txt", codeTree);

        //Test: with empty file
        freq = file.countFrequencies("inputs/test2.txt");
        codeTree = file.makeCodeTree(freq);
        codes = file.computeCodes(codeTree);
        file.compressFile(codes, "inputs/test2.txt", "test2_compressed.txt");
        file.decompressFile("test2_compressed.txt","test2_decompressed.txt", codeTree);
    }
}
