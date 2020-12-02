import java.util.ArrayList;
import java.util.Properties;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class Lemmatization {

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
