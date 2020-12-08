import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

public class MeanReciprocalRank {
	private IndexSearcher searcher;
	private int[] answers;
	private int index;
	
	public MeanReciprocalRank() {
		this.searcher = null;
		this.answers = new int[100];
		this.index = 0;
	}
	
	public void setSearcher(IndexSearcher searcher) {
		if (this.searcher == null) {
			this.searcher = searcher;
		}
	}
	
	public void add(ScoreDoc[] hits, String answer) {
		for (int i = 0; i < hits.length; i++) {
			try {
				String[] parts = answer.split("\\|");
				for (String part : parts) {
					if (this.searcher.doc(hits[i].doc).get("title").equals(part)) {
						this.answers[this.index] = i + 1;
						this.index++;
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
	}

	public double calculate() {
		double summation = 0.0;
		for (int index : this.answers) {
			if (index != 0) {
				summation += (1.0 / index);
			}
		}
		return summation / 100;
	}
}
