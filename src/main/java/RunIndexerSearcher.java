public class RunIndexerSearcher {
    /* Wrapper class to run indexing the documents and performing search on the indexed documents.
     * The search results are stored in results_ashrivas.txt */
    public static void main(String[] args) throws Exception {
        Index index = new Index();
        Searcher searcher = new Searcher();
        index.indexer();
        searcher.search();
    }
}
