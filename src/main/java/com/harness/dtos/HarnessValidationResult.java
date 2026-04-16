package com.harness.dtos;

import java.util.List;

public class HarnessValidationResult {

    private String truckModel;
    private String harnessId;
    private boolean valid;
    private List<String> missingFiles;

    public HarnessValidationResult() {}

    public HarnessValidationResult(String truckModel, String harnessId,
            boolean valid, List<String> missingFiles) {
        this.truckModel = truckModel;
        this.harnessId = harnessId;
        this.valid = valid;
        this.missingFiles = missingFiles;
    }

    public String getTruckModel() { return truckModel; }
    public void setTruckModel(String truckModel) { this.truckModel = truckModel; }

    public String getHarnessId() { return harnessId; }
    public void setHarnessId(String harnessId) { this.harnessId = harnessId; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<String> getMissingFiles() { return missingFiles; }
    public void setMissingFiles(List<String> missingFiles) { this.missingFiles = missingFiles; }
}
