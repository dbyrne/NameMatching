package byrne.mitre.main;

import byrne.mitre.NGramAnalyzer;
import byrne.mitre.NameEntry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class NameMatcher {

    public static void main(String[] args) {

        try {

            long startTime = System.currentTimeMillis();

            System.out.println("Loading Index...");
            final Analyzer analyzer = new NGramAnalyzer(2,4);

            Directory index = new RAMDirectory();
            IndexWriter writer =
                new IndexWriter(index, analyzer, true,
                                IndexWriter.MaxFieldLength.UNLIMITED);
            loadIndex("index.txt", writer);
            writer.close();

            System.out.println("Running queries...");
            BufferedReader bufferedReader =
                new BufferedReader(new FileReader("queries.txt"));
            final BufferedWriter out =
                new BufferedWriter(new FileWriter("results.txt"));
            final int hitsPerPage = 1000;
            final IndexSearcher searcher = new IndexSearcher(index, true);
            String line = null;
            final DecimalFormat df = new DecimalFormat("#.00");

            final int N_THREADS = 8;
            BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(10000);
            ThreadPoolExecutor executor =
                new ThreadPoolExecutor(N_THREADS, N_THREADS,
                                       Long.MAX_VALUE, TimeUnit.SECONDS, queue);

            int submittedTaskCount = 0;

            while ((line = bufferedReader.readLine()) != null) {
                final String l1 = line;
                Runnable r = new Runnable() {
                        public void run() {

                            try {
                                NameEntry entry = new NameEntry(l1);

                                TokenStream tokenStream = analyzer.tokenStream
                                    ("ngrams",
                                     new StringReader(entry.getFullName()));

                                BooleanQuery bq = new BooleanQuery();
                                while (tokenStream.incrementToken()) {
                                    Term t =
                                        new Term("ngrams",
                                                 tokenStream.getAttribute
                                                 (TermAttribute.class).term());
                                    bq.add(new TermQuery(t),
                                           BooleanClause.Occur.SHOULD);
                                }

                                TopScoreDocCollector collector =
                                    TopScoreDocCollector.create(hitsPerPage,
                                                                true);
                                searcher.search(bq, collector);
                                ScoreDoc[] hits =
                                    collector.topDocs().scoreDocs;

                                for(int i = 0; i < hits.length; ++i) {

                                    int docId = hits[i].doc;
                                    Document d = searcher.doc(docId);
                                    
                                    out.write(entry.getID() + "|" +
                                              d.get("id") + "|" +
                                              df.format(hits[i].score)+"\n");
                                }
                            } catch (IOException IOE) {
                            }
                        }
                    };
                submittedTaskCount += 1;
                executor.execute(r);
            }

            while (executor.getCompletedTaskCount() < submittedTaskCount) {
                Thread.yield();
            }

            bufferedReader.close();
            searcher.close();
            out.close();

            executor.shutdown();

            long endTime = System.currentTimeMillis();
            System.out.println("Total run time: " +
                               (endTime - startTime) / 60000 + " minutes");

        } catch (IOException e) {
            System.out.println(e);
        }

    }

    private static void loadIndex(String filename, IndexWriter writer)
        throws IOException {

        BufferedReader bufferedReader =
            new BufferedReader(new FileReader(filename));

        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            NameEntry entry = new NameEntry(line);
            Document doc = new Document();
            doc.add(new Field("id", entry.getID(), Field.Store.YES,
                              Field.Index.NOT_ANALYZED));
            doc.add(new Field("name", entry.getFullName(), Field.Store.YES,
                              Field.Index.NOT_ANALYZED));
            doc.add(new Field("ngrams",
                              new StringReader(entry.getFullName()),
                              Field.TermVector.YES));
            writer.addDocument(doc);

        }
        bufferedReader.close();
    }
}