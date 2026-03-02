package com.harness.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

public class UploadHarnessRequest {

    @Schema(type = "string", format = "binary", description = "Info PDF")
    private MultipartFile info;

    @Schema(type = "string", format = "binary", description = "Troubleshooting PDF")
    private MultipartFile troubleshooting;

    @Schema(type = "string", format = "binary", description = "Wiring PDF")
    private MultipartFile wiring;

    @Schema(type = "string", format = "binary", description = "Powerflow Video")
    private MultipartFile powerFlow;

    @Schema(type = "string", format = "binary", description = "Repair Video")
    private MultipartFile repair;

    @Schema(type = "string", format = "binary", description = "End devices JSON")
    private MultipartFile endDevices;

    @Schema(type = "string", format = "binary", description = "iOS Unity bundle")
    private MultipartFile iosBundle;

    @Schema(type = "array", description = "Knowledge PDFs")
    private MultipartFile[] knowledge;

    public MultipartFile getInfo() {
        return info;
    }

    public void setInfo(MultipartFile info) {
        this.info = info;
    }

    public MultipartFile getTroubleshooting() {
        return troubleshooting;
    }

    public void setTroubleshooting(MultipartFile troubleshooting) {
        this.troubleshooting = troubleshooting;
    }

    public MultipartFile getWiring() {
        return wiring;
    }

    public void setWiring(MultipartFile wiring) {
        this.wiring = wiring;
    }

    public MultipartFile getPowerFlow() {
        return powerFlow;
    }

    public void setPowerFlow(MultipartFile powerFlow) {
        this.powerFlow = powerFlow;
    }

    public MultipartFile getRepair() {
        return repair;
    }

    public void setRepair(MultipartFile repair) {
        this.repair = repair;
    }

    public MultipartFile getEndDevices() {
        return endDevices;
    }

    public void setEndDevices(MultipartFile endDevices) {
        this.endDevices = endDevices;
    }

    public MultipartFile getIosBundle() {
        return iosBundle;
    }

    public void setIosBundle(MultipartFile iosBundle) {
        this.iosBundle = iosBundle;
    }

    public MultipartFile[] getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(MultipartFile[] knowledge) {
        this.knowledge = knowledge;
    }
}

