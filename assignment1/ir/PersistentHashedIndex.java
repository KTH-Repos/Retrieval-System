/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */

package ir;

import java.io.*;
import java.net.IDN;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.*;

/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = INDEXDIR + "/dictionary";

    /** The data file name */
    public static final String DATA_FNAME = INDEXDIR + "/data";

    /** The tokens file name */
    public static final String tokenS_FNAME = "tokens";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();

    ArrayList<Long> usedHashes = new ArrayList<>();

    private final int ENTRYBYTESIZE = 16;

    // size of index in hashedindex is 195634
    // ===================================================================

    /**
     * A helper class representing one entry in the dictionary hashtable.
     */
    public class Entry {
        //
        // YOUR CODE HERE
        //
        long pointer; // pointer to postinglist
        char firstChar;
        char lastChar;
        int size; // size of postingslist in bytes

        public Entry(long pointer, int size, char firstChar, char lastChar) {
            this.pointer = pointer;
            this.firstChar = firstChar;
            this.lastChar = lastChar;
            this.size = size;
        }
    }

    // ==================================================================

    /**
     * Constructor. Opens the dictionary file and the data file.
     * If these files don't exist, they will be created.
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile(DICTIONARY_FNAME, "rw");
            dataFile = new RandomAccessFile(DATA_FNAME, "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Writes data to the data file at a specified place.
     *
     * @return The number of bytes written.
     */
    int writeData(String dataString, long ptr) {
        try {
            dataFile.seek(ptr);
            byte[] data = dataString.getBytes();
            dataFile.write(data);
            return data.length;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Reads data from the data file
     */
    String readData(long ptr, int size) {
        try {
            dataFile.seek(ptr);
            byte[] data = new byte[size];
            dataFile.readFully(data);
            return new String(data);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ==================================================================
    //
    // Reading and writing to the dictionary file.

    /*
     * Writes an entry to the dictionary hash table file.
     *
     * @param entry The key of this entry is assumed to have a fixed length
     * 
     * @param ptr The place in the dictionary file to store the entry
     */
    void writeEntry(Entry entry, long ptr) throws IOException {
        //
        // YOUR CODE HERE
        //
        dictionaryFile.seek(ptr);
        dictionaryFile.writeChar(entry.firstChar);
        dictionaryFile.writeChar(entry.lastChar);
        dictionaryFile.writeLong(entry.pointer);
        dictionaryFile.writeInt(entry.size);

    }

    /**
     * Reads an entry from the dictionary file.
     *
     * @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry(long ptr, String term) {
        //
        // REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        long originalPtr = ptr;
        while(true) {
            try {
                dictionaryFile.seek(ptr);
                if(dictionaryFile.readChar() == term.charAt(0)){
                    Character firstchar = term.charAt(0);
                    Character secChar = dictionaryFile.readChar();
                    // Check if the second character matches the token
                    if (term.length() > 1 && secChar == term.charAt(1)) {
                        // Read additional data and return an Entry
                        long dataPtr = dictionaryFile.readLong();
                        int size = dictionaryFile.readInt();
                        return new Entry(dataPtr, size, firstchar, secChar);
                    }
                    
                    // Check if the second character is a wildcard '*'
                    if (term.length() == 1 && secChar == '-') {
                        // Read additional data and return an Entry
                        long dataPtr = dictionaryFile.readLong();
                        int size = dictionaryFile.readInt();
                        return new Entry(dataPtr, size, firstchar, '-');
                    }
                }

                long hash = ptr/ENTRYBYTESIZE;
                ptr = ((hash+1
                )%TABLESIZE)*ENTRYBYTESIZE;
                dictionaryFile.seek(ptr);
                if(ptr == originalPtr || dictionaryFile.readChar() == '\0' || dictionaryFile.readChar() == ' ')
                    return null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ==================================================================

    /**
     * Writes the document names and document lengths to file.
     *
     * @throws IOException { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream(INDEXDIR + "/docInfo");
        for (Map.Entry<Integer, String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }

    /**
     * Reads the document names and document lengths from file, and
     * put them in the appropriate data structures.
     *
     * @throws IOException { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File(INDEXDIR + "/docInfo");
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(new Integer(data[0]), data[1]);
                docLengths.put(new Integer(data[0]), new Integer(data[2]));
            }
        }
        freader.close();
    }

    /**
     * Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list

            //
            // YOUR CODE HERE
            //
            for (String token : index.keySet()) {
                long dataFilePointer = dataFile.getFilePointer();
                PostingsList pl = index.get(token);
                String postingsListString = pl.toString();
                int size = writeData(postingsListString, dataFilePointer);
                long pointer = hash(token);
                if (usedHashes.contains(pointer)) {
                    pointer = findNewHash(pointer);
                    collisions++;
                }
                usedHashes.add(pointer);
                if(token.length() > 1) {
                    Entry entry = new Entry(dataFilePointer, size, token.charAt(0), token.charAt(1));
                    writeEntry(entry, pointer);
                }else {
                    Entry entry = new Entry(dataFilePointer, size, token.charAt(0), '*');
                    writeEntry(entry, pointer);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println(collisions + " collisions.");
    }

    // ==================================================================

    /**
     * Returns the postings for a specific token, or null
     * if the token is not in the index.
     */
    public PostingsList getPostings(String token) {
        //
        // REPLACE THE STATEMENT BELOW WITH YOUR CODE
        // ed
        PostingsList postingsList = index.get(token);
        if(postingsList == null) {
            long hash = hash(token);
            Entry entry = readEntry(hash, token);
            if(entry != null) {
                String postingsListString = readData(entry.pointer, entry.size);
                postingsList = getPostingsList(postingsListString);
                index.put(token, postingsList);
            }
        }
        
        return postingsList;
    }

    public boolean isValidInteger(String str) {
        try {
            // Attempt to parse the string as an integer
            Integer.parseInt(str.trim());
            // If parsing succeeds, return true
            return true;
        } catch (NumberFormatException e) {
            // If parsing fails, return false
            return false;
        }
    }
    

    public PostingsList getPostingsList(String encodedString) {

        PostingsList postingsList = new PostingsList();
        // Split the encoded string by semi-colons to get individual entries
        encodedString = encodedString.substring(0, encodedString.length()-1);
        
        String[] entries = encodedString.split(";");
    
        // Process each entry
        for (String entry : entries) {
            // Split the entry by "/" to separate docID and the rest
            String[] parts = entry.split("/");   
            if(!parts[0].trim().isEmpty() && !isValidInteger(parts[0].trim()) && parts[0].trim().equals("")) {
                continue;
            }
            int docID = Integer.parseInt(parts[0].trim()); // Trim before parsing
            String rest = parts[1].trim(); // Trim after splitting
    
            // Split the rest by ":" to separate score and offsets
            String[] parts2 = rest.split(":");
            double score = Double.parseDouble(parts2[0].trim()); // Trim before parsing
            // Extract offsets and add them to an ArrayList
            String[] offsetStrings = parts2[1].trim().split(","); // Trim after splitting
            ArrayList<Integer> offsets = new ArrayList<>();
            for (String offsetStr : offsetStrings) {
                offsets.add(Integer.parseInt(offsetStr.trim())); // Trim before parsing
            }
    
            // Create a new PostingsEntry and add it to the postingsList
            PostingsEntry postingsEntry = new PostingsEntry(docID, score, offsets);
            postingsList.addPersistedEntry(postingsEntry);
        }
        return postingsList;
    }
    
    /**
     * Inserts this token in the main-memory hashtable.
     */
    public void insert(String token, int docID, int offset) {
        //
        // YOUR CODE HERE
        //
        if (!index.containsKey(token)) {
            PostingsList list = new PostingsList(docID, offset);
            index.put(token, list);
        } else {
            PostingsList list = index.get(token);
            list.addEntry(docID, offset);
        }
    }

    public long hash(String token) {
        return (Math.abs(token.hashCode())% TABLESIZE) * ENTRYBYTESIZE;
    }

    public long findNewHash(long pointer) {
        long hash = pointer/ENTRYBYTESIZE;
        do {
            hash = (hash+1)%TABLESIZE;
            pointer = (hash)*ENTRYBYTESIZE;
        } while(usedHashes.contains(pointer));
        return pointer;
    }

    /**
     * Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println(index.keySet().size() + " unique words");
        System.err.print("Writing index to disk...");
        writeIndex();
        System.err.println("done!");
    }
}



