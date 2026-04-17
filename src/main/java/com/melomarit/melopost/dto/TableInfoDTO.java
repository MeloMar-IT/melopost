package com.melomarit.melopost.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableInfoDTO {
    private String name;
    private String type; // "TABLE" or "TYPE"
    private List<Map<String, Object>> schema;
    private List<String> primaryKeys;
    private QueryResultDTO data;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<Map<String, Object>> getSchema() { return schema; }
    public void setSchema(List<Map<String, Object>> schema) { this.schema = schema; }
    public List<String> getPrimaryKeys() { return primaryKeys; }
    public void setPrimaryKeys(List<String> primaryKeys) { this.primaryKeys = primaryKeys; }
    public QueryResultDTO getData() { return data; }
    public void setData(QueryResultDTO data) { this.data = data; }
}
