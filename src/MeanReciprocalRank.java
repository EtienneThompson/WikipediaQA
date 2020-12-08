import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

/**
 * Class used to calculate a system's MPR score.
 * @author etiennethompson
 */
public class MeanReciprocalRank {
	private IndexSearcher searcher;
	private int[] answers;
	private int index;
	private int totalQueries;
	
	/**
	 * Initialize variables.
	 */
	public MeanReciprocalRank() {
		this.searcher = null;
		this.answers = new int[100];
		this.index = 0;
		this.totalQueries = 0;
	}
	
	/**
	 * Set which searcher is used for querying.
	 * 
	 * The searcher is needed for getting information from the documents Lucene returns from it's searches.
	 * 
	 * @param searcher The IndexSearcher used by QueryMachine.
	 */
	public void setSearcher(IndexSearcher searcher) {
		if (this.searcher == null) {
			this.searcher = searcher;
		}
	}
	
	/**
	 * Find where in the top 100 documents the answer is and record that position in answers.
	 * @param hits The list of top 100 ranked documents returned by the searcher.
	 * @param answer The answer for those documents specified in the questions file.
	 */
	public void add(ScoreDoc[] hits, String answer) {
		// Iterate through each file returned.
		for (int i = 0; i < hits.length; i++) {
			try {
				// Iterate through all the different answers.
				String[] parts = answer.split("\\|");
				for (String part : parts) {
					// If we find an answer, then record it's position.
					if (this.searcher.doc(hits[i].doc).get("title").equals(part)) {
						this.answers[this.index] = i + 1;
						this.index++;
						this.totalQueries++;
						return;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// If we get out of the for loop, the answer was not in the hits.
		this.answers[this.index] = 0;
		this.index++;
		this.totalQueries++;
	}

	/**
	 * Run the traditional MPR algorithm over the currently recorded answers.
	 * @return double representing the calculated score.
	 */
	public double calculate() {
		if (this.totalQueries == 0) {
			// If we haven't recorded any answer positions, return.
			return 0.0;
		}
		// Perform the inverse summation.
		double summation = 0.0;
		for (int index : this.answers) {
			if (index != 0) {
				summation += (1.0 / index);
			}
		}
		// Divide by the total number of queries.
		return summation / this.totalQueries;
	}
}
