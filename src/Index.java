import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

/**
 * This class contains all of the information related to the index.
 * @author etiennethompson
 */
public class Index {
	private Analyzer analyzer;
	private Directory index;
	private IndexWriter writer;
	private String inputDirectory;
	
	/**
	 * Initialize the Analyzer and Index.
	 * @param method String representing which text processing algoirthm to use.
	 * @param inputDirectory String representing which directory input files are located in.
	 * @param bm25 boolean determining whether to use the bm25 scoring algorithm.
	 */
	public Index(String method, String inputDirectory, boolean bm25) {
		this.inputDirectory = inputDirectory;
		// Determine the needed analyzer and path to the directory.
		String directoryPath = "directory/";
		if (method.equals("none")) {
			this.analyzer = new StandardAnalyzer();
		} else if (method.equals("lemma")) {
			this.analyzer = new WhitespaceAnalyzer();
			directoryPath = "lemma-directory/";
		} else {
			this.analyzer = new EnglishAnalyzer();
		}
		try {
			// Create the directory.
			this.index = new SimpleFSDirectory(Paths.get(directoryPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Return the analyzer the index used.
	 * @return the analyzer used to index the documents.
	 */
	public Analyzer getAnalyzer() {
		return this.analyzer;
	}
	
	/**
	 * Return the Directory	where index is stored.
	 * @return the Directory with the files.
	 */
	public Directory getIndex() {
		return this.index;
	}
	
	/**
	 * Return the writer used to create the index.
	 * @return the IndexWriter to write to the index.
	 */
	public IndexWriter getWriter() {
		return this.writer;
	}
	
	/**
	 * Construct the index from files containing different Wikipedia page documents.
	 * 
	 * Documents are expected to be in the resources/documents/regular|lemma/ directory.
	 */
	public void buildIndex() {
		IndexWriterConfig config = new IndexWriterConfig(this.analyzer);
		
		try {
			this.writer = new IndexWriter(this.index, config);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
		// Get a list of all wiki page files to process.
		File f = new File("resources/documents/" + inputDirectory);
		String[] pathnames = f.list();
		
		// Go through all of the files in the directory.
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
				// Read the next line.
				String line = input.nextLine();
				if (line.startsWith("[[")) {
					// Page title encountered.
					if (!title.equals("") && !content.equals("")) {
						// If statement to not index empty strings.
						try {
							// Remove punctuation from the document.
							content = content.replaceAll("\\.|,|'|\"|\\?|!|\\(|\\)|&||\\||:|;|/|$", "");
							content = content.replaceAll("-|=", " ");
							
							// Add it to the index.
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
					// Concatenate document content.
					content += line + " ";
				}
			}
		}
		System.out.println("Number of documents indexed: " + count);
		
	}
	
	/**
	 * Create a new document and add it to the index.
	 * @param title String representing the document title.
	 * @param content String representing the document content.
	 * @throws IOException if writer cannot write the document.
	 */
	private void addDoc(String title, String content) throws IOException {
		Document d = new Document();
		d.add(new StringField("title", title, Field.Store.YES));
		d.add(new TextField("content", content, Field.Store.YES));
		this.writer.addDocument(d);
	}
}
