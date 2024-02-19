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
                    if (queryType == queryType.INTERSECTION_QUERY) {
                        answer = intersect(list1, list2);
                    } else if (queryType == queryType.PHRASE_QUERY) {
                        answer = positionalIntersect(list1, list2);
                    }
                } else if (i == 1) {
                    continue;
                } else {
                    PostingsList list = extractPostingList(query, i);
                    if (queryType == queryType.INTERSECTION_QUERY) {
                        answer = intersect(answer, list);
                    } else if (queryType == queryType.PHRASE_QUERY) {
                        answer = positionalIntersect(answer, list);
                    }
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

    private PostingsList positionalIntersect(PostingsList pl1, PostingsList pl2) {
        PostingsList answer = new PostingsList();
        ListIterator<PostingsEntry> itr1 = pl1.getEntries().listIterator();
        ListIterator<PostingsEntry> itr2 = pl2.getEntries().listIterator();

        // Check if both lists have elements
        if (!itr1.hasNext() || !itr2.hasNext())
            return answer;

        PostingsEntry itr1Value = itr1.next();
        PostingsEntry itr2Value = itr2.next();

        while (true) {
            if (itr1Value.docID == itr2Value.docID) {
                ArrayList<Integer> pp1 = itr1Value.offset;
                ArrayList<Integer> pp2 = itr2Value.offset;
                ListIterator<Integer> itrOffset1 = pp1.listIterator();
                ListIterator<Integer> itrOffset2 = pp2.listIterator();

                // Check if both offset lists have elements
                if (!itrOffset1.hasNext() || !itrOffset2.hasNext())
                    break;

                Integer itrOffset1Value = itrOffset1.next();
                Integer itrOffset2Value = itrOffset2.next();

                while (true) {
                    if (itrOffset2Value - itrOffset1Value == 1) {
                        answer.addEntry(itr1Value.docID, itrOffset2Value);
                        if (itrOffset1.hasNext())
                            itrOffset1Value = itrOffset1.next();
                        else
                            break;
                        if (itrOffset2.hasNext())
                            itrOffset2Value = itrOffset2.next();
                        else
                            break;
                    } else if (itrOffset2Value > itrOffset1Value) {
                        if (itrOffset1.hasNext())
                            itrOffset1Value = itrOffset1.next();
                        else
                            break;
                    } else {
                        if (itrOffset2.hasNext())
                            itrOffset2Value = itrOffset2.next();
                        else
                            break;
                    }
                }
                if (itr1.hasNext())
                    itr1Value = itr1.next();
                else
                    break;

                if (itr2.hasNext())
                    itr2Value = itr2.next();
                else
                    break;
            } else if (itr1Value.docID < itr2Value.docID) {
                if (itr1.hasNext())
                    itr1Value = itr1.next();
                else
                    break;
            } else {
                if (itr2.hasNext())
                    itr2Value = itr2.next();
                else
                    break;
            }
        }

        return answer;
    }

    /*
     * private void intersectOffsets(PostingsEntry pe1, PostingsEntry pe2, int
     * docID, PostingsList answer) {
     * // PostingsList answer = new PostingsList();
     * ArrayList<Integer> pp1 = pe1.offset;
     * ArrayList<Integer> pp2 = pe2.offset;
     * 
     * ListIterator<Integer> itrOffset1 = pp1.listIterator();
     * ListIterator<Integer> itrOffset2 = pp2.listIterator();
     * 
     * while (itrOffset1.hasNext() && itrOffset2.hasNext()) {
     * Integer itrOffset1Value = itrOffset1.next();
     * Integer itrOffset2Value = itrOffset2.next();
     * 
     * while (itrOffset1Value != null && itrOffset2Value != null) {
     * if (Math.abs(itrOffset2Value - itrOffset1Value) == 1) {
     * // PostingsEntry entry = new PostingsEntry(docID, itrOffset2Value);
     * // ArrayList<PostingsEntry> list = answer.getEntries();
     * answer.getEntries().add(new PostingsEntry(docID));
     * break;
     * } else if (itrOffset2Value > itrOffset1Value) {
     * if (itrOffset1.hasNext()) {
     * itrOffset1Value = itrOffset1.next();
     * } else {
     * return;
     * }
     * } else {
     * if (itrOffset2.hasNext()) {
     * itrOffset2Value = itrOffset2.next();
     * } else {
     * return;
     * }
     * }
     * }
     * }
     * // return answer;
     * }
     */

}