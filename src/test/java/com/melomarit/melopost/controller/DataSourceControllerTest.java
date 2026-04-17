package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.DataSource;
import com.melomarit.melopost.service.DataSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataSourceControllerTest {

    @Mock
    private DataSourceService dataSourceService;

    @InjectMocks
    private DataSourceController dataSourceController;

    private UUID dsId;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dsId = UUID.randomUUID();
        dataSource = new DataSource();
        dataSource.setUuid(dsId);
        dataSource.setName("Test Source");
    }

    @Test
    void getAll_shouldReturnList() {
        when(dataSourceService.getAllDataSources()).thenReturn(Arrays.asList(dataSource));
        List<DataSource> result = dataSourceController.getAll();
        assertEquals(1, result.size());
    }

    @Test
    void getById_shouldReturnDataSource() {
        when(dataSourceService.getDataSourceById(dsId)).thenReturn(Optional.of(dataSource));
        ResponseEntity<DataSource> result = dataSourceController.getById(dsId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(dataSource, result.getBody());
    }

    @Test
    void create_shouldSaveDataSource() {
        when(dataSourceService.saveDataSource(any(DataSource.class))).thenReturn(dataSource);
        DataSource result = dataSourceController.create(new DataSource());
        assertNotNull(result);
        verify(dataSourceService).saveDataSource(any(DataSource.class));
    }

    @Test
    void update_shouldUpdateExisting() {
        when(dataSourceService.getDataSourceById(dsId)).thenReturn(Optional.of(dataSource));
        when(dataSourceService.saveDataSource(any(DataSource.class))).thenReturn(dataSource);
        
        DataSource updateData = new DataSource();
        updateData.setName("Updated Source");
        
        ResponseEntity<DataSource> result = dataSourceController.update(dsId, updateData);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Updated Source", dataSource.getName());
    }

    @Test
    void delete_shouldCallService() {
        when(dataSourceService.getDataSourceById(dsId)).thenReturn(Optional.of(dataSource));
        ResponseEntity<Void> result = dataSourceController.delete(dsId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(dataSourceService).deleteDataSource(dsId);
    }
}
