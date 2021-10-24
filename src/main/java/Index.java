import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.*;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.MultiSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Index {

    public void indexer() throws Exception{

        final String indicesPATH = "Index";
        final Path documetDirectory = Paths.get("cranfield_dataset/cran.all.1400");

        if (!Files.isReadable(documetDirectory)) {
            System.out.println("The following document path is not present on the system or is not readable:");
            System.out.println(documetDirectory.toAbsolutePath());
            System.exit(1);
        }

        Date indexingStartTimestamp = new Date();

        try {

            System.out.println("Indexing cranfield dataset to the '" + indicesPATH + "'  directory ");

            // Declaring several analyzers that are used to process TextField
            Analyzer standardAnalyzer = new StandardAnalyzer();
            Analyzer simpleAnalyzer = new SimpleAnalyzer();
            Analyzer whitespaceAnalyzer = new WhitespaceAnalyzer();
            Analyzer englishAnalyzer = new EnglishAnalyzer();
            Analyzer customAnalyzer = CustomAnalyzer.builder().withTokenizer("standard").addTokenFilter("lowercase")
                    .addTokenFilter("stop").addTokenFilter("porterstem").addTokenFilter("capitalization").build();

            Directory indexDirectory = FSDirectory.open(Paths.get(indicesPATH));
            IndexWriterConfig indexWriterConf = new IndexWriterConfig(englishAnalyzer);
            indexWriterConf.setSimilarity(new BM25Similarity());

//			TF-DF Classic Similarity
//			indexWriterConf.setSimilarity(new ClassicSimilarity());

//			Bayesian smoothing using Dirichlet priors.
//			indexWriterConf.setSimilarity(new LMDirichletSimilarity());

//			Language model based on the Jelinek-Mercer smoothing method. The lambda value being float 0.7
//			indexWriterConf.setSimilarity(new LMJelinekMercerSimilarity((float) 0.7));

//			A Multi similarity model combining BM25Similarity and LMJelinekMercerSimilarity
//            indexWriterConf.setSimilarity(new MultiSimilarity(new Similarity[]{new BM25Similarity(),new LMJelinekMercerSimilarity((float) 0.7)}));

//          A Multi similarity model combining LMJelinekMercerSimilarity and LMDirichletSimilarity
//            indexWriterConf.setSimilarity(new MultiSimilarity(new Similarity[]{new LMJelinekMercerSimilarity((float) 0.7),new LMDirichletSimilarity()}));

//          A Multi similarity model combining BM25Similarity and LMDirichletSimilarity
//            indexWriterConf.setSimilarity(new MultiSimilarity(new Similarity[]{new BM25Similarity(),new LMDirichletSimilarity()}));

            indexWriterConf.setOpenMode(OpenMode.CREATE);

            IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConf);
            indexCranfieldDocuments(indexWriter, documetDirectory);

//           forceMerge(1) creates one segment out of the many segments generated. This optimizes search performance
//           forceMerge has side effects with memory leaks especially if the index is large. In our use case, it shall enhance performance
            indexWriter.forceMerge(1);
            indexWriter.close();

            long totalTimeToIndex = new Date().getTime() - indexingStartTimestamp.getTime();
            System.out.println("Indexing completed. Time taken to Index (in milliseconds) = " + totalTimeToIndex);

        } catch (Exception exception) {
            System.out.println("Execption occured: " + exception.getClass());
            System.out.println("Exception Message: " + exception.getMessage());
        }
    }

    private static void indexCranfieldDocuments(IndexWriter indexWriter, Path documetDirectory) throws Exception {
        try {
            System.out.println("Indexing the cranfield dataset documents");
            String completeText = "";

            InputStream inputStream = Files.newInputStream(documetDirectory);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            String presentLine = br.readLine();

            while (presentLine != null) {
                Document document = new Document();

                if (presentLine.startsWith(".I")) {
//					We are simply storing the document id number. It may be unnecessary to analyze it.
                    document.add(new StringField("doc_id", presentLine.substring(3), Field.Store.YES));
                    presentLine = br.readLine();
                }

                if (presentLine.startsWith(".T")) {
                    presentLine = br.readLine();
                    // We add the full title to the document by reading the text till we encounter
                    // ".A"
                    while (!presentLine.startsWith(".A")) {
                        completeText = completeText + presentLine + " ";
                        presentLine = br.readLine();
                    }
                    document.add(new TextField("title", completeText, Field.Store.YES));
                    completeText = "";
                }

                if (presentLine.startsWith(".A")) {
                    presentLine = br.readLine();

                    /*
                     * We add the author names to the document by reading the text till we encounter
                     * ".B"
                     */
                    while (!presentLine.startsWith(".B")) {
                        completeText = completeText + presentLine + " ";
                        presentLine = br.readLine();
                    }
                    document.add(new TextField("author", completeText, Field.Store.YES));
                    completeText = "";
                }

                if (presentLine.startsWith(".B")) {
                    presentLine = br.readLine();

                    /*
                     * We add the bibliography to the document by reading the text till we encounter
                     * ".W"
                     */
                    while (!presentLine.startsWith(".W")) {
                        completeText = completeText + presentLine + " ";
                        presentLine = br.readLine();
                    }

                    document.add(new TextField("bibliography", completeText, Field.Store.YES));
                    completeText = "";
                }

                if (presentLine.startsWith(".W")) {
                    presentLine = br.readLine();
                    /*
                     * We add the words to the document by reading the text till we have reached the
                     * end of the document and encounter ".I"
                     */
                    while (presentLine != null && !presentLine.startsWith(".I")) {
                        completeText = completeText + presentLine + " ";
                        presentLine = br.readLine();
                    }
                    document.add(new TextField("contentSubstance", completeText, Field.Store.YES));
                    completeText = "";
                }
                indexWriter.addDocument(document);
            }
        } catch (Exception exception) {
            System.out.println("Execption occured: " + exception.getClass());
            System.out.println("Exception Message: " + exception.getMessage());
        }
    }
}
