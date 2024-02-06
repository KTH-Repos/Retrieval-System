/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

    /** The index as a hashtable. */
    private HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();

    /**
     * Inserts this token in the hashtable.
     */
    public void insert(String token, int docID, int offset) {
        //
        // YOUR CODE HERE
        //

        // For every token, there will be an associated postingList
        // One postinglist wll contain each and every docID where the token exists.

        if (!index.containsKey(token)) {
            PostingsList list = new PostingsList(docID, offset);
            index.put(token, list);
        } else {
            PostingsList list = getPostings(token);
            list.addEntry(docID, offset);
        }
    }

    /**
     * Returns the postingslist for a specific token or null
     * if the term is not in the index.
     */
    public PostingsList getPostings(String token) {
        //
        // REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        return index.get(token);
    }

    /**
     * No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
