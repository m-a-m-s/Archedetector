package com.rug.archedetector.util;

import com.rug.archedetector.dao.CommentRepository;
import com.rug.archedetector.dao.IssueListRepository;
import com.rug.archedetector.dao.IssueRepository;
import com.rug.archedetector.lucene.IssueListIndexer;
import com.rug.archedetector.lucene.LuceneSearcher;
import com.rug.archedetector.model.Comment;
import com.rug.archedetector.model.Issue;
import com.rug.archedetector.model.IssueList;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReindexHelpers {

    public static synchronized void reindexPartial(
            List<String> IDs,
            IssueListRepository issueListRepo,
            IssueRepository issueRepo,
            CommentRepository commentRepo) throws IOException, ParseException {
        List<IssueList> issueLists = issueListRepo.findAll();
        IssueListIndexer indexer = new IssueListIndexer();

        for (String ID : IDs) {
            for (IssueList list : issueLists) {
                if (ID.startsWith(list.getKey() + "-")) {

                    Directory dir = FSDirectory.open(Path.of(IssueListIndexer.indexDir + list.getId()));
                    Query query = LuceneSearcher.issueQueryParser.parse("key:\"" + ID + "\"");

                    // check that we have such a doc
                    IndexReader reader = DirectoryReader.open(dir);
                    IndexSearcher searcher = new IndexSearcher(reader);
                    if (searcher.count(query) != 0) {
                        IndexWriter writer = new IndexWriter(
                                dir,
                                new IndexWriterConfig(new StandardAnalyzer())
                        );

                        // delete it
                        writer.deleteDocuments(query);
                        writer.close();
                    }
                    reader.close();

                    // remake it from given information
                    List<Issue> issueListIssues = issueRepo.findByIssueListId(list.getId());
                    for (Issue issue : issueListIssues) {
                        if (Objects.equals(issue.getKey(), ID)) {
                            // this is the issue
                            List<Comment> comments = commentRepo.findCommentByIssueId(issue.getId(), Sort.unsorted());
                            List<Issue> issues = new ArrayList<Issue>();
                            issues.add(issue);
                            indexer.index(list, issues, comments);
                            break;
                        }
                    }
                }
            }
        }
    }
}
