import java.util.*;
import java.io.*;

public class PageRank {

	/**
	 * Maximal number of documents. We're assuming here that we
	 * don't have more docs than we can keep in main memory.
	 */
	final static int MAX_NUMBER_OF_DOCS = 2000000;

	/**
	 * Mapping from document names to document numbers.
	 */
	HashMap<String, Integer> docNumber = new HashMap<String, Integer>();

	/**
	 * Mapping from document numbers to document names
	 */
	String[] docName = new String[MAX_NUMBER_OF_DOCS];

	/**
	 * A memory-efficient representation of the transition matrix.
	 * The outlinks are represented as a HashMap, whose keys are
	 * the numbers of the documents linked from.
	 * <p>
	 *
	 * The value corresponding to key i is a HashMap whose keys are
	 * all the numbers of documents j that i links to.
	 * <p>
	 *
	 * If there are no outlinks from i, then the value corresponding
	 * key i is null.
	 */
	HashMap<Integer, HashMap<Integer, Boolean>> link = new HashMap<Integer, HashMap<Integer, Boolean>>();

	/**
	 * Contains the pagerank probabilities of every document in a
	 * collection.
	 */
	Map<String, Double> pageRankProbs = new HashMap<>();

	/**
	 * Map docID to docTitle
	 */
	HashMap<String, String> idToTitle = new HashMap<>();

	/**
	 * The number of outlinks from each node.
	 */
	int[] out = new int[MAX_NUMBER_OF_DOCS];

	/**
	 * The probability that the surfer will be bored, stop
	 * following links, and take a random jump somewhere.
	 */
	final static double BORED = 0.15;

	/**
	 * Convergence criterion: Transition probabilities do not
	 * change more that EPSILON from one iteration to another.
	 */
	final static double EPSILON = 0.0001;

	/* --------------------------------------------- */

	public PageRank(String filename) {
		int noOfDocs = readDocs(filename);
		iterate(noOfDocs, 1000);
		// readDocumentNames("davisTitles.txt");
		// writePageRankToFile();

	}

	/* --------------------------------------------- */

	/**
	 * Reads the documents and fills the data structures.
	 *
	 * @return the number of documents read.
	 */
	int readDocs(String filename) {
		int fileIndex = 0;
		try {
			System.err.print("Reading file... ");
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS) {
				int index = line.indexOf(";");
				String title = line.substring(0, index);
				Integer fromdoc = docNumber.get(title);
				// Have we seen this document before?
				if (fromdoc == null) {
					// This is a previously unseen doc, so add it to the table.
					fromdoc = fileIndex++;
					docNumber.put(title, fromdoc);
					docName[fromdoc] = title;
				}
				// Check all outlinks.
				fileIndex = getTok(fileIndex, line, index, fromdoc);
			}
			if (fileIndex >= MAX_NUMBER_OF_DOCS) {
				System.err.print("stopped reading since documents table is full. ");
			} else {
				System.err.print("done. ");
			}
		} catch (FileNotFoundException e) {
			System.err.println("File " + filename + " not found!");
		} catch (IOException e) {
			System.err.println("Error reading file " + filename);
		}
		System.err.println("Read " + fileIndex + " number of documents");
		return fileIndex;
	}

	private int getTok(int fileIndex, String line, int index, Integer fromdoc) {
		StringTokenizer tok = new StringTokenizer(line.substring(index + 1), ",");
		while (tok.hasMoreTokens() && fileIndex < MAX_NUMBER_OF_DOCS) {
			String otherTitle = tok.nextToken();
			Integer otherDoc = docNumber.get(otherTitle);
			if (otherDoc == null) {
				// This is a previousy unseen doc, so add it to the table.
				otherDoc = fileIndex++;
				docNumber.put(otherTitle, otherDoc);
				docName[otherDoc] = otherTitle;
			}
			// Set the probability to 0 for now, to indicate that there is
			// a link from fromdoc to otherDoc.
			if (link.get(fromdoc) == null) {
				link.put(fromdoc, new HashMap<Integer, Boolean>());
			}
			if (link.get(fromdoc).get(otherDoc) == null) {
				link.get(fromdoc).put(otherDoc, true);
				out[fromdoc]++;
			}
		}
		return fileIndex;
	}

	/**
	 * Read davisTitles.txt to map docID to docTitle in idToTitle
	 * 
	 * @param filename
	 */
	void readDocumentNames(String filename) {
		int fileIndex = 0;
		try {
			System.err.print("Reading davisTitles.txt... ");
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS) {
				int index = line.indexOf(";");
				String id = line.substring(0, index);
				String title = line.substring(index + 1);
				idToTitle.put(id, title);
			}
			if (fileIndex >= MAX_NUMBER_OF_DOCS) {
				System.err.print("stopped reading since documents table is full. ");
			} else {
				System.err.print("done. ");
			}
		} catch (FileNotFoundException e) {
			System.err.println("File " + filename + " not found!");
		} catch (IOException e) {
			System.err.println("Error reading file " + filename);
		}
		System.err.println("Read " + fileIndex + " number of documents");
	}

	/**
	 * Write pageRankedProb to file
	 */
	void writePageRankToFile() {
		BufferedWriter bufferedWriter = null;
		try {
			bufferedWriter = new BufferedWriter(new FileWriter("pageRankProb.txt"));
			for (Map.Entry<String, Double> entry : pageRankProbs.entrySet()) {
				String docTitle = idToTitle.get(entry.getKey());
				bufferedWriter.write(docTitle + "=" + entry.getValue());
				bufferedWriter.newLine(); // Move to the next line
				bufferedWriter.flush();
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				bufferedWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/* --------------------------------------------- */

	/*
	 * Chooses a probability vector a, and repeatedly computes
	 * aP, aP^2, aP^3... until aP^i = aP^(i+1).
	 */
	void iterate(int numberOfDocs, int maxIterations) {

		// YOUR CODE HERE
		double[] currentStat = new double[numberOfDocs];
		double[] nextStat = new double[numberOfDocs];

		for (int iteration = 1; iteration <= maxIterations; iteration++) {
			if (iteration == 1) {
				currentStat[0] = 1.0;
			} else {
				currentStat = Arrays.copyOf(nextStat, nextStat.length);
			}
			for (int i = 0; i < numberOfDocs; i++) {
				double result = 0.0;
				for (int j = 0; j < numberOfDocs; j++) {
					if (link.get(j) == null) {
						result += currentStat[j] * calculateP(numberOfDocs, numberOfDocs);
					} else {
						if (link.get(j).containsKey(i)) {
							result += currentStat[j] * calculateP(out[j], numberOfDocs);
						} else {
							result += currentStat[j] * (BORED * (1.0 / numberOfDocs));
						}
					}
				}
				nextStat[i] = result;
			}
			// nextStat = normalize(nextStat);
			if (calculateManhattanNorm(currentStat, nextStat) < EPSILON) {
				System.out.println("Convergence occured after " + iteration + " iterations.");
				break;
			}
		}
		getSortedProbs(numberOfDocs, nextStat);
	}

	/**
	 * Sort the probabilities of the caluclated pageRanks
	 * 
	 * @param numberOfDocs
	 * @param nextStat
	 */
	private void getSortedProbs(int numberOfDocs, double[] nextStat) {
		for (int i = 0; i < numberOfDocs; i++) {
			pageRankProbs.put(docName[i], nextStat[i]);
		}

		List<Map.Entry<String, Double>> sortedProbs = new ArrayList<>(pageRankProbs.entrySet());
		sortedProbs.sort(Map.Entry.comparingByValue());
		Collections.reverse(sortedProbs);

		int counter = 0;
		for (Map.Entry<String, Double> entry : sortedProbs) {
			System.out.println(entry.getKey() + ": " + entry.getValue());
			counter++;
			if (counter == 30)
				break;
		}
	}

	/**
	 * Normalize the stationary array after every iteration
	 * 
	 * @param nextStat
	 * @return
	 */
	public double[] normalize(double[] nextStat) {
		double sum = 0.0;
		double[] newNextStat = nextStat;
		for (int i = 0; i < newNextStat.length; i++) {
			sum += newNextStat[i];
		}
		if (sum != 1.0) {
			System.out.println("Array normalized bc sum is " + sum);
			for (int i = 0; i < newNextStat.length; i++) {
				newNextStat[i] = newNextStat[i] / sum;
			}
		}
		return newNextStat;
	}

	/**
	 * Calculate the entries of the transition matrix
	 * 
	 * @param numlinks
	 * @param numberOfDocs
	 * @return
	 */
	private double calculateP(int numlinks, int numberOfDocs) {
		return (((1 - BORED) * (1.0 / numlinks))) + BORED * (1.0 / numberOfDocs);
	}

	/**
	 * Calculate the difference between the old and new stationary arrays
	 * after every iteration
	 * 
	 * @param currentStat
	 * @param nextStat
	 * @return
	 */
	private double calculateManhattanNorm(double[] currentStat, double[] nextStat) {
		double diff = 0.0;
		for (int i = 0; i < currentStat.length; i++) {
			diff += Math.abs(nextStat[i] - currentStat[i]);
		}
		return diff;
	}

	/**
	 * Print the transition matrix
	 * 
	 * @param map
	 */
	public static void printHashMap(HashMap<Integer, HashMap<Integer, Boolean>> map) {
		for (Map.Entry<Integer, HashMap<Integer, Boolean>> entry : map.entrySet()) {
			Integer key1 = entry.getKey();
			HashMap<Integer, Boolean> innerMap = entry.getValue();
			for (Map.Entry<Integer, Boolean> innerEntry : innerMap.entrySet()) {
				Integer key2 = innerEntry.getKey();
				Boolean value = innerEntry.getValue();
				System.out.println("Key1: " + key1 + ", Key2: " + key2 + ", Value: " + value);
			}
		}
	}

	public static void printHashMapElements(HashMap<String, Integer> hashMap) {
		// Iterate over the entries of the HashMap and print key-value pairs
		for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
			System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
		}
	}

	/* --------------------------------------------- */

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		if (args.length != 1) {
			System.err.println("Please give the name of the link file");
		} else {
			new PageRank(args[0]);
			long endTime = System.currentTimeMillis();
			double execTime = (endTime - startTime) / 1000.0;
			System.out.println("exec time in seconds: " + execTime);
		}
	}
}