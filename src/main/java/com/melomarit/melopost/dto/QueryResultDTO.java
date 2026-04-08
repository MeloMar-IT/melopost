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
public class QueryResultDTO {
    private String sql;
    private List<String> columns;
    private List<List<Object>> rows;
    private String message;
    private String error;
    private Integer affectedRows;
    private Boolean isSelect;

    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }
    public List<List<Object>> getRows() { return rows; }
    public void setRows(List<List<Object>> rows) { this.rows = rows; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Integer getAffectedRows() { return affectedRows; }
    public void setAffectedRows(Integer affectedRows) { this.affectedRows = affectedRows; }
    public Boolean getIsSelect() { return isSelect; }
    public void setIsSelect(Boolean isSelect) { this.isSelect = isSelect; }
}
