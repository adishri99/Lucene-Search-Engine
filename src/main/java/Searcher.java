import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

public class Searcher {
    public void search() throws Exception {

        try {
            String results_path = "results_ashrivas.txt";
            final String indices_PATH = "Index";
            String queryStr = "";
            String query_PATH = "cranfield_dataset/cran.qry";

            PrintWriter writer = new PrintWriter(results_path, "UTF-8");
            IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indices_PATH)));
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            BufferedReader br = Files.newBufferedReader(Paths.get(query_PATH), StandardCharsets.UTF_8);

//			Select an analyzer
            Analyzer standardAnalyzer = new StandardAnalyzer();
            Analyzer simpleAnalyzer = new SimpleAnalyzer();
            Analyzer whitespaceAnalyzer = new WhitespaceAnalyzer();
            Analyzer englishAnalyzer = new EnglishAnalyzer();
            Analyzer customAnalyzer = CustomAnalyzer.builder().withTokenizer("standard").addTokenFilter("lowercase")
                    .addTokenFilter("stop").addTokenFilter("porterstem").addTokenFilter("capitalization").build();

//			Select a scoring method

            indexSearcher.setSimilarity(new BM25Similarity());

//			TF-DF Classic Similarity
//			indexSearcher.setSimilarity(new ClassicSimilarity());

//			Bayesian smoothing using Dirichlet priors.
//			indexSearcher.setSimilarity(new LMDirichletSimilarity());

//			Language model based on the Jelinek-Mercer smoothing method. The lambda value being float 0.7
//			indexSearcher.setSimilarity(new LMJelinekMercerSimilarity((float) 0.7));

//			A Multi similarity model combining BM25Similarity and LMJelinekMercerSimilarity
//            indexSearcher.setSimilarity(new MultiSimilarity(new Similarity[]{new BM25Similarity(),new LMJelinekMercerSimilarity((float) 0.7)}));

//          A Multi similarity model combining LMJelinekMercerSimilarity and LMDirichletSimilarity
//            indexSearcher.setSimilarity(new MultiSimilarity(new Similarity[]{new LMJelinekMercerSimilarity((float) 0.7),new LMDirichletSimilarity()}));

//          A Multi similarity model combining BM25Similarity and LMDirichletSimilarity
//            indexSearcher.setSimilarity(new MultiSimilarity(new Similarity[]{new BM25Similarity(),new LMDirichletSimilarity()}));

            MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
                    new String[] { "title", "author", "bibliography", "contentSubstance" }, englishAnalyzer);
            String presentLine = br.readLine();
            String query_id = "";
            int iterator = 0;

            /*
             * Extracting each query from cran.qry and performing a search on this query on
             * the documents indexed by the indexer
             */

            System.out.println("Reading Queries from '" + query_PATH + "' and forming search resuts in the : " + results_path + " file");
            while (presentLine != null) {
                iterator += 1;
                if (presentLine.startsWith(".I")) {
                    query_id = Integer.toString(iterator);
                    presentLine = br.readLine();
                }
                if (presentLine.startsWith(".W")) {
                    presentLine = br.readLine();
                    while (presentLine != null && !presentLine.startsWith(".I")) {
                        queryStr = queryStr + presentLine + " ";
                        presentLine = br.readLine();
                    }
                }
                queryStr = queryStr.trim();
                Query query = queryParser.parse(QueryParser.escape(queryStr));
                queryStr = "";
                executeSearch(indexSearcher, writer, Integer.parseInt(query_id), query);
            }

            System.out.println("Search Results can be found in the: " + results_path + " file");
            writer.close();
            indexReader.close();

        } catch (Exception e) {

            System.out.println("Execption occured: " + e.getClass());
            System.out.println("Exception Message: " + e.getMessage());
        }

    }

    // Perform a search based on the query and write the search results to the write
    public static void executeSearch(IndexSearcher isearcher, PrintWriter pwriter, Integer queryNum, Query query)
            throws Exception {
        // Get the set of results
        ScoreDoc[] hits = isearcher.search(query, 1400).scoreDocs;

        /*
         * Writing the results in a format that would be compatible with trec_eval so
         * that the results can be evaluated
         */
        for (int i = 0; i < hits.length; i++) {
            Document document = isearcher.doc(hits[i].doc);
            pwriter.println(queryNum + " 0 " + document.get("doc_id") + " " + i + " " + hits[i].score + " ASHRIVAS");
        }
    }

}
