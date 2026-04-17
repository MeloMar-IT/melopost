package com.melomarit.melopost.service;

import com.melomarit.melopost.model.IncidentNote;
import com.melomarit.melopost.repository.IncidentNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class IncidentNoteService {
    private final IncidentNoteRepository repository;

    public IncidentNoteService(IncidentNoteRepository repository) {
        this.repository = repository;
    }

    public List<IncidentNote> findAll() {
        return repository.findAll();
    }

    public IncidentNote findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Note not found"));
    }

    public List<IncidentNote> findByIncidentRef(String incidentRef) {
        return repository.findByIncidentRef(incidentRef);
    }

    public IncidentNote save(IncidentNote note) {
        if (note.getContent() != null && note.getContent().length() > 1000000) {
            note.setContent(note.getContent().substring(0, 1000000));
        }
        if (note.getCreatedAt() == null) {
            note.onCreate();
        } else {
            note.onUpdate();
        }
        return repository.save(note);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    public List<IncidentNote> search(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return repository.findAll();
        }
        return repository.findByIncidentRefContaining(keyword);
    }
}
