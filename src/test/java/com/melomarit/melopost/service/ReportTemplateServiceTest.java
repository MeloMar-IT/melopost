package com.melomarit.melopost.service;

import com.melomarit.melopost.model.ReportTemplate;
import com.melomarit.melopost.repository.ReportTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportTemplateServiceTest {

    @Mock
    private ReportTemplateRepository repository;

    @InjectMocks
    private ReportTemplateService service;

    @Test
    void findAll_shouldReturnAll() {
        when(repository.findAll()).thenReturn(Arrays.asList(new ReportTemplate()));
        List<ReportTemplate> result = service.findAll();
        assertEquals(1, result.size());
    }

    @Test
    void findById_shouldReturnTemplate() {
        UUID id = UUID.randomUUID();
        ReportTemplate template = new ReportTemplate();
        when(repository.findById(id)).thenReturn(Optional.of(template));
        
        Optional<ReportTemplate> result = service.findById(id);
        assertTrue(result.isPresent());
    }

    @Test
    void findDefault_shouldReturnDefault() {
        ReportTemplate template = new ReportTemplate();
        when(repository.findByIsDefaultTrue()).thenReturn(Optional.of(template));
        
        Optional<ReportTemplate> result = service.findDefault();
        assertTrue(result.isPresent());
    }

    @Test
    void save_shouldHandleDefaultFlag() {
        UUID oldId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        
        ReportTemplate oldDefault = new ReportTemplate();
        oldDefault.setUuid(oldId);
        oldDefault.setDefault(true);
        
        ReportTemplate newDefault = new ReportTemplate();
        newDefault.setUuid(newId);
        newDefault.setDefault(true);
        
        when(repository.findByIsDefaultTrue()).thenReturn(Optional.of(oldDefault));
        when(repository.save(any(ReportTemplate.class))).thenAnswer(i -> i.getArguments()[0]);
        
        ReportTemplate saved = service.save(newDefault);
        
        assertTrue(saved.isDefault());
        assertFalse(oldDefault.isDefault());
        verify(repository).save(oldDefault);
        verify(repository).save(newDefault);
    }

    @Test
    void deleteById_shouldCallRepository() {
        UUID id = UUID.randomUUID();
        service.deleteById(id);
        verify(repository).deleteById(id);
    }
}
