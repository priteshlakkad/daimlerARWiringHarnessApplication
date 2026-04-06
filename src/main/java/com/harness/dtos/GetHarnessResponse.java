package com.harness.dtos;

import java.util.List;

public class GetHarnessResponse {

    private String truckModel;
    private String harnessId;
    private List<UploadedFileResponse> files;

    public GetHarnessResponse() {
    }

    public GetHarnessResponse(String truckModel, String harnessId, List<UploadedFileResponse> files) {
        this.truckModel = truckModel;
        this.harnessId = harnessId;
        this.files = files;
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

    public List<UploadedFileResponse> getFiles() {
        return files;
    }

    public void setFiles(List<UploadedFileResponse> files) {
        this.files = files;
    }
}
