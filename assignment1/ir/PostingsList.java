/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;

public class PostingsList {

    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

    /**
     * Create a new PostingsList with one PostingsEntry
     * 
     * @param docID
     * @param offset
     */
    public PostingsList(int docID, int offset) {
        list.add(new PostingsEntry(docID, offset));
    }

    /**
     * Insert a new entry to the list if the list already exists
     * 
     * @param docID
     * @param offset
     */
    public void addEntry(int docID, int offset) {

        PostingsEntry entry = findPostingsEntry(docID);
        if (entry != null) { // it already exists
            entry.offset.add(offset);
        } else {
            list.add(new PostingsEntry(docID, offset));
        }
    }

    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get(int i) {
        return list.get(i);
    }

    //
    // YOUR CODE HERE
    //

    public PostingsEntry findPostingsEntry(int targetDocID) {
        for (PostingsEntry entry : list) {
            if (entry.docID == targetDocID) {
                return entry; // Found the desired PostingsEntry
            }
        }
        return null; // Not found
    }
}
