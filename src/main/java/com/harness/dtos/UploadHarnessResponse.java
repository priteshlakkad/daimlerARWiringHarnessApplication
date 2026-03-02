package com.harness.dtos;

import java.util.List;


public class UploadHarnessResponse {

    private String harnessId;
    private List<String> uploadedKeys;

    public UploadHarnessResponse() {}

    public UploadHarnessResponse(String harnessId, List<String> uploadedKeys) {
        this.harnessId = harnessId;
        this.uploadedKeys = uploadedKeys;
    }

    public String getHarnessId() { return harnessId; }
    public void setHarnessId(String harnessId) { this.harnessId = harnessId; }

    public List<String> getUploadedKeys() { return uploadedKeys; }
    public void setUploadedKeys(List<String> uploadedKeys) { this.uploadedKeys = uploadedKeys; }
}

