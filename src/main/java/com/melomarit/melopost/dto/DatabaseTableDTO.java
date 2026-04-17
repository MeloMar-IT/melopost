package com.melomarit.melopost.dto;

import java.util.List;

public class DatabaseTableDTO {
    private String name;
    private String type; // "TABLE" or "TYPE"
    private Integer rowCount;
    private List<String> columns;

    public DatabaseTableDTO() {}

    public DatabaseTableDTO(String name, String type, Integer rowCount, List<String> columns) {
        this.name = name;
        this.type = type;
        this.rowCount = rowCount;
        this.columns = columns;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }
}
