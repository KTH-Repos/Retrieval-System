/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
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

    Map<String, Double> pagedRankProb = new HashMap<>();

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

        getKGrams(query);

        //-----------------------------------------------------------//

        PostingsList searchResults = null;

        switch (queryType) {
            case INTERSECTION_QUERY:
                searchResults = simpleSearch(query, queryType);
                break;

            case PHRASE_QUERY:
                searchResults = simpleSearch(query, queryType);
                break;

            case RANKED_QUERY:
                if (pagedRankProb.isEmpty())
                    readPagedRank(); 
                if (rankingType == rankingType.TF_IDF)
                    searchResults = rankedTfIdf(query.queryterm);

                else if (rankingType == rankingType.PAGERANK)
                    searchResults = rankedPageRank(query.queryterm);

                else if (rankingType == rankingType.COMBINATION)
                    searchResults = combinedPagedRank(query.queryterm);
                break;
        }
        return searchResults;
    }

    private void getKGrams(Query query) {
        String[] kGrams = new String[query.queryterm.size()];
        for(int i = 0; i < query.queryterm.size(); i++) {
            kGrams[i] = query.queryterm.get(i).term;
        }
        kgIndex.findKGrams(kGrams);
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

    private PostingsList rankedTfIdf(List<Query.QueryTerm> queryTerms) {
        // Create empty dictionary to hold document scores
        Map<Integer, Double> scores = new HashMap<>();

        // Loop through all search query terms
        for (Query.QueryTerm queryTerm : queryTerms) {
            String term = queryTerm.term;
            // Retrieve documents containing the search term
            PostingsList allDocuments = index.getPostings(term);
            if(allDocuments == null) {
                continue;
            }
            // Loop through all retrieved documents
            for (PostingsEntry document : allDocuments.getEntries()) {
                // Calculate the score for the document for the current query term
                double score = tfidf(document, term);

                // Accumulate the score for the document
                scores.merge(document.docID, score, Double::sum);
            }
        }

        // Create a set to keep track of unique documents
        Set<Integer> uniqueDocs = new HashSet<>();

        // Create a PostingsList to store the results
        PostingsList results = new PostingsList();

        // Add all documents from scores to the results list
        for (Query.QueryTerm queryTerm : queryTerms) {
            String term = queryTerm.term;
            // Retrieve documents containing the search term
            PostingsList allDocuments = index.getPostings(term);
            
            if(allDocuments == null) {
                continue;
            }
            for (PostingsEntry document : allDocuments.getEntries()) {
                if (!uniqueDocs.contains(document.docID)) {
                    document.score = scores.getOrDefault(document.docID, 0.0) / index.docLengths.get(document.docID);
                    results.getEntries().add(document);
                    uniqueDocs.add(document.docID); // Add document to the set
                }
            }
        }
        // Sort the results by score
        results.sortEntries();
        return results;
    }

    public double tfidf(PostingsEntry entry, String term) {

        int termFrequency = entry.offset.size();
        int collectionSize = index.docNames.size();
        int documentFrequency = index.getPostings(term).size();

        double idf = Math.log10(collectionSize / documentFrequency);
        return termFrequency * idf;
    }

    public PostingsList rankedPageRank(List<Query.QueryTerm> queryTerms) {
        for (Query.QueryTerm queryTerm : queryTerms) {
            String term = queryTerm.term;

            // Retrieve documents containing the search term
            PostingsList allDocuments = index.getPostings(term);

            // Loop through all retrieved documents
            for (PostingsEntry document : allDocuments.getEntries()) {
                String docTitle = Index.docNames.get(document.docID);
                docTitle = docTitle.substring(docTitle.lastIndexOf("\\") + 1);
                document.score = pagedRankProb.get(docTitle);
            }
        }
        // Create a set to keep track of unique documents
        Set<Integer> uniqueDocs = new HashSet<>();

        PostingsList results = new PostingsList();

        for (Query.QueryTerm queryTerm : queryTerms) {
            String term = queryTerm.term;
            // Retrieve documents containing the search term
            PostingsList allDocuments = index.getPostings(term);

            for (PostingsEntry document : allDocuments.getEntries()) {
                if (!uniqueDocs.contains(document.docID)) {
                    results.getEntries().add(document);
                    uniqueDocs.add(document.docID); // Add document to the set
                }
            }
        }
        results.sortEntries();
        return results;
    }

    public PostingsList combinedPagedRank(List<Query.QueryTerm> queryTerms) {
        // Create empty dictionary to hold document scores
        Map<Integer, Double> scores = new HashMap<>();
        // Loop through all search query terms
        for (Query.QueryTerm queryTerm : queryTerms) {
            String term = queryTerm.term;

            // Retrieve documents containing the search term
            PostingsList allDocuments = index.getPostings(term);
            if(allDocuments == null) {
                continue;
            }
            // Loop through all retrieved documents
            for (PostingsEntry document : allDocuments.getEntries()) {
                // Calculate the score for the document for the current query term
                double score = tfidf(document, term);

                // Accumulate the score for the document
                scores.merge(document.docID, score, Double::sum);
            }
        }
        // Create a set to keep track of unique documents
        Set<Integer> uniqueDocs = new HashSet<>();

        // Create a PostingsList to store the results
        PostingsList results = new PostingsList();

        double weight = 0.005;
        // Add all documents from scores to the results list
        for (Query.QueryTerm queryTerm : queryTerms) {
            String term = queryTerm.term;
            // Retrieve documents containing the search term
            PostingsList allDocuments = index.getPostings(term);
            if(allDocuments == null) {
                continue;
            }

            for (PostingsEntry document : allDocuments.getEntries()) {
                if (!uniqueDocs.contains(document.docID)) {
                    double tdfScore = scores.getOrDefault(document.docID, 0.0) / index.docLengths.get(document.docID);
                    String docTitle = Index.docNames.get(document.docID);
                    docTitle = docTitle.substring(docTitle.lastIndexOf("\\") + 1);
                    double pageRankScore = pagedRankProb.getOrDefault(docTitle, 0.0);
                    document.score = (1 - weight) * pageRankScore + weight * tdfScore;
                    results.getEntries().add(document);
                    uniqueDocs.add(document.docID); // Add document to the set
                }
            }
        }
        results.sortEntries();
        return results;
    }

    public void readPagedRank() {
        try {
            System.err.println("Reading pagedRankProb to search engine... ");
            BufferedReader reader = new BufferedReader(new FileReader("pagerank/pageRankProb.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("="); // Split line by '='
                String title = parts[0].trim(); // Get the title
                double probability = Double.parseDouble(parts[1].trim()); // Get the probability
                pagedRankProb.put(title, probability); // Put title and probability into HashMap
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}