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
import org.apache.lucene.search.similarities.TFIDFSimilarity;

public class QueryMachine {
	private Index index;
	private MeanReciprocalRank mpr;
	private BufferedWriter writer;
	private String method;
	private boolean category;
	private boolean bm25;
	
	public QueryMachine(Index index, String method, boolean category, boolean bm25) {
		this.index = index;
		this.mpr = new MeanReciprocalRank();
		this.method = method;
		this.category = category;
		this.bm25 = bm25;
	}
	
	public void processQuestionFile() {
		Scanner input;
		try {
			input = new Scanner(new File ("resources/queries/questions.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
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
				String result = this.query(category, q, answer);
				System.out.println("Result: " + result + " Answer: " + answer);
				this.writer.write("Result: " + result + " - Answer: " + answer + "\n");
				// Split to handle answers with multiple correct titles.
				String[] parts = answer.split("\\|");
				for (String part : parts) {
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
		double eval = this.mpr.calculate();
		System.out.println("Evaluation: " + eval);
		try {
			this.writer.write("Total score: " + score + "\n");
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Total score: " + score);
	}

	public String query(String cat, String qString, String answer) throws IOException {
		Query q;
		
		// Add the category to the query and lowercase everything.
		if (this.category) { 
			qString = cat + " " + qString;
		}
		
		// Remove the punctuation from the query.
		qString = qString.toLowerCase();
		qString = qString.replaceAll("'", "");
		qString = qString.replaceAll("\\.|,|!|\\?|&|\"|:|;|-|\\(|\\)", " ");
		if (this.method.equals("lemma")) {
			// Lemmatize the query if we are lemmatizing the index.
			qString = Lemmatization.removeStopWords(qString);
			qString = Lemmatization.lemma(qString);
		}
			
		try {
			q = new QueryParser("content", this.index.getAnalyzer()).parse(qString);
			System.out.println("Query: " + qString);
			this.writer.write("Query: " + qString + "\n");
		} catch (ParseException e) {
			System.out.print("Couldn't parse query: ");
			e.printStackTrace();
			return "";
		}
		
		IndexReader reader = DirectoryReader.open(this.index.getIndex());
		IndexSearcher searcher = new IndexSearcher(reader);
		if (!this.bm25) {
			System.out.println("Setting tfidf similarity");
			searcher.setSimilarity(new ClassicSimilarity());
		}
		this.mpr.setSearcher(searcher);
		TopDocs docs = searcher.search(q, 100);
		ScoreDoc[] hits = docs.scoreDocs;
		this.mpr.add(hits, answer);
		
		if (hits.length > 0) {
			Document d = searcher.doc(hits[0].doc);
			return d.get("title");
		} else {
			return "No hit found :(";
		}
	}
}
