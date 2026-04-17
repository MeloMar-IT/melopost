package com.melomarit.melopost.dto;

public class ApiEndpointDTO {
    private String method;
    private String url;
    private String description;
    private String exampleJson;

    public ApiEndpointDTO() {}

    public ApiEndpointDTO(String method, String url, String description) {
        this.method = method;
        this.url = url;
        this.description = description;
    }

    public ApiEndpointDTO(String method, String url, String description, String exampleJson) {
        this.method = method;
        this.url = url;
        this.description = description;
        this.exampleJson = exampleJson;
    }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExampleJson() { return exampleJson; }
    public void setExampleJson(String exampleJson) { this.exampleJson = exampleJson; }
}
