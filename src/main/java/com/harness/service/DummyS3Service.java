package com.harness.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Log4j2
@ConditionalOnProperty(name = "app.use-s3", havingValue = "nonexistent")
public class DummyS3Service extends S3ServiceBase {

    @Override
    public void upload(String key, MultipartFile file) {
        log.info("S3 is disabled. Skipping upload for key={}", key);
    }

    @Override
    public List<String> listKeys(String truckModel, String harnessId) {
        log.debug("S3 is disabled. listKeys called for truckModel={}, harnessId={}", truckModel, harnessId);
        return List.of();
    }

    @Override
    public List<String> listAllDownloadableKeys(String truckModel) {
        log.debug("S3 is disabled. listAllDownloadableKeys called for truckModel={}", truckModel);
        return List.of();
    }

    @Override
    public List<String> listAllDownloadableKeys() {
        log.debug("S3 is disabled. listAllDownloadableKeys (all trucks) called.");
        return List.of();
    }

    @Override
    public List<String> findKeysByHarnessId(String harnessId) {
        log.debug("S3 is disabled. findKeysByHarnessId called for harnessId={}", harnessId);
        return List.of();
    }

    @Override
    public List<String> listAllFiles() {
        log.debug("S3 is disabled. listAllFiles called.");
        return List.of();
    }

    @Override
    public String presign(String key, int ttlSeconds) {
        log.warn("S3 is disabled. Presign requested for key={}, ttlSeconds={}", key, ttlSeconds);
        return "S3_DISABLED";
    }

    @Override
    public String getPublicUrl(String key) {
        return "S3_DISABLED";
    }

    @Override
    public void uploadImage(MultipartFile file) {
        log.warn("S3 is disabled. uploadImage skipped.");
    }

    @Override
    public byte[] downloadImage() throws Exception {
        throw new Exception("S3 is disabled. No image available.");
    }

    @Override
    public String getImageKey() {
        return null;
    }

    @Override
    public void uploadGltf(String truckModel, MultipartFile gltfFile) {
        log.warn("S3 is disabled. uploadGltf skipped for truckModel={}", truckModel);
    }

    @Override
    public void uploadTruckIcon(String truckModel, MultipartFile iconFile) {
        log.warn("S3 is disabled. uploadTruckIcon skipped for truckModel={}", truckModel);
    }

    @Override
    public java.util.Map<String, String> getGltfKeys(String truckModel) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("gltfKey", null);
        result.put("iconKey", null);
        return result;
    }

    @Override
    public void uploadEndDeviceFile(String truckModel, MultipartFile file) {
        log.warn("S3 is disabled. uploadEndDeviceFile skipped for truckModel={}", truckModel);
    }

    @Override
    public List<String> getEndDeviceFiles(String truckModel) {
        log.debug("S3 is disabled. getEndDeviceFiles called for truckModel={}", truckModel);
        return List.of();
    }

    @Override
    public void deleteEndDeviceFile(String truckModel, String fileName) {
        log.warn("S3 is disabled. deleteEndDeviceFile skipped for truckModel={}, fileName={}", truckModel, fileName);
    }

    @Override
    public void uploadFaultCodeFile(String truckModel, MultipartFile file) {
        log.warn("S3 is disabled. uploadFaultCodeFile skipped for truckModel={}", truckModel);
    }

    @Override
    public List<String> getFaultCodeFiles(String truckModel) {
        log.debug("S3 is disabled. getFaultCodeFiles called for truckModel={}", truckModel);
        return List.of();
    }

    @Override
    public void deleteFaultCodeFile(String truckModel, String fileName) {
        log.warn("S3 is disabled. deleteFaultCodeFile skipped for truckModel={}, fileName={}", truckModel, fileName);
    }

    @Override
    public List<String> listTruckFiles(String truckModel) {
        log.debug("S3 is disabled. listTruckFiles called for truckModel={}", truckModel);
        return List.of();
    }

    @Override
    public List<String> listTruckModels() {
        log.debug("S3 is disabled. listTruckModels called.");
        return List.of();
    }

    @Override
    public List<String> listHarnessIds(String truckModel) {
        log.debug("S3 is disabled. listHarnessIds called for truckModel={}", truckModel);
        return List.of();
    }

    @Override
    public void deleteTruck(String truckModel) {
        log.warn("S3 is disabled. deleteTruck skipped for truckModel={}", truckModel);
    }

    @Override
    public void deleteHarness(String truckModel, String harnessId) {
        log.warn("S3 is disabled. deleteHarness skipped for truckModel={}, harnessId={}", truckModel, harnessId);
    }

    @Override
    public void deleteHarnessFile(String truckModel, String harnessId, String category, String fileName) {
        log.warn("S3 is disabled. deleteHarnessFile skipped for truckModel={}, harnessId={}, category={}, fileName={}",
                truckModel, harnessId, category, fileName);
    }

    @Override
    public void deleteGltfFolder(String truckModel) {
        log.warn("S3 is disabled. deleteGltfFolder skipped for truckModel={}", truckModel);
    }

    @Override
    public void deleteAllFaultCodeFiles(String truckModel) {
        log.warn("S3 is disabled. deleteAllFaultCodeFiles skipped for truckModel={}", truckModel);
    }

    @Override
    public void deleteAllEndDeviceFiles(String truckModel) {
        log.warn("S3 is disabled. deleteAllEndDeviceFiles skipped for truckModel={}", truckModel);
    }

    @Override
    public void uploadWorkshopManualFile(String truckModel, MultipartFile file) {
        log.warn("S3 is disabled. uploadWorkshopManualFile skipped for truckModel={}", truckModel);
    }

    @Override
    public List<String> getWorkshopManualFiles(String truckModel) {
        log.debug("S3 is disabled. getWorkshopManualFiles called for truckModel={}", truckModel);
        return List.of();
    }

    @Override
    public void deleteWorkshopManualFile(String truckModel, String fileName) {
        log.warn("S3 is disabled. deleteWorkshopManualFile skipped for truckModel={}, fileName={}", truckModel, fileName);
    }

    @Override
    public void deleteAllWorkshopManualFiles(String truckModel) {
        log.warn("S3 is disabled. deleteAllWorkshopManualFiles skipped for truckModel={}", truckModel);
    }

    @Override
    public com.harness.dtos.UploadedFileResponse upload3dModel(
            String truckModel, String harnessId, org.springframework.web.multipart.MultipartFile file) {
        log.warn("S3 is disabled. upload3dModel skipped for truckModel={}, harnessId={}", truckModel, harnessId);
        return new com.harness.dtos.UploadedFileResponse("", "");
    }
}
