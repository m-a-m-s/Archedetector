package com.rug.archedetector.service;

import com.rug.archedetector.dao.*;
import com.rug.archedetector.model.Comment;
import com.rug.archedetector.model.Issue;
import com.rug.archedetector.model.IssueList;
import com.rug.archedetector.util.ReindexHelpers;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        ReindexHelpers.reindexPartial(IDs, issueListRepository, issueRepository, commentRepository);
    }
}
