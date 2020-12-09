import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;

/**
 * Handles querying an index.
 * @author etiennethompson
 */
public class QueryMachine {
	private Index index;
	private MeanReciprocalRank mrr;
	private BufferedWriter writer;
	private IndexReader reader;
	private IndexSearcher searcher;
	private String method;
	private boolean category;
	private boolean bm25;
	
	/**
	 * Initialize the searching objects and MeanReciprocalRank object.
	 * @param index
	 * @param method
	 * @param category
	 * @param bm25
	 */
	public QueryMachine(Index index, String method, boolean category, boolean bm25) {
		this.index = index;
		this.mrr = new MeanReciprocalRank();
		this.method = method;
		this.category = category;
		this.bm25 = bm25;
		try {
			this.reader = DirectoryReader.open(this.index.getIndex());
			this.searcher = new IndexSearcher(reader);
			this.mrr.setSearcher(this.searcher);
			if (!this.bm25) {
				System.out.println("Setting tfidf similarity");
				searcher.setSimilarity(new ClassicSimilarity());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Process the file containing the questions and answers, and run the questions over the index.
	 */
	public void processQuestionFile() {
		Scanner input;
		try {
			input = new Scanner(new File ("resources/queries/questions.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		// Create an output file whose name is based on system.
		try {
			String filename = (this.bm25 ? "" : "tfidf_") + "" + (this.category ? "category_" : "") + "query_" + this.method + ".txt";
			this.writer = new BufferedWriter(new FileWriter("resources/output/" + filename));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		int index = 1;
		int score = 0;
		while (input.hasNextLine()) {
			// Read in the 4 lines given to each query.
			String category = input.nextLine();
			String q = input.nextLine();
			String answer = input.nextLine();
			// Consume the newline.
			input.nextLine();
			
			try {
				this.writer.write("Query: " + index + "\n");
				// Run the question over the index.
				String result = this.query(category, q, answer);
				System.out.println("Result: " + result + " Answer: " + answer);
				this.writer.write("Result: " + result + " - Answer: " + answer + "\n");
				// Split to handle answers with multiple correct titles.
				String[] parts = answer.split("\\|");
				for (String part : parts) {
					// Check if my answer is the same as any of the given answers.
					if (result.equals(part)) {
						this.writer.write("Correct!\n");
						System.out.println("Correct!");
						score += 1;
					}
				}
				this.writer.write("\n");
				index++;
			} catch (IOException e) {
				System.out.print("Error querying documents: ");
				e.printStackTrace();
			}
		}
		// Calculate the total MPR score after all queries have been processed.
		double eval = this.mrr.calculate();
		System.out.println("Evaluation: " + eval);
		try {
			this.writer.write("Total score: " + score + "\n");
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Total score: " + score);
	}

	/**
	 * Query the index for relevant documents and return the top result.
	 * @param cat The category the query belongs to.
	 * @param qString The query String.
	 * @param answer The answer string.
	 * @return The String title of the top ranked document.
	 * @throws IOException The index cannot be queried.
	 */
	public String query(String cat, String qString, String answer) throws IOException {
		Query q;
		
		if (this.category) {
			// Add the category to the query if desired.
			qString = cat + " " + qString;
		}
		
		// Remove the punctuation from the query and lowercase query.
		qString = qString.toLowerCase();
		qString = qString.replaceAll("'", "");
		qString = qString.replaceAll("\\.|,|!|\\?|&|\"|:|;|-|\\(|\\)", " ");
		if (this.method.equals("lemma")) {
			// Lemmatize the query if we are lemmatizing the index.
			qString = Lemmatization.removeStopWords(qString);
			qString = Lemmatization.lemma(qString);
		}
			
		try {
			// Parse the query.
			q = new QueryParser("content", this.index.getAnalyzer()).parse(qString);
			System.out.println("Query: " + qString);
			this.writer.write("Query: " + qString + "\n");
		} catch (ParseException e) {
			System.out.print("Couldn't parse query: ");
			e.printStackTrace();
			return "";
		}
		
		// Search and rank the documents.
		TopDocs docs = this.searcher.search(q, 100);
		ScoreDoc[] hits = docs.scoreDocs;
		this.mrr.add(hits, answer);
		
		// Return the top document.
		if (hits.length > 0) {
			Document d = this.searcher.doc(hits[0].doc);
			return d.get("title");
		} else {
			return "No hit found :(";
		}
	}
}
