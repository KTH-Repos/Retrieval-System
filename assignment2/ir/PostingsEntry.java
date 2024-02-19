/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;
    public ArrayList<Integer> offset = new ArrayList<>();

    /**
     * PostingsEntries are compared by their score (only relevant
     * in ranked retrieval).
     *
     * The comparison is defined so that entries will be put in
     * descending order.
     */

    public PostingsEntry(int docID, int offset) {
        this.docID = docID;
        this.offset.add(offset);
    }

    public PostingsEntry(int docID) {
        this.docID = docID;
    }

    public int compareTo(PostingsEntry other) {
        return Double.compare(other.score, score);
    }

    //
    // YOUR CODE HERE
    //

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("docID=").append(docID);
        sb.append(", score=").append(score);
        // sb.append(", offsets=").append(offset.toString());
        sb.append("Offsets: [");
        for (int i = 0; i < offset.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(offset.get(i).toString());
        }
        sb.append("]");
        return sb.toString();
    }
}
