package byrne.mitre.main;

import byrne.mitre.NGramAnalyzer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class NameMatcher {

  public static void main(String[] args) {

    try {
    
      Analyzer analyzer = new NGramAnalyzer(3,3);
      
      Directory index = new RAMDirectory();
      IndexWriter writer = new IndexWriter(index, analyzer, true, IndexWriter.MaxFieldLength.UNLIMITED);
      
      addDoc(writer, "Lucene in Action");
      addDoc(writer, "Lucene for Dummies");
      addDoc(writer, "Managing Gigabytes");
      addDoc(writer, "The Art of Computer Science");
      
      writer.close();

      String querystr = "Lukene";
      BooleanQuery bq = new BooleanQuery();
      bq.add(new TermQuery(new Term("name","ene")),BooleanClause.Occur.SHOULD);

      int hitsPerPage = 10;
      IndexSearcher searcher = new IndexSearcher(index, true);
      TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
      searcher.search(bq, collector);
      ScoreDoc[] hits = collector.topDocs().scoreDocs;

      System.out.println("Found " + hits.length + " hits.");
      for(int i=0;i<hits.length;++i) {
        int docId = hits[i].doc;
        Document d = searcher.doc(docId);
        System.out.println((i + 1) + ". " + d.get("id"));
      }

       searcher.close();
    } catch (IOException e) {
       System.out.println(e);
    }

  }

  private static void addDoc(IndexWriter writer, String value) throws IOException {
  
    Document doc = new Document();
    doc.add(new Field("id", value, Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc.add(new Field("name", new StringReader(value),Field.TermVector.YES));
    writer.addDocument(doc);
  }
}