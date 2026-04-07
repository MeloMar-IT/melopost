package com.melomarit.melopost.service;

import com.melomarit.melopost.model.Story;
import com.melomarit.melopost.repository.StoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StoryService {
    @Autowired
    private StoryRepository repository;

    public List<Story> findAll() { return repository.findAll(); }
    public Optional<Story> findById(Long id) { return repository.findById(id); }
    public Story save(Story story) { return repository.save(story); }
    public void deleteById(Long id) { repository.deleteById(id); }
}
