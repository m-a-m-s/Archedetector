package com.rug.archedetector.service;

import com.rug.archedetector.dao.CommentRepository;
import com.rug.archedetector.dao.IssueListRepository;
import com.rug.archedetector.dao.IssueRepository;
import com.rug.archedetector.dao.TagRepository;
import com.rug.archedetector.exceptions.ResourceNotFoundException;
import com.rug.archedetector.model.CSVLine;
import com.rug.archedetector.model.Issue;
import com.rug.archedetector.model.Tag;
import com.rug.archedetector.util.ReindexHelpers;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
public class TagService {
    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private IssueListRepository issueListRepo;

    @Autowired
    private IssueRepository issueRepo;

    @Autowired
    private CommentRepository commentRepo;

    public List<Tag> getAll(){
        return tagRepository.findAll();
    }

    public Tag save(Tag tag){
        return tagRepository.save(tag);
    }

    /**
     * This function Deletes a tag from the database. First it checks if the tag exists.
     * Then deletes all relations to the other tables and after deletes itself.
     *
     * @param id a tag id
     * @return a response
     */
    public ResponseEntity<?> delete(Long id) {
        return tagRepository.findById(id).map(tag -> {
            tag.prepareForDelete();
            tagRepository.delete(tag);
            return ResponseEntity.ok().build();
        }).orElseThrow(() -> new ResourceNotFoundException("id " + id + " not found"));
    }

    // used in uploadTags
    private boolean isType(String[] line, int type) {
        if (line.length <= type) return false;
        return line[type].length() > 0;
    }

    /**
     * Set tags for a whole collection of issues at once, regardless of issue list
     */
    public void uploadTags(InputStream csv, String newTag) throws IOException, ParseException {
        // first: get all issue keys and which tags they need
        BufferedReader reader = new BufferedReader(new InputStreamReader(csv));
        String[] firstLine = reader.readLine().split(",");
        int issue_id = -1;
        int existence = -1;
        int property = -1;
        int executive = -1;
        int thisId = 0;
        for (String header : firstLine) {
            switch (header.toLowerCase()) {
                case "issue_id" -> issue_id = thisId;
                case "executive" -> executive = thisId;
                case "existence" -> existence = thisId;
                case "property" -> property = thisId;
            }
            thisId++;
        }

        String csvLine;
        List<CSVLine> lines = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        while ((csvLine = reader.readLine()) != null) {
            String[] row = csvLine.split(",");
            CSVLine line = new CSVLine();
            line.key = row[issue_id];
            line.existence = isType(row, existence);
            line.executive = isType(row, executive);
            line.property = isType(row, property);
            lines.add(line);
            keys.add(line.key);
        }

        csv.close();

        System.out.println("Done reading CSV, writing to repos");

        Tag existenceTag = tagRepository.findByName("Existence");
        Tag executiveTag = tagRepository.findByName("Executive");
        Tag propertyTag = tagRepository.findByName("Property");

        // find new tag if exists
        Tag newNameTag = tagRepository.findByName(newTag);
        if (newNameTag == null) {
            // create
            newNameTag = new Tag();
            newNameTag.setName(newTag);
            tagRepository.save(newNameTag);
            newNameTag = tagRepository.findByName(newTag);
        }

        Tag finalNewNameTag = newNameTag;

        lines.forEach(line -> {
            Issue issue = issueRepo.findByKey(line.key);

            Set<Tag> tags = issue.getTags();
            if (line.existence) tags.add(existenceTag);
            else tags.remove(existenceTag);

            if (line.property) tags.add(propertyTag);
            else tags.remove(propertyTag);

            if (line.executive) tags.add(executiveTag);
            else tags.remove(executiveTag);

            tags.add(finalNewNameTag);

            issue.setTags(tags);

            issueRepo.save(issue);
        });

        System.out.println("Done writing to repos, reindexing");

        ReindexHelpers.reindexPartial(keys, issueListRepo, issueRepo, commentRepo);

        System.out.println("Reindex done, issues tagged");
    }
}
