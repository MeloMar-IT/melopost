package com.melomarit.melopost.service;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.PostmortemDocument;
import com.melomarit.melopost.repository.PostmortemDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class PostmortemDocumentService {
    private final PostmortemDocumentRepository repository;

    public PostmortemDocumentService(PostmortemDocumentRepository repository) {
        this.repository = repository;
    }

    public List<PostmortemDocument> findAll() {
        return repository.findAll();
    }

    public Optional<PostmortemDocument> findById(Long id) {
        return repository.findById(id);
    }

    public List<PostmortemDocument> findByPostmortemId(Long postmortemId) {
        return repository.findByPostmortemId(postmortemId);
    }

    public PostmortemDocument save(PostmortemDocument document) {
        return repository.save(document);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public void uploadDocument(Postmortem postmortem, MultipartFile file) throws IOException {
        if (!file.isEmpty()) {
            PostmortemDocument doc = new PostmortemDocument();
            doc.setFileName(file.getOriginalFilename());
            doc.setContentType(file.getContentType());
            doc.setSize(file.getSize());
            doc.setData(file.getBytes());
            doc.setPostmortem(postmortem);
            pmAddDocument(postmortem, doc);
            repository.save(doc);
        }
    }

    public void processFiles(Postmortem postmortem, MultipartFile[] files) throws IOException {
        if (files != null) {
            for (MultipartFile file : files) {
                uploadDocument(postmortem, file);
            }
        }
    }

    private void pmAddDocument(Postmortem postmortem, PostmortemDocument doc) {
        if (postmortem.getDocuments() == null) {
            postmortem.setDocuments(new java.util.ArrayList<>());
        }
        postmortem.getDocuments().add(doc);
    }
}
