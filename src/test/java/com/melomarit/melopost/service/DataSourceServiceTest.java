package com.melomarit.melopost.service;

import com.melomarit.melopost.model.DataSource;
import com.melomarit.melopost.repository.DataSourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.cql.CqlOperations;
import org.springframework.test.util.ReflectionTestUtils;
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

    @Mock
    private CassandraOperations cassandraOperations;

    @Mock
    private CqlOperations cqlOperations;

    @Spy
    private SecretCipher secretCipher = new SecretCipher("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", false);

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
    void saveDataSource_shouldStoreNewPasswordEncrypted() {
        DataSource ds = new DataSource();
        when(dataSourceRepository.save(ds)).thenReturn(ds);

        DataSource result = dataSourceService.saveDataSource(ds, "secret-123");

        assertTrue(result.getPassword().startsWith(SecretCipher.PREFIX));
        assertFalse(result.getPassword().contains("secret-123"));
        assertEquals("secret-123", dataSourceService.decryptPassword(result));
    }

    @Test
    void saveDataSource_shouldEncryptAgain_whenNewPasswordLooksEncrypted() {
        DataSource ds = new DataSource();
        when(dataSourceRepository.save(ds)).thenReturn(ds);

        DataSource result = dataSourceService.saveDataSource(ds, "enc:v1:abc");

        assertNotEquals("enc:v1:abc", result.getPassword());
        assertEquals("enc:v1:abc", dataSourceService.decryptPassword(result));
    }

    @Test
    void saveDataSource_shouldKeepStoredCiphertext_whenNewPasswordBlank() {
        DataSource ds = new DataSource();
        ds.setPassword("ignored");
        DataSource existing = new DataSource();
        existing.setUuid(ds.getUuid());
        existing.setPassword("enc:v1:stored");
        when(dataSourceRepository.findById(ds.getUuid())).thenReturn(Optional.of(existing));
        when(dataSourceRepository.save(ds)).thenReturn(ds);

        assertEquals("enc:v1:stored", dataSourceService.saveDataSource(ds, "").getPassword());
    }

    @Test
    void saveDataSource_shouldStoreNull_whenNewRowHasNoPassword() {
        DataSource ds = new DataSource();
        ds.setPassword("ignored");
        when(dataSourceRepository.findById(ds.getUuid())).thenReturn(Optional.empty());
        when(dataSourceRepository.save(ds)).thenReturn(ds);

        assertNull(dataSourceService.saveDataSource(ds, null).getPassword());
    }

    private DataSource withPassword(String password) {
        DataSource ds = new DataSource();
        ds.setPassword(password);
        return ds;
    }

    @Test
    void encryptPlaintextPasswords_shouldEncryptOnlyPlainRows() {
        DataSource plain = withPassword("plain-token");
        DataSource already = withPassword(secretCipher.encrypt("x"));
        DataSource empty = withPassword(null);
        when(dataSourceRepository.findAll()).thenReturn(List.of(plain, already, empty));
        when(cassandraOperations.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString(), any(Object[].class))).thenReturn(true);

        assertEquals(1, dataSourceService.encryptPlaintextPasswords());

        verify(cqlOperations, times(1)).execute(anyString(), any(Object[].class));
        verify(dataSourceRepository, never()).save(any());
    }

    @Test
    void encryptPlaintextPasswords_shouldNotCountRowsWhoseUpdateWasNotApplied() {
        when(dataSourceRepository.findAll()).thenReturn(List.of(withPassword("plain-token")));
        when(cassandraOperations.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString(), any(Object[].class))).thenReturn(false);

        assertEquals(0, dataSourceService.encryptPlaintextPasswords());
    }

    @Test
    void encryptPlaintextPasswords_shouldLeaveUndecryptablePrefixedRowsAlone() {
        when(dataSourceRepository.findAll()).thenReturn(List.of(withPassword("enc:v1:garbage")));

        assertEquals(0, dataSourceService.encryptPlaintextPasswords());
        verifyNoInteractions(cassandraOperations);
    }

    @Test
    void encryptPlaintextPasswords_shouldDoNothing_whenCipherDisabled() {
        ReflectionTestUtils.setField(dataSourceService, "secretCipher", new SecretCipher("", false));

        assertEquals(0, dataSourceService.encryptPlaintextPasswords());
        verifyNoInteractions(dataSourceRepository);
        verifyNoInteractions(cassandraOperations);
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
