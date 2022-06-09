package com.rug.archedetector.controller;

import com.rug.archedetector.dao.CommentRepository;
import com.rug.archedetector.dao.IssueRepository;
import com.rug.archedetector.lucene.IssueListIndexer;
import com.rug.archedetector.model.Comment;
import com.rug.archedetector.model.Issue;
import com.rug.archedetector.model.IssueList;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ReindexWorker extends Thread{
    private final IssueList list;
    private final IssueListIndexer indexer;
    private final IssueRepository issueRepo;
    private final CommentRepository commentRepo;

    private static final int STEP = 200;

    public ReindexWorker(
            IssueList list,
            IssueListIndexer indexer,
            IssueRepository issueRepo,
            CommentRepository commentRepo) {
        this.list = list;
        this.indexer = indexer;
        this.issueRepo = issueRepo;
        this.commentRepo = commentRepo;
    }

    @Override
    public void run() {
        List<Issue> issues = issueRepo.findByIssueListId(list.getId());
        List<Issue> issueChunk = new ArrayList<>();
        for (int i = 0; i < issues.size(); i += STEP) {
            System.out.println(Instant.now() + ": Reindexed " + i + " out of " + issues.size() + " issues...");
            List<Comment> comments = new ArrayList<>();
            for (int j = i; (j < (i + STEP)) && (j < issues.size()); j++) {
                Issue thisIssue = issues.get(j);
                issueChunk.add(thisIssue);
                comments.addAll(commentRepo.findCommentByIssueId(thisIssue.getId(), Sort.unsorted()));
            }
            indexer.index(list, issueChunk, comments);
            issueChunk = new ArrayList<>();
        }
    }
}
