package com.melomarit.melopost.service;

import com.melomarit.melopost.model.IncidentNote;
import com.melomarit.melopost.repository.IncidentNoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IncidentNoteServiceTest {

    @Mock
    private IncidentNoteRepository repository;

    @InjectMocks
    private IncidentNoteService service;

    private IncidentNote note1;
    private IncidentNote note2;

    @BeforeEach
    void setUp() {
        note1 = new IncidentNote();
        note1.setUuid(UUID.randomUUID());
        note1.setIncidentRef("INC-100");
        note1.setContent("Note 1 content");

        note2 = new IncidentNote();
        note2.setUuid(UUID.randomUUID());
        note2.setIncidentRef("INC-200");
        note2.setContent("Note 2 content");
    }

    @Test
    void findAll_shouldReturnAll() {
        when(repository.findAll()).thenReturn(Arrays.asList(note1, note2));
        List<IncidentNote> result = service.findAll();
        assertEquals(2, result.size());
    }

    @Test
    void findById_shouldReturnNote() {
        when(repository.findById(note1.getUuid())).thenReturn(Optional.of(note1));
        IncidentNote result = service.findById(note1.getUuid());
        assertEquals("INC-100", result.getIncidentRef());
    }

    @Test
    void findById_notFound_shouldThrow() {
        UUID randomId = UUID.randomUUID();
        when(repository.findById(randomId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.findById(randomId));
    }

    @Test
    void findByIncidentRef_shouldFilter() {
        when(repository.findByIncidentRef("INC-100")).thenReturn(Arrays.asList(note1));
        List<IncidentNote> result = service.findByIncidentRef("INC-100");
        assertEquals(1, result.size());
        assertEquals("Note 1 content", result.get(0).getContent());
    }

    @Test
    void save_shouldSetDates() {
        when(repository.save(any(IncidentNote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        IncidentNote newNote = new IncidentNote();
        newNote.setIncidentRef("INC-NEW");
        
        IncidentNote saved = service.save(newNote);
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        // Since LocalDateTime.now() is called twice, they might differ by a few nanoseconds
        // or be exactly same depending on system clock resolution. 
        // We just care that both are set and updatedAt is not before createdAt
        assertFalse(saved.getUpdatedAt().isBefore(saved.getCreatedAt()));
    }

    @Test
    void save_existing_shouldUpdateDate() {
        LocalDateTime oldDate = LocalDateTime.now().minusDays(1);
        note1.setCreatedAt(oldDate);
        note1.setUpdatedAt(oldDate);
        
        when(repository.save(any(IncidentNote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        IncidentNote saved = service.save(note1);
        assertEquals(oldDate, saved.getCreatedAt());
        assertTrue(saved.getUpdatedAt().isAfter(oldDate));
    }

    @Test
    void deleteById_shouldCallRepository() {
        service.deleteById(note1.getUuid());
        verify(repository, times(1)).deleteById(note1.getUuid());
    }

    @Test
    void search_withKeyword_shouldCallRepository() {
        when(repository.findByIncidentRefContaining("INC")).thenReturn(Arrays.asList(note1, note2));
        List<IncidentNote> result = service.search("INC");
        assertEquals(2, result.size());
    }

    @Test
    void search_emptyKeyword_shouldReturnAll() {
        when(repository.findAll()).thenReturn(Arrays.asList(note1, note2));
        List<IncidentNote> result = service.search("");
        assertEquals(2, result.size());
    }

    @Test
    void save_shouldTruncateLongContent() {
        when(repository.save(any(IncidentNote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        IncidentNote longNote = new IncidentNote();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000010; i++) {
            sb.append("a");
        }
        longNote.setContent(sb.toString());
        longNote.setIncidentRef("INC-LONG");
        
        IncidentNote saved = service.save(longNote);
        assertEquals(1000000, saved.getContent().length());
    }
}
