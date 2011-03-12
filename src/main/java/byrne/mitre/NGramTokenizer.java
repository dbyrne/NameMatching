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

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.AttributeSource;

/**
 * Tokenizes the input into n-grams of the given size(s).
 */
public final class NGramTokenizer extends Tokenizer {

  public static final int DEFAULT_MIN_NGRAM_SIZE = 2;
  public static final int DEFAULT_MAX_NGRAM_SIZE = 4;
    
  public static final Set<Character> DEFAULT_WHITESPACE_CHARS;
  static {
    Character whitespace[] = { ' ', '\t', '\n' };
    DEFAULT_WHITESPACE_CHARS = new HashSet<Character>(Arrays.asList(whitespace));
  }
  
  private int gramSize;
  private int minGram;
  private int maxGram;
  private int tmp;

  private LinkedList<Integer> charsQueue;
  private LinkedList<Integer> offsetQueue;
  private Set<Character> whitespace;
      
  private TermAttribute termAtt = addAttribute(TermAttribute.class);
  private OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
  
  private boolean collapse;

  /**
   * Creates NGramTokenizer with default min and max n-grams.
   * @param input {@link Reader} holding the input to be tokenized
   */
  public NGramTokenizer(Reader input) {
    this(input, DEFAULT_MIN_NGRAM_SIZE, DEFAULT_MAX_NGRAM_SIZE, DEFAULT_WHITESPACE_CHARS);
  }

  /**
   * Creates NGramTokenizer with given min and max n-grams.
   * @param input {@link Reader} holding the input to be tokenized
   * @param minGram the smallest n-gram to generate
   * @param maxGram the largest n-gram to generate
   */
  public NGramTokenizer(Reader input, int minGram, int maxGram) {
    this(input, minGram, maxGram, DEFAULT_WHITESPACE_CHARS);
  }

  /**
   * Creates NGramTokenizer with given min and max n-grams.
   * @param input {@link Reader} holding the input to be tokenized
   * @param minGram the smallest n-gram to generate
   * @param maxGram the largest n-gram to generate
   * @param whitespace whitespace characters to be collapsed together
   */
  public NGramTokenizer(Reader input, int minGram, int maxGram, Set<Character> whitespace) {
    super(input);
    init(minGram, maxGram, whitespace);
  }

  /**
   * Creates NGramTokenizer with given min and max n-grams.
   * @param source {@link AttributeSource} to use
   * @param input {@link Reader} holding the input to be tokenized
   * @param minGram the smallest n-gram to generate
   * @param maxGram the largest n-gram to generate
   */
  public NGramTokenizer(AttributeSource source, Reader input, int minGram, int maxGram) {
    this(source, input, minGram, maxGram, DEFAULT_WHITESPACE_CHARS);
  }

  /**
   * Creates NGramTokenizer with given min and max n-grams.
   * @param source {@link AttributeSource} to use
   * @param input {@link Reader} holding the input to be tokenized
   * @param minGram the smallest n-gram to generate
   * @param maxGram the largest n-gram to generate
   * @param whitespace whitespace characters to be collapsed together
   */
  public NGramTokenizer(AttributeSource source, Reader input, int minGram, int maxGram, Set<Character> whitespace) {
    super(source, input);
    init(minGram, maxGram, whitespace);
  }

  /**
   * Creates NGramTokenizer with given min and max n-grams.
   * @param factory {@link org.apache.lucene.util.AttributeSource.AttributeFactory} to use
   * @param input {@link Reader} holding the input to be tokenized
   * @param minGram the smallest n-gram to generate
   * @param maxGram the largest n-gram to generate
   */
  public NGramTokenizer(AttributeFactory factory, Reader input, int minGram, int maxGram) {
    this(factory, input, minGram, maxGram, DEFAULT_WHITESPACE_CHARS);
  }

  /**
   * Creates NGramTokenizer with given min and max n-grams.
   * @param factory {@link org.apache.lucene.util.AttributeSource.AttributeFactory} to use
   * @param input {@link Reader} holding the input to be tokenized
   * @param minGram the smallest n-gram to generate
   * @param maxGram the largest n-gram to generate
   * @param whitespace whitespace characters to be collapsed together
   */
  public NGramTokenizer(AttributeFactory factory, Reader input, int minGram, int maxGram, Set<Character> whitespace) {
    super(factory, input);
    init(minGram, maxGram, whitespace);
  }
  
  private void init(int minGram, int maxGram, Set<Character> whitespace) {
    if (minGram < 1) {
      throw new IllegalArgumentException("minGram must be greater than zero");
    }
    if (minGram > maxGram) {
      throw new IllegalArgumentException("minGram must not be greater than maxGram");
    }
    this.minGram = minGram;
    this.maxGram = maxGram;
    this.whitespace = whitespace;
    this.maxGram = maxGram;
    this.minGram = minGram;
    gramSize = minGram;
    resetPosition();
  }

  @Override
  public final boolean incrementToken() throws IOException {
    if (gramSize > maxGram)
      return false;

    if (charsQueue.isEmpty()) {
      charsQueue.offer((int)'_');
      offsetQueue.offer(0);
      for (int x = 0; x < gramSize-1; x++) {
        int count = nextChar();
        if (tmp == -1) {
          count += offsetQueue.getLast()-1;
          offsetAtt.setOffset(correctOffset(count), correctOffset(count));
          return false;
        } else {
          charsQueue.offer(tmp);
          offsetQueue.offer(offsetQueue.getLast()+count);
        }
      }
    } else if (tmp == -1) {
      ++gramSize;
      resetPosition();
      input.reset();
      return incrementToken();
    } else {
      int count = nextChar();
      if (tmp == -1) {
        charsQueue.offer((int)'_');
        offsetQueue.offer(offsetQueue.getLast()+count-1);
      } else {
        charsQueue.offer(tmp);
        offsetQueue.offer(offsetQueue.getLast()+count);
      }
      charsQueue.poll();
      offsetQueue.poll();

      int dist = offsetQueue.get(1)-offsetQueue.getFirst()-1;
      if (dist > 0)
        offsetQueue.set(0, offsetQueue.getFirst()+dist);
    }

    final StringBuilder sb = new StringBuilder();
    for (int i : charsQueue)
      sb.append((char) i);

    if (offsetQueue.getFirst() != offsetQueue.getLast()) {
      clearAttributes();
      termAtt.setTermBuffer(sb.toString());
      offsetAtt.setOffset(correctOffset(offsetQueue.getFirst()), correctOffset(offsetQueue.getLast()));
      return true;
    } else {
      return incrementToken();
    }
  }    
  
  @Override
  public void reset(Reader input) throws IOException {
    super.reset(input);
    reset();
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    gramSize = minGram;
    resetPosition();
  }

  private void resetPosition() {
    charsQueue = new LinkedList<Integer>();
    offsetQueue = new LinkedList<Integer>();
    offsetQueue.offer(0);
    tmp = 0;
    collapse = false;
  }
    
  /** Returns the number of characters read*/
  private int nextChar() throws IOException {
    int count = 0;
    if (collapse) {
      collapse = false;
      do {
        tmp = input.read();
        ++count;
      } while (whitespace.contains(new Character((char)tmp)));
    } else {
      tmp = input.read();
      ++count;
      if (whitespace.contains(new Character((char)tmp))) {
        tmp = '_';
        collapse = true;
      }
    }
    return count;
  }
}

