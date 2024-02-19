/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

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
        PostingsList searchResults = null;

        if (queryType == queryType.INTERSECTION_QUERY || queryType == queryType.PHRASE_QUERY) {
            searchResults = simpleSearch(query, queryType);
        } else if (queryType == queryType.RANKED_QUERY) {
            searchResults = rankedTfIdf(query.queryterm);
            for (int i = 0; i < searchResults.getEntries().size(); i++) {
                System.out.println(searchResults.getEntries().get(i).docID);
            }
        }
        return searchResults;
    }

    private PostingsList simpleSearch(Query query, QueryType queryType) {
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

    public PostingsList rankResults(Query query) {

        Map<PostingsEntry, Double> scores = new HashMap<>();

        for (int i = 0; i < query.queryterm.size(); i++) {
            String term = query.queryterm.get(i).term;
            PostingsList list = index.getPostings(term);

            for (int j = 0; j < list.size(); j++) {
                PostingsEntry entry = list.get(i);
                double score = tfidf(entry, term);

                scores.merge(entry, score, Double::sum);

                /*
                 * if (!scores.containsKey(entry)) {
                 * scores.put(entry, score);
                 * } else {
                 * scores.put(entry, scores.get(entry) + score);
                 * }
                 */
            }
        }
        PostingsList rankedResults = new PostingsList();

        // ArrayList<String> queryTerms = removeDuplicates(query);

        for (int i = 0; i < query.queryterm.size(); i++) {
            PostingsList termDocs = index.getPostings(query.queryterm.get(i).term);

            for (int j = 0; j < termDocs.size(); j++) {
                PostingsEntry entry = termDocs.get(i);
                int entryIndex = rankedResults.findPostingsEntryIndex(entry);
                if (entryIndex < 0) {
                    rankedResults.addEntry(entry);
                }
            }
        }

        ArrayList<PostingsEntry> matchedDocs = new ArrayList<>(scores.keySet());
        for (int i = 0; i < matchedDocs.size(); i++) {
            PostingsEntry entry = matchedDocs.get(i);
            entry.score = scores.get(entry) / index.docLengths.get(entry.docID);
        }

        rankedResults.setList(matchedDocs);
        rankedResults.sortEntries();
        return rankedResults;
    }

    private PostingsList rankedTfIdf(List<Query.QueryTerm> queryTerms) {
        // Create empty dictionary to hold document scores
        Map<Integer, Double> scores = new HashMap<>();

        // Loop through all search query terms
        for (Query.QueryTerm queryTerm : queryTerms) {
            String term = queryTerm.term;

            // Retrieve documents containing the search term
            PostingsList allDocuments = index.getPostings(term);

            // Loop through all retrieved documents
            for (PostingsEntry document : allDocuments.getEntries()) {
                // Calculate the score for the document for the current query term
                double score = tfidf(document, term);

                // Accumulate the score for the document
                scores.merge(document.docID, score, Double::sum);
            }
        }

        // Create a PostingsList to store the results
        PostingsList results = new PostingsList();

        // Add all documents from scores to the results list
        for (Query.QueryTerm queryTerm : queryTerms) {
            String term = queryTerm.term;
            // Retrieve documents containing the search term
            PostingsList allDocuments = index.getPostings(term);

            for (PostingsEntry document : allDocuments.getEntries()) {
                if (!results.getEntries().contains(document)) {
                    document.score = scores.getOrDefault(document.docID, 0.0) / index.docLengths.get(document.docID);
                    results.getEntries().add(document);
                }
            }
        }

        // Sort the results by score
        results.sortEntries();

        return results;
    }

    public ArrayList<String> removeDuplicates(Query query) {

        ArrayList<String> queryTerms = new ArrayList<>();
        for (int i = 0; i < query.queryterm.size(); i++) {
            queryTerms.add(query.queryterm.get(i).term);
        }

        Set<String> set = new LinkedHashSet<>();

        // Add the elements to set
        set.addAll(queryTerms);

        // Clear the list
        queryTerms.clear();

        // add the elements of set
        // with no duplicates to the list
        queryTerms.addAll(set);
        return queryTerms;
    }

    public double tfidf(PostingsEntry entry, String term) {

        int termFrequency = entry.offset.size();
        int collectionSize = index.docNames.size();
        int documentFrequency = index.getPostings(term).size();

        double idf = Math.log10(collectionSize / documentFrequency);
        return termFrequency * idf;
    }

}