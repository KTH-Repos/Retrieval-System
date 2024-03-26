/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ir.Query.QueryTerm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * This is the main class for the search engine.
 */
public class Engine {

    /** The inverted index. */
    Index index = new HashedIndex();
    // Assignment 1.7: Comment the line above and uncomment the next line
    // Index index = new PersistentHashedIndex();

    /** The indexer creating the search index. */
    Indexer indexer;

    /** The searcher used to search the index. */
    Searcher searcher;

    /** K-gram index */
    KGramIndex kgIndex = null;
    // Assignment 3: Comment the line above and uncomment the next line
    // KgramIndex kgIndex = new KGramIndex(2);

    /** Spell checker */
    SpellChecker speller;
    // Assignment 3: Comment the line above and uncomment the next line
    // SpellChecker = new SpellChecker( index, kgIndex );

    /** The engine GUI. */
    SearchGUI gui;

    /** Directories that should be indexed. */
    ArrayList<String> dirNames = new ArrayList<String>();

    /** Lock to prevent simultaneous access to the index. */
    Object indexLock = new Object();

    /** The patterns matching non-standard words (e-mail addresses, etc.) */
    String patterns_file = null;

    /** The file containing the logo. */
    String pic_file = "";

    /** The file containing the pageranks. */
    String rank_file = "";

    /** For persistent indexes, we might not need to do any indexing. */
    boolean is_indexing = true;

    int docCounter = 0;

    /* ----------------------------------------------- */

    /**
     * Constructor.
     * Indexes all chosen directories and files
     */
    public Engine(String[] args) {
        decodeArgs(args);
        indexer = new Indexer(index, kgIndex, patterns_file);
        searcher = new Searcher(index, kgIndex);
        gui = new SearchGUI(this);
        gui.init();
        boolean calculateEucLength = false;
        /*
         * Calls the indexer to index the chosen directory structure.
         * Access to the index is synchronized since we don't want to
         * search at the same time we're indexing new files (this might
         * corrupt the index).
         */
        if (is_indexing) {
            synchronized (indexLock) {
                gui.displayInfoText("Indexing, please wait...");
                long startTime = System.currentTimeMillis();
                for (int i = 0; i < dirNames.size(); i++) {
                    File dokDir = new File(dirNames.get(i));
                    indexer.processFiles(dokDir, is_indexing);
                }
                if (calculateEucLength) {
                    System.out.println("Calculating euclidean length of all docs!");
                    calculateEuclideanLength(new File(dirNames.get(0)));
                    writeEuclideanLengthToFile();
                    System.out.println("Finish writing euclidean length of all docs to local file!");
                } else {
                    readDataFromFile();
                }
                long elapsedTime = System.currentTimeMillis() - startTime;
                gui.displayInfoText(String.format("Indexing done in %.1f seconds.", elapsedTime / 1000.0));
                index.cleanup();
            }
        } else {
            gui.displayInfoText("Index is loaded from disk");
        }
    }

    /* ----------------------------------------------- */

    /**
     * Decodes the command line arguments.
     */
    private void decodeArgs(String[] args) {
        int i = 0, j = 0;
        while (i < args.length) {
            if ("-d".equals(args[i])) {
                i++;
                if (i < args.length) {
                    dirNames.add(args[i++]);
                }
            } else if ("-p".equals(args[i])) {
                i++;
                if (i < args.length) {
                    patterns_file = args[i++];
                }
            } else if ("-l".equals(args[i])) {
                i++;
                if (i < args.length) {
                    pic_file = args[i++];
                }
            } else if ("-r".equals(args[i])) {
                i++;
                if (i < args.length) {
                    rank_file = args[i++];
                }
            } else if ("-ni".equals(args[i])) {
                i++;
                is_indexing = false;
            } else {
                System.err.println("Unknown option: " + args[i]);
                break;
            }
        }
    }

    /**
     * Calculate the euclidean length of all documents in the corpus
     * Result is saved in a hashmap<String, Double>
     * 
     * @return
     */
    private void calculateEuclideanLength(File f) {
        if (f.canRead()) {
            if (f.isDirectory()) {
                String[] fs = f.list();
                // an IO error could occur
                if (fs != null) {
                    for (int i = 0; i < fs.length; i++) {
                        calculateEuclideanLength(new File(f, fs[i]));
                    }
                }
            } else {
                int docID = docCounter++;
                try {
                    Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
                    Tokenizer tok = new Tokenizer(reader, true, false, true, patterns_file);
                    HashMap<String, Integer> termFreqForCurrentDoc = new HashMap<>();
                    while (tok.hasMoreTokens()) {
                        String token = tok.nextToken();
                        termFreqForCurrentDoc.merge(token, 1, Integer::sum);
                    }
                    reader.close();
                    int collectionSize = index.docNames.size();
                    double sumOfResults = 0.0;
                    for (String term : termFreqForCurrentDoc.keySet()) {
                        int termFreq = termFreqForCurrentDoc.get(term);
                        double documentFrequency = indexer.documentFrequency.get(term);
                        double idf = Math.log((double) collectionSize / (double) documentFrequency);
                        sumOfResults += (termFreq * idf) * (termFreq * idf);
                    }
                    searcher.docsEucLength.put(docID, Math.sqrt(sumOfResults));
                } catch (IOException e) {
                    System.err.println("Warning: IOException during indexing.");
                }
            }
        }
    }

    /**
     * Store in a file euclidean length of every word in a doc
     * 
     * @param filenameDir
     * @param docID
     * @param euclideanLength
     */
    private void writeEuclideanLengthToFile() {
        // Write Euclidean length to file
        String fileName = "euclideanLength.txt";
        System.out.println("Writing euclidean length of every doc to a " + fileName);
        // String docTitle = Paths.get(filenameDir).getFileName().toString();
        try (FileWriter writer = new FileWriter(fileName, true)) {
            for (Map.Entry<Integer, Double> entry : searcher.docsEucLength.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing Euclidean length to file: " + e.getMessage());
        }
    }

    /**
     * Reads euclidean length of docs from euclideanLength.txt and stores data in
     * hashmap in searcher
     * 
     * @param fileName
     * @param docIDLengthMap
     */
    public void readDataFromFile() {
        try {
            System.err.println("Reading euclidean length of documents from euclideanLength.txt");
            String fileName = "euclideanLength.txt";
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = reader.readLine()) != null) {
                // Split the line by comma and semicolon
                String[] parts = line.split("[,]");
                int docID = Integer.parseInt(parts[0].trim());
                double length = Double.parseDouble(parts[1].trim());
                searcher.docsEucLength.put(docID, length);
            }
            System.err.println("Finished reading from euclideanLength.txt");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /* ----------------------------------------------- */

    public static void main(String[] args) {
        Engine e = new Engine(args);
    }

}
