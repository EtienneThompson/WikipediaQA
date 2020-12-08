import java.util.ArrayList;
import java.util.Properties;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * Static Class which provides methods needed to Lemmatize text.
 * @author etiennethompson
 */
public class Lemmatization {

	/**
	 * Run the StanfordCoreNLP lemmatization algorithm over the given content.
	 * @param content String that should be lemmatized.
	 * @return The lemmatized content.
	 */
	public static String lemma(String content) {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		
		CoreDocument document = pipeline.processToCoreDocument(content);
		String lemmaContent = "";
		for (CoreLabel tok : document.tokens()) {
			lemmaContent += tok.lemma() + " ";
		}
		return lemmaContent;
	}
	
	/**
	 * Remove all of the English stop words from the given content.
	 * @param content String from which to remove stop words.
	 * @return content String without stop words.
	 */
	public static String removeStopWords(String content) {
		CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
		String[] words = content.split("\\s+");
		ArrayList<String> tokens = new ArrayList<String>();
		for (String word : words) {
			tokens.add(word);
		}
		tokens.removeAll(stopWords);
		return String.join(" ", tokens);
	}
}
