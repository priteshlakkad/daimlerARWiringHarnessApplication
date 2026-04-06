package com.harness.dtos;

import java.util.List;

public class UploadHarnessResponse {

    private String truckModel;
    private String harnessId;
    private List<String> uploadedKeys;
    private List<UploadedFileResponse> uploadedFiles;

    public UploadHarnessResponse() {
    }

    public UploadHarnessResponse(String truckModel, String harnessId, List<String> uploadedKeys) {
        this.truckModel = truckModel;
        this.harnessId = harnessId;
        this.uploadedKeys = uploadedKeys;
    }

    public UploadHarnessResponse(String truckModel, String harnessId, List<String> uploadedKeys,
            List<UploadedFileResponse> uploadedFiles) {
        this.truckModel = truckModel;
        this.harnessId = harnessId;
        this.uploadedKeys = uploadedKeys;
        this.uploadedFiles = uploadedFiles;
    }

    public String getTruckModel() {
        return truckModel;
    }

    public void setTruckModel(String truckModel) {
        this.truckModel = truckModel;
    }

    public String getHarnessId() {
        return harnessId;
    }

    public void setHarnessId(String harnessId) {
        this.harnessId = harnessId;
    }

    public List<String> getUploadedKeys() {
        return uploadedKeys;
    }

    public void setUploadedKeys(List<String> uploadedKeys) {
        this.uploadedKeys = uploadedKeys;
    }

    public List<UploadedFileResponse> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(List<UploadedFileResponse> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }
}
