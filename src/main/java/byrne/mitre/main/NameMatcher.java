package byrne.mitre.main;

import byrne.mitre.NGramAnalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

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
      Analyzer analyzer = new NGramAnalyzer(3,3);
      Directory index = new RAMDirectory();
      IndexWriter writer = new IndexWriter(index, analyzer, true, IndexWriter.MaxFieldLength.UNLIMITED);
      loadIndex("index.txt", writer);
      writer.close();

      System.out.println("Performing trigram comparison...");
      BufferedReader bufferedReader = new BufferedReader(new FileReader("queries.txt"));
      BufferedWriter out = new BufferedWriter(new FileWriter("results.txt"));
      int hitsPerPage = 500;
      IndexSearcher searcher = new IndexSearcher(index, true);
      String line = null;
      
      
	  while ((line = bufferedReader.readLine()) != null) {
		  
		  String[] vals = line.split("\\|");
		  if (vals[1].equals("FNU")) {
			  vals[1] = "";
		  }
		  String name = vals[1]+" "+vals[2];
		  
	      TokenStream tokenStream = analyzer.tokenStream("ngrams", new StringReader(name));
	      
	      BooleanQuery bq = new BooleanQuery();
	      while (tokenStream.incrementToken()) {
	    	  Term t = new Term("ngrams",tokenStream.getAttribute(TermAttribute.class).term());
	    	  bq.add(new TermQuery(t),BooleanClause.Occur.SHOULD);
	      }
	      
	      TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
	      searcher.search(bq, collector);
	      ScoreDoc[] hits = collector.topDocs().scoreDocs;

	      for(int i=0;i<hits.length;++i) {
	        int docId = hits[i].doc;
	        Document d = searcher.doc(docId);
	        out.write(vals[0]+"|"+d.get("id")+"|"+(500-i)/501.0+"\n");
	      }
		  
	  }
	  
	  bufferedReader.close();
      searcher.close();
      out.close();

      long endTime = System.currentTimeMillis();
      
      System.out.println("Total run time: " + (endTime-startTime)/60000 + " minutes");
      
    } catch (IOException e) {
       System.out.println(e);
    }

  }

  private static void loadIndex(String filename, IndexWriter writer) throws IOException {
	  BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));
	  
	  String line = null;
	  
	  while ((line = bufferedReader.readLine()) != null) {
		  
		  String[] vals = line.split("\\|");
		  if (vals[1].equals("FNU")) {
			  vals[1] = "";
		  }
		  String name = vals[1]+" "+vals[2];
		  
		  Document doc = new Document();
		  doc.add(new Field("id", vals[0], Field.Store.YES, Field.Index.NOT_ANALYZED));
		  doc.add(new Field("name", name, Field.Store.NO, Field.Index.NOT_ANALYZED));
		  doc.add(new Field("ngrams", new StringReader(name),Field.TermVector.YES));
		  writer.addDocument(doc);
		  
	  }
	  bufferedReader.close();
  }
}