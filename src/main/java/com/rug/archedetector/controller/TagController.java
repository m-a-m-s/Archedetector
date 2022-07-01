package com.rug.archedetector.controller;

import com.rug.archedetector.model.MailingList;
import com.rug.archedetector.model.Tag;
import com.rug.archedetector.service.MailingListService;
import com.rug.archedetector.service.TagService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


@RestController
public class TagController {
    @Autowired
    private TagService tagService;

    @GetMapping("/tag")
    public List<Tag> getTags() {
        return tagService.getAll();
    }

    @PostMapping("/tag")
    public Tag addTag(@RequestBody Tag tag) {
        return tagService.save(tag);
    }

    @DeleteMapping("/tag/{id}")
    public ResponseEntity<?> deleteTag(@PathVariable long id) {
        return tagService.delete(id);
    }

    @PostMapping(path = "/tag/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadTags(@RequestBody MultipartFile file, String newTag) throws IOException, ParseException {
        tagService.uploadTags(file.getInputStream(), newTag);
    }
}
