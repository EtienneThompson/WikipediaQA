/**
 * Main class for Watson Wikipedia Question Answering system.
 * @author etiennethompson
 */
public class Watson {
	private static boolean category = true;
	private static String inputDirectory = "regular";
	private static String method = "stem";
	private static boolean bm25 = true;
	private static boolean methodSet = false;
	private static boolean rebuild = false;

	public static void main(String[] args) {
		boolean valid = argparse(args);
		if (!valid) {
			System.out.println("Usage: watson [-h|--help] [--lemma|--none] [--tfidf] [--no-cateogry] [--rebuild]");
			return;
		}
		// Create index object.
		Index index = new Index(method, inputDirectory, bm25);
		if (rebuild) {
			// Rebuild the index if asked to.
			index.buildIndex();
		}
		// Create the query object.
		QueryMachine qm = new QueryMachine(index, method, category, bm25);
		// Run the queries in the given file.
		qm.processQuestionFile();
	}

	public static boolean argparse(String[] args) {
		/*
		 * Possible arguments and combinations:
		 * no args 	     = use the default values.
		 * --lemma       = use lemmatization during indexing and querying. Can't be combined with "--none"
		 * --none        = use no stemming/lemmatization during indexing and querying. Can't be combined with "--lemma"
		 * --tfidf       = use the tfidf scoring algorithm instead of bm25.
		 * --no-category = don't use the category when querying.
		 * --rebuild	 = reindex the documents.
		 */
		for (String arg : args) {
			if (arg.equals("--lemma")) {
				if (methodSet) {
					// Method was already set with "--lemma" or "--none".
					return false;
				}
				method = "lemma";
				inputDirectory = "lemma2";
				methodSet = true;
			} else if (arg.equals("--none")) {
				if (methodSet) {
					return false;
				}
				method = "none";
				inputDirectory = "regular";
				methodSet = true;
			} else if (arg.equals("--tfidf")) {
				bm25 = false;
			} else if (arg.equals("--no-category")) {
				category = false;
			} else if (arg.equals("--rebuild")) {
				rebuild = true;
			} else if (arg.equals("-h") || arg.equals("--help")) {
				System.out.println("Watson is a Question Answering system for jeopardy questions, with answers being Wikipedia page titles.");
				System.out.println();
				System.out.println("The default run configuration is to include the category from questions.txt in the query, to use Stemming");
				System.out.println("for indexing and query parsing, and to use BM25 scoring algorithm.");
				System.out.println();
				System.out.println("\t-h|--help\t\tList the possible command line arguments with a short description.");
				System.out.println("\t--lemma\t\t\tIndex Wikipedia pages and parse queries using Lemmatization.");
				System.out.println("\t--none\t\t\tIndex Wikipedia pages and parse queries without using Stemming or Lemmatization.");
				System.out.println("\t--tfidf\t\t\tScore the documents using the tf-idf algorithm instead of BM25.");
				System.out.println("\t--no-category\t\tDon't include the category of the answer as part of the query.");
				System.out.println("\t--rebuild\t\tReindex all of the documents.");
				return false;
			} else {
				return false;
			}
		}
		return true;
	}
}
