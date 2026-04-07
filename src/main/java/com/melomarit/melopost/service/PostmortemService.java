package com.melo.melopost.service;

import com.melo.melopost.model.Postmortem;
import com.melo.melopost.repository.PostmortemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PostmortemService {
    private final PostmortemRepository repository;

    public PostmortemService(PostmortemRepository repository) {
        this.repository = repository;
    }

    public List<Postmortem> findAll() {
        return repository.findAll();
    }

    public List<Postmortem> findRecent() {
        return repository.findTop5ByOrderByCreatedAtDesc();
    }

    public Postmortem findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Postmortem not found"));
    }

    public Postmortem save(Postmortem postmortem) {
        return repository.save(postmortem);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<Postmortem> search(String keyword) {
        return repository.search(keyword);
    }
}
