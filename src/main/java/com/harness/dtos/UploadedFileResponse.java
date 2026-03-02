package com.harness.dtos;

public class UploadedFileResponse {

    private String key;
    private String url;

    public UploadedFileResponse() {}

    public UploadedFileResponse(String key, String url) {
        this.key = key;
        this.url = url;
    }

    // Getters & Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}

