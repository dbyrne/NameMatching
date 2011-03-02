package byrne.mitre;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ngram.NGramTokenizer;

public class NGramAnalyzer extends Analyzer {
	
  private int minGram;
  private int maxGram;
  
  public NGramAnalyzer(int minGram, int maxGram) {
    this.minGram = minGram;
    this.maxGram = maxGram;
  }
  
  @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
	  return new NGramTokenizer(reader, minGram, maxGram);
  }
}
