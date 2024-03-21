/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class KGramIndex {

    /** Mapping from term ids to actual term strings */
    HashMap<Integer, String> id2term = new HashMap<Integer, String>();

    /** Mapping from term strings to term ids */
    HashMap<String, Integer> term2id = new HashMap<String, Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    HashMap<String, List<KGramPostingsEntry>> index = new HashMap<String, List<KGramPostingsEntry>>();

    /** The ID of the last processed term */
    int lastTermID = -1;

    /** Number of symbols to form a K-gram */
    int K = 3;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }

    /**
     * Get intersection of two postings lists
     */
    private List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {
        //
        // YOUR CODE HERE
        //
        List<KGramPostingsEntry> answer = new ArrayList<>();

        if (p1 == null || p2 == null) {
            return answer;
        }

        ListIterator<KGramPostingsEntry> itr1 = p1.listIterator();
        ListIterator<KGramPostingsEntry> itr2 = p2.listIterator();

        while (itr1.hasNext() && itr2.hasNext()) {
            KGramPostingsEntry pe1 = itr1.next();
            KGramPostingsEntry pe2 = itr2.next();

            while (true) {
                if (pe1.tokenID == pe2.tokenID) {
                    answer.add(new KGramPostingsEntry(pe1.tokenID));
                    break;
                } else if (pe1.tokenID < pe2.tokenID && itr1.hasNext()) {
                    pe1 = itr1.next();
                } else if (pe1.tokenID > pe2.tokenID && itr2.hasNext()) {
                    pe2 = itr2.next();
                } else {
                    return answer; // No common elements left
                }
            }
        }
        return answer;
    }

    /** Inserts all k-grams from a token into the index. */
    public void insert(String token) {
        //
        // YOUR CODE HERE
        //
        if (getIDByTerm(token) == null) {
            int tokenID = generateTermID();
            term2id.put(token, tokenID);
            id2term.put(tokenID, token);
            KGramPostingsEntry kEntry = new KGramPostingsEntry(tokenID);
            String newToken = "^" + token + "$";

            int numOfKGrams = token.length() + 3 - getK();
            String kGramsHolder = "";

            for (int i = 0; i < numOfKGrams; i++) {

                kGramsHolder = newToken.substring(i, i + getK());

                if (!index.containsKey(kGramsHolder)) {
                    index.put(kGramsHolder, new ArrayList<KGramPostingsEntry>());
                }

                if (!index.get(kGramsHolder).contains(kEntry)) {
                    index.get(kGramsHolder).add(kEntry);
                }
            }
        }
    }

    /** Get postings for the given k-gram */
    public List<KGramPostingsEntry> getPostings(String kgram) {
        //
        // YOUR CODE HERE
        //
        if (index.containsKey(kgram)) {
            return index.get(kgram);
        }
        return null;
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }

    public void findKGrams(String[] kgrams) {
        List<KGramPostingsEntry> answer = null;

        for (int i = 0; i < kgrams.length; i++) {
            if (kgrams[i].length() != getK()) {
                System.out.println("K = " + getK() + "doesn't match with length of this input : " + kgrams[i]);
                return;
            }

            if (i == 0) {
                answer = getPostings(kgrams[i]);
            } else {
                answer = intersect(answer, getPostings(kgrams[i]));
            }
        }
        if (answer == null) {
            System.out.println("No posting(s) found!");
        } else {
            System.out.println("Found " + answer.size() + " postings(s) for " + Arrays.toString(kgrams));
        }
    }

    private static HashMap<String, String> decodeArgs(String[] args) {
        HashMap<String, String> decodedArgs = new HashMap<String, String>();
        int i = 0, j = 0;
        while (i < args.length) {
            if ("-p".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("patterns_file", args[i++]);
                }
            } else if ("-f".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("file", args[i++]);
                }
            } else if ("-k".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("k", args[i++]);
                }
            } else if ("-kg".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("kgram", args[i++]);
                }
            } else {
                System.err.println("Unknown option: " + args[i]);
                break;
            }
        }
        return decodedArgs;
    }

    public static void main(String[] arguments) throws FileNotFoundException, IOException {
        HashMap<String, String> args = decodeArgs(arguments);

        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        KGramIndex kgIndex = new KGramIndex(k);

        File f = new File(args.get("file"));
        Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
        Tokenizer tok = new Tokenizer(reader, true, false, true, args.get("patterns_file"));
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            kgIndex.insert(token);
        }

        String[] kgrams = args.get("kgram").split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != k) {
                System.err.println(
                        "Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
                System.exit(1);
            }

            if (postings == null) {
                postings = kgIndex.getPostings(kgram);
            } else {
                postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
            }
        }
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }
    }
}
