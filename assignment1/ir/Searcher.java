/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.ListIterator;

import ir.Query.QueryTerm;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    /** Constructor */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     * Searches the index for postings matching the query.
     * 
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType, NormalizationType normType) {
        //
        // REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        if (query.queryterm.size() < 2) { // empty or one term
            return extractPostingList(query, 0);
        } else {
            PostingsList answer = new PostingsList();
            for (int i = 0; i < query.queryterm.size(); i++) {
                if (i == 0) {
                    PostingsList list1 = extractPostingList(query, 0);
                    PostingsList list2 = extractPostingList(query, 1);
                    answer = intersect(list1, list2);
                } else if (i == 1) {
                    continue;
                } else {
                    PostingsList list = extractPostingList(query, i);
                    answer = intersect(answer, list);
                }
            }
            return answer;
        }
    }

    private PostingsList extractPostingList(Query query, int termIndex) {
        return index.getPostings(query.queryterm.get(termIndex).term);
    }

    public PostingsList intersect(PostingsList pl1, PostingsList pl2) {
        PostingsList answer = new PostingsList();
        ListIterator<PostingsEntry> itr1 = pl1.getEntries().listIterator();
        ListIterator<PostingsEntry> itr2 = pl2.getEntries().listIterator();

        while (itr1.hasNext() && itr2.hasNext()) {
            PostingsEntry pe1 = itr1.next();
            PostingsEntry pe2 = itr2.next();

            while (true) {
                if (pe1.docID == pe2.docID) {
                    answer.getEntries().add(new PostingsEntry(pe1.docID));
                    break;
                } else if (pe1.docID < pe2.docID && itr1.hasNext()) {
                    pe1 = itr1.next();
                } else if (pe1.docID > pe2.docID && itr2.hasNext()) {
                    pe2 = itr2.next();
                } else {
                    return answer; // No common elements left
                }
            }
        }

        return answer;
    }

}