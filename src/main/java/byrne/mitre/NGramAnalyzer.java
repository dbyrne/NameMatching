package byrne.mitre;

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

import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

public class NGramAnalyzer extends Analyzer {
	
  private int minGram;
  private int maxGram;
  private final static Set<Character> WHITESPACE;
  static {
	    Character w[] = { ' ', '\t', '\n', '-', '\'', '.', ',' };
	    WHITESPACE = new HashSet<Character>(Arrays.asList(w));
  }
  
  public NGramAnalyzer(int minGram, int maxGram) {
    this.minGram = minGram;
    this.maxGram = maxGram;
  }
  
  @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
	  return new NGramTokenizer(reader, minGram, maxGram, WHITESPACE);
  }
}
