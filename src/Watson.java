import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;

public class Watson {

	public static void main(String[] args) {
		Index index = new Index(args[0]);
		Scanner input;
		try {
			input = new Scanner(new File ("resources/queries/questions.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter("resources/output/tfidf_cateogry_query_none.txt"));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		int score = 0;
		MeanReciprocalRank mpr = new MeanReciprocalRank();
		while (input.hasNextLine()) {
			// Read in the 4 lines given to each query.
			String category = input.nextLine();
			String q = input.nextLine();
			String answer = input.nextLine();
			// Consume the newline.
			input.nextLine();
			
			try {
				String result = query(category, q, index, writer, mpr, answer);
				System.out.println("Result: " + result + " Answer: " + answer);
				if (result.equals(answer)) {
					score += 1;
				}
				writer.write("Result: " + result + " - Answer: " + answer + "\n");
				writer.write("\n");
			} catch (IOException e) {
				System.out.print("Error querying documents: ");
				e.printStackTrace();
			}
		}
		double eval = mpr.calculate();
		System.out.println("Evaluation: " + eval);
		try {
			writer.write("Total score: " + score + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Total score: " + score);
	}
	
	public static String query(String category, String qString, Index index, BufferedWriter writer, MeanReciprocalRank mpr, String answer) throws IOException {
		Query q;
		
		// Add the category to the query and lowercase everything.
		qString = category + " " + qString;
		
		// Remove the punctuation from the query.
		qString = qString.toLowerCase();
		qString = qString.replaceAll("\\.|,|!|\\?|&|'|\"|:|;|-", " ");
//		qString = Lemmatization.removeStopWords(qString);
//		qString = Lemmatization.lemma(qString);
		
		try {
			q = new QueryParser("content", index.getAnalyzer()).parse(qString);
			System.out.println("Query: " + qString);
			writer.write("Query: " + qString + "\n");
		} catch (ParseException e) {
			System.out.print("Couldn't parse query: ");
			e.printStackTrace();
			return "";
		}
		
		IndexReader reader = DirectoryReader.open(index.getIndex());
		IndexSearcher searcher = new IndexSearcher(reader);
		searcher.setSimilarity(new ClassicSimilarity());
		mpr.setSearcher(searcher);
		TopDocs docs = searcher.search(q, 100);
		ScoreDoc[] hits = docs.scoreDocs;
		mpr.add(hits, answer);
		
		if (hits.length > 0) {
			Document d = searcher.doc(hits[0].doc);
			return d.get("title");
		} else {
			return "No hit found :(";
		}
	}
	

}
