This is the code for my entry into the MITRE Name Matching Challenge.
 
Challenge website: https://mitrechallenge.mitre.org/NameMatching/login/create
Team name: Yet Another Challenge Team
 
Before you can run it, there are two prerequisites:
-Install Maven
-Get the MITRE Name Matching test data

The NGramTokenizer used for this project is much different than the standard
one that comes with Lucene-analyzers.  Currently, it is attached as a patch
to JIRA issue LUCENE-2947:

https://issues.apache.org/jira/browse/LUCENE-2947

If you try it out, I'd love to get some feedback.

Copyright (C) 2011 David Byrne <david.r.byrne@gmail.com>