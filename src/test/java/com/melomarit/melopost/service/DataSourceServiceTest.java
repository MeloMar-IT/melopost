package com.melomarit.melopost.service;

import com.melomarit.melopost.model.DataSource;
import com.melomarit.melopost.repository.DataSourceRepository;
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
public class DataSourceServiceTest {

    @Mock
    private DataSourceRepository dataSourceRepository;

    @InjectMocks
    private DataSourceService dataSourceService;

    @Test
    void getAllDataSources_shouldReturnAll() {
        DataSource ds = new DataSource();
        when(dataSourceRepository.findAll()).thenReturn(Arrays.asList(ds));
        
        List<DataSource> result = dataSourceService.getAllDataSources();
        assertEquals(1, result.size());
    }

    @Test
    void getDataSourceById_shouldReturnDataSource() {
        UUID id = UUID.randomUUID();
        DataSource ds = new DataSource();
        when(dataSourceRepository.findById(id)).thenReturn(Optional.of(ds));
        
        Optional<DataSource> result = dataSourceService.getDataSourceById(id);
        assertTrue(result.isPresent());
    }

    @Test
    void saveDataSource_shouldSave() {
        DataSource ds = new DataSource();
        when(dataSourceRepository.save(ds)).thenReturn(ds);
        
        DataSource result = dataSourceService.saveDataSource(ds);
        assertNotNull(result);
        verify(dataSourceRepository).save(ds);
    }

    @Test
    void deleteDataSource_shouldDelete() {
        UUID id = UUID.randomUUID();
        dataSourceService.deleteDataSource(id);
        verify(dataSourceRepository).deleteById(id);
    }

    @Test
    void getDataSourcesByOperation_shouldFilter() {
        DataSource ds1 = new DataSource();
        ds1.setOperation("Read");
        DataSource ds2 = new DataSource();
        ds2.setOperation("Create");
        
        when(dataSourceRepository.findAll()).thenReturn(Arrays.asList(ds1, ds2));
        
        List<DataSource> result = dataSourceService.getDataSourcesByOperation("Read");
        assertEquals(1, result.size());
        assertEquals("Read", result.get(0).getOperation());
    }

    @Test
    void getTemplates_shouldReturnList() {
        List<DataSource> result = dataSourceService.getTemplates();
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(ds -> ds.getName().contains("Service NOW")));
    }
}
