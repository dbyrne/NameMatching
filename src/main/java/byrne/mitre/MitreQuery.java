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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;

public class MitreQuery implements Runnable {

	final static DecimalFormat df = new DecimalFormat("#.00");
	final static int hitsPerPage = 1000;
	final NameEntry entry;
	final Analyzer analyzer;
	final IndexSearcher searcher;
	final BufferedWriter out;
	
	public MitreQuery(NameEntry entry, 
				Analyzer analyzer, 
				IndexSearcher searcher,
				BufferedWriter out) {
		this.entry = entry;
		this.analyzer = analyzer;
		this.searcher = searcher;
		this.out = out;
	}
	
	public void run() {

		try {
			

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

}
