package com.melomarit.melopost.service;

import com.melomarit.melopost.model.DataSource;
import com.melomarit.melopost.repository.DataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
public class DataSourceService {

    private static final Logger log = LoggerFactory.getLogger(DataSourceService.class);

    private final DataSourceRepository dataSourceRepository;
    private SecretCipher secretCipher;
    private final CassandraOperations cassandraOperations;

    @Autowired
    public DataSourceService(DataSourceRepository dataSourceRepository, SecretCipher secretCipher,
                             CassandraOperations cassandraOperations) {
        this.dataSourceRepository = dataSourceRepository;
        this.secretCipher = secretCipher;
        this.cassandraOperations = cassandraOperations;
    }

    public List<DataSource> getAllDataSources() {
        return dataSourceRepository.findAll();
    }

    public Optional<DataSource> getDataSourceById(UUID id) {
        return dataSourceRepository.findById(id);
    }

    /**
     * Saves a data source. The password in the entity is ignored: a non-blank {@code newPassword} is
     * stored encrypted (always, whatever it looks like); a blank one keeps the stored value (null for a new row).
     */
    public DataSource saveDataSource(DataSource dataSource, String newPassword) {
        if (newPassword != null && !newPassword.isEmpty()) {
            dataSource.setPassword(secretCipher.encrypt(newPassword));
        } else {
            String stored = null;
            if (dataSource.getUuid() != null) {
                stored = dataSourceRepository.findById(dataSource.getUuid()).map(DataSource::getPassword).orElse(null);
            }
            dataSource.setPassword(stored);
        }
        return dataSourceRepository.save(dataSource);
    }

    /** Returns the plain token of a data source (for outbound use only; never expose it). */
    public String decryptPassword(DataSource dataSource) {
        return secretCipher.decrypt(dataSource.getPassword());
    }

    /**
     * Encrypts tokens still stored in plain text, one conditional update of the password column per row,
     * so a concurrent change or delete is never overwritten. Returns the number of rows encrypted.
     */
    public int encryptPlaintextPasswords() {
        if (!secretCipher.isEnabled()) {
            return 0;
        }
        int encrypted = 0;
        for (DataSource ds : dataSourceRepository.findAll()) {
            String stored = ds.getPassword();
            if (stored == null || stored.isBlank()) {
                continue;
            }
            if (secretCipher.isEncrypted(stored)) {
                try {
                    secretCipher.decrypt(stored);
                } catch (IllegalStateException e) {
                    log.warn("[DATASOURCE_KEY] Stored token of data source {} starts with '{}' but cannot be decrypted with the current key; left untouched.",
                            ds.getUuid(), SecretCipher.PREFIX);
                }
                continue;
            }
            String enc = secretCipher.encrypt(stored);
            boolean applied = cassandraOperations.getCqlOperations().execute(
                    "UPDATE data_source SET password = ? WHERE uuid = ? IF password = ?", enc, ds.getUuid(), stored);
            if (applied) {
                encrypted++;
            } else {
                log.info("[DATASOURCE_KEY] Data source {} changed or was deleted meanwhile; skipped, retried on next start.", ds.getUuid());
            }
        }
        return encrypted;
    }

    public void deleteDataSource(UUID id) {
        dataSourceRepository.deleteById(id);
    }

    public List<DataSource> getDataSourcesByOperation(String operation) {
        return dataSourceRepository.findAll().stream()
                .filter(ds -> operation.equalsIgnoreCase(ds.getOperation()))
                .toList();
    }

    public List<DataSource> getTemplates() {
        List<DataSource> templates = new ArrayList<>();
        
        templates.add(createTemplate("Service NOW read", "ServiceNow", "Read", "https://instance.service-now.com/api/now/table/incident"));
        templates.add(createTemplate("Services NOW new record", "ServiceNow", "Create", "https://instance.service-now.com/api/now/table/incident"));
        templates.add(createTemplate("IJRA read", "Jira", "Read", "https://your-domain.atlassian.net/rest/api/3/search"));
        templates.add(createTemplate("IJRA create new record", "Jira", "Create", "https://your-domain.atlassian.net/rest/api/3/issue"));
        templates.add(createTemplate("Azure DEV/OPS read", "Azure DevOps", "Read", "https://dev.azure.com/{organization}/{project}/_apis/wit/workitems"));
        templates.add(createTemplate("Azure DEV/OPS create new record", "Azure DevOps", "Create", "https://dev.azure.com/{organization}/{project}/_apis/wit/workitems/${type}"));
        
        return templates;
    }

    private DataSource createTemplate(String name, String type, String operation, String url) {
        DataSource ds = new DataSource();
        ds.setName(name);
        ds.setType(type);
        ds.setOperation(operation);
        ds.setUrl(url);
        ds.setDescription("Template for " + name);
        return ds;
    }
}
