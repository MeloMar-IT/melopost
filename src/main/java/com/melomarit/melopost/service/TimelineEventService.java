package com.melo.melopost.service;

import com.melo.melopost.model.TimelineEvent;
import com.melo.melopost.repository.TimelineEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TimelineEventService {
    @Autowired
    private TimelineEventRepository repository;

    public List<TimelineEvent> findAll() { return repository.findAll(); }
    public Optional<TimelineEvent> findById(Long id) { return repository.findById(id); }
    public TimelineEvent save(TimelineEvent event) { return repository.save(event); }
    public void deleteById(Long id) { repository.deleteById(id); }
}
