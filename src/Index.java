import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/**
 * This class contains all of the information related to the index.
 * @author etiennethompson
 *
 */
public class Index {
	private Analyzer analyzer;
	private Directory index;
	private IndexWriter writer;
	private String method;
	private String inputDirectory;
	private boolean bm25;
	
	public Index(String method, String inputDirectory, boolean bm25) {
		this.method = method;
		this.inputDirectory = inputDirectory;
		this.bm25 = bm25;
		if (method.equals("none")) {
			this.analyzer = new StandardAnalyzer();
		} else if (method.equals("lemma")) {
			this.analyzer = new WhitespaceAnalyzer();
		} else {
			this.analyzer = new EnglishAnalyzer();
		}
		this.index = new RAMDirectory();
		this.buildIndex();
	}
	
	public Analyzer getAnalyzer() {
		return this.analyzer;
	}
	
	public Directory getIndex() {
		return this.index;
	}
	
	public IndexWriter getWriter() {
		return this.writer;
	}
	
	private void buildIndex() {
		IndexWriterConfig config = new IndexWriterConfig(this.analyzer);
		if (!this.bm25) { 
			config.setSimilarity(new ClassicSimilarity());
		}
		
		try {
			this.writer = new IndexWriter(this.index, config);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
		// Get a list of all wiki page files to process.
		File f = new File("resources/documents/" + inputDirectory);
		String[] pathnames = f.list();
		
		int count = 0;
		for (String path : pathnames) {
			Scanner input;
			try {
				input = new Scanner(new File("resources/documents/" + inputDirectory + "/" + path));
				System.out.println(path);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			String title = "";
			String content = "";
			while (input.hasNextLine()) {
				String line = input.nextLine();
				if (line.startsWith("[[")) {
					if (!title.equals("") && !content.equals("")) {
						// If statement to not index empty strings.
						try {
							// Remove punctuation from the document.
							content = content.replaceAll("\\.|,|'|\"|\\?|!|\\(|\\)|&||\\||:|;|/|$", "");
							content = content.replaceAll("-|=", " ");
							
							this.addDoc(title, content);
							this.writer.commit();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					count++;
					// Remove the  "[[" and  "]]" from the title.
					title = line.replaceAll("\\[|\\]", "");
					content = "";
				} else {
					content += line + " ";
				}
			}
		}
		System.out.println("Number of documents indexed: " + count);
		
	}
	
	private void addDoc(String title, String content) throws IOException {
		Document d = new Document();
		d.add(new StringField("title", title, Field.Store.YES));
		d.add(new TextField("content", content, Field.Store.YES));
		this.writer.addDocument(d);
	}
}
