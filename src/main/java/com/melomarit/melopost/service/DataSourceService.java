package com.melomarit.melopost.service;

import com.melomarit.melopost.model.DataSource;
import com.melomarit.melopost.repository.DataSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
public class DataSourceService {

    @Autowired
    private DataSourceRepository dataSourceRepository;

    public List<DataSource> getAllDataSources() {
        return dataSourceRepository.findAll();
    }

    public Optional<DataSource> getDataSourceById(UUID id) {
        return dataSourceRepository.findById(id);
    }

    public DataSource saveDataSource(DataSource dataSource) {
        return dataSourceRepository.save(dataSource);
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
