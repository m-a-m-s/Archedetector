package com.rug.archedetector.service;

import com.rug.archedetector.dao.*;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class IssueService {
    @Autowired
    private IssueListRepository issueListRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private QueryCollectionRepository queryCollectionRepository;

    public Page<Issue> getIssueByIssueListId(Long id, Pageable pageable) {
        return issueRepository.findByIssueListId(id, pageable);
    }

    public List<Comment> getCommentsByIssueId(Long id, Sort sort) {
        return commentRepository.findCommentByIssueId(id, sort);
    }

    public Page<Issue> getIssueByQueryCollectionId(Long queryCollectionId, Pageable pageable) {
        return queryCollectionRepository.findById(queryCollectionId).map(queryCollection -> {
            List<Long> listIssueId = new ArrayList<>();
            for(IssueList issueList : queryCollection.getIssueLists()){
                listIssueId.add(issueList.getId());
            }
            return issueRepository.findByIssueListIdIn(listIssueId, pageable);
        }).orElseThrow();
    }

    public Issue saveIssue(Issue issue) {
        return issueRepository.save(issue);
    }


    public void reindexPartial(List<String> IDs) throws ParseException, IOException {
        List<IssueList> issueLists = issueListRepository.findAll();
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
                    List<Issue> issueListIssues = issueRepository.findByIssueListId(list.getId());
                    for (Issue issue : issueListIssues) {
                        if (Objects.equals(issue.getKey(), ID)) {
                            // this is the issue
                            List<Comment> comments = commentRepository.findCommentByIssueId(issue.getId(), Sort.unsorted());
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
