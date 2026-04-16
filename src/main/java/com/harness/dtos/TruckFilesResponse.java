package com.harness.dtos;

import java.util.Map;

public class TruckFilesResponse {

    private String truckId;
    private Map<String, Object> files;

    public TruckFilesResponse(String truckId, Map<String, Object> files) {
        this.truckId = truckId;
        this.files = files;
    }

    public String getTruckId() {
        return truckId;
    }

    public Map<String, Object> getFiles() {
        return files;
    }
}
