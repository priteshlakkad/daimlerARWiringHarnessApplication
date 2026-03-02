package com.harness.dtos;

import java.util.List;

public class GetHarnessResponse {

    private String harnessId;
    private List<UploadedFileResponse> files;

    public GetHarnessResponse() {}

    public GetHarnessResponse(String harnessId, List<UploadedFileResponse> files) {
        this.harnessId = harnessId;
        this.files = files;
    }

    public String getHarnessId() { return harnessId; }
    public void setHarnessId(String harnessId) { this.harnessId = harnessId; }

    public List<UploadedFileResponse> getFiles() { return files; }
    public void setFiles(List<UploadedFileResponse> files) { this.files = files; }
}

