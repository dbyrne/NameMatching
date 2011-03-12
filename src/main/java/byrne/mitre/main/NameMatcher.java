package byrne.mitre.main;

/**
 * Copyright 2011 David Byrne
 *    
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

import byrne.mitre.MitreQuery;
import byrne.mitre.NGramAnalyzer;
import byrne.mitre.NameEntry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class NameMatcher {

    public static void main(String[] args) {

        try {

            long startTime = System.currentTimeMillis();

            System.out.println("Loading Index...");
            final Analyzer analyzer = new NGramAnalyzer(2,4);

            final Directory index = new RAMDirectory();
            final IndexWriter writer =
                new IndexWriter(index, analyzer, true,
                                IndexWriter.MaxFieldLength.UNLIMITED);
            loadIndex("index.txt", writer);
            writer.close();

            System.out.println("Running queries...");
            final BufferedReader bufferedReader =
                new BufferedReader(new FileReader("queries.txt"));
            final BufferedWriter out =
                new BufferedWriter(new FileWriter("results.txt"));
            
            final IndexSearcher searcher = new IndexSearcher(index, true);
            String line = null;  
            
            final int N_THREADS = Runtime.getRuntime().availableProcessors();
            System.out.println("Total threads: " + N_THREADS);
            
            final ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);

            while ((line = bufferedReader.readLine()) != null) {
                final NameEntry entry = new NameEntry(line);
                final MitreQuery q = new MitreQuery(entry, analyzer, searcher, out);
                executor.execute(q);
            }
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);

            bufferedReader.close();
            searcher.close();
            out.close();

            long endTime = System.currentTimeMillis();
            System.out.println("Total run time: " +
                               (endTime - startTime) / 60000 + " minutes");

        } catch (IOException e) {
            System.out.println(e);
        } catch (InterruptedException e) {
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