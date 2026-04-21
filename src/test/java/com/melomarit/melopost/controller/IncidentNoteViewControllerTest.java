package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.IncidentNote;
import com.melomarit.melopost.service.IncidentNoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IncidentNoteViewControllerTest {

    @Mock
    private IncidentNoteService service;

    @Mock
    private Model model;

    @InjectMocks
    private IncidentNoteViewController controller;

    private IncidentNote note;
    private UUID noteId;

    @BeforeEach
    void setUp() {
        noteId = UUID.randomUUID();
        note = new IncidentNote();
        note.setUuid(noteId);
        note.setIncidentRef("INC-123");
        note.setContent("Test content");
    }

    @Test
    void migrate_shouldRedirectToPostmortemNew() {
        when(service.findById(noteId)).thenReturn(note);
        
        String result = controller.migrate(noteId);
        
        assertEquals("redirect:/postmortems/new?incidentRef=INC-123&note=Test content", result);
    }

    @Test
    void showCreateForm_shouldAddEmptyNoteToModel() {
        String view = controller.showCreateForm(model);
        
        assertEquals("incident-notes/form", view);
        org.mockito.ArgumentCaptor<IncidentNote> captor = org.mockito.ArgumentCaptor.forClass(IncidentNote.class);
        org.mockito.Mockito.verify(model).addAttribute(org.mockito.ArgumentMatchers.eq("note"), captor.capture());
        
        IncidentNote capturedNote = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNull(capturedNote.getUuid(), "New note should have null UUID");
    }
}
