package com.harness.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public abstract class S3ServiceBase {
        public abstract void upload(String key, MultipartFile file) throws Exception;

        public abstract List<String> listKeys(String truckModel, String harnessId);

        public abstract List<String> listAllDownloadableKeys(String truckModel);

        /**
         * Return troubleshooting + powerflow file keys across ALL truck model folders.
         */
        public abstract List<String> listAllDownloadableKeys();

        public abstract List<String> findKeysByHarnessId(String harnessId);

        /**
         * Return every file key stored under cdn/v1/ across ALL truck model folders.
         */
        public abstract List<String> listAllFiles();

        public abstract String presign(String key, int ttlSeconds);

        public abstract String getPublicUrl(String key);

        // ── Image (single-file slot at cdn/v1/images/) ───────────────────────────

        /** Upload and replace the single image stored in cdn/v1/images/. */
        public abstract void uploadImage(MultipartFile file) throws Exception;

        /** Return the raw bytes of the stored image, or throw if none exists. */
        public abstract byte[] downloadImage() throws Exception;

        /**
         * Return the storage key of the current image (e.g. "cdn/v1/images/image.jpg")
         * or {@code null} if no image has been uploaded yet.
         */
        public abstract String getImageKey();

        // ── GLTF / Truck Icon (per-truck slot at cdn/v1/{truckModel}/GLTF/) ──────

        /**
         * Upload (and replace if existing) the GLTF model file.
         * Stored at {@code cdn/v1/{truckModel}/GLTF/{truckModel}_model.gltf}.
         */
        public abstract void uploadGltf(String truckModel, MultipartFile gltfFile) throws Exception;

        /**
         * Upload (and replace if existing) the truck icon file.
         * Stored at {@code cdn/v1/{truckModel}/GLTF/{originalFilename}}.
         */
        public abstract void uploadTruckIcon(String truckModel, MultipartFile iconFile) throws Exception;

        /**
         * Return a map with keys {@code "gltfKey"} and {@code "iconKey"} pointing to
         * the respective storage keys, or {@code null} values if the files are absent.
         */
        public abstract java.util.Map<String, String> getGltfKeys(String truckModel);

        // ── EndDevice (per-device slot at cdn/v1/{truckModel}/enddevices/) ──

        /**
         * Upload an EndDevice file.
         * Stored at {@code cdn/v1/{truckModel}/enddevices/{originalFilename}}.
         */
        public abstract void uploadEndDeviceFile(String truckModel, MultipartFile file)
                        throws Exception;

        /**
         * Return all file keys in the EndDevice folder for the given truck model.
         */
        public abstract List<String> getEndDeviceFiles(String truckModel);

        /**
         * Delete a specific enddevice file.
         */
        public abstract void deleteEndDeviceFile(String truckModel, String fileName)
                        throws Exception;

        // ── FaultCodes (per-truck multi-file slot at cdn/v1/{truckModel}/faultcodes/)
        // ──

        /**
         * Upload a faultcode file.
         * Stored at {@code cdn/v1/{truckModel}/faultcodes/{originalFilename}}.
         */
        public abstract void uploadFaultCodeFile(String truckModel, MultipartFile file)
                        throws Exception;

        /**
         * Return all file keys in the FaultCodes folder for the given truck model.
         */
        public abstract List<String> getFaultCodeFiles(String truckModel);

        /**
         * Delete a specific faultcode file.
         */
        public abstract void deleteFaultCodeFile(String truckModel, String fileName)
                        throws Exception;

        /**
         * Return all file keys stored anywhere under cdn/v1/{truckModel}/.
         * Returns an empty list if no files exist for the given truck model.
         */
        public abstract List<String> listTruckFiles(String truckModel);

        // ── Truck model and harness listing ──────────────────────────────────────

        /**
         * Return all distinct truck model names that have at least one file
         * stored under cdn/v1/.
         */
        public abstract List<String> listTruckModels();

        /**
         * Return all distinct harness IDs stored under
         * cdn/v1/{truckModel}/harnesses/.
         */
        public abstract List<String> listHarnessIds(String truckModel);

        // ── Bulk deletions ────────────────────────────────────────────────────────

        /**
         * Delete every file stored under cdn/v1/{truckModel}/.
         * Covers harnesses, GLTF, enddevices, and faultcodes sub-trees.
         */
        public abstract void deleteTruck(String truckModel) throws Exception;

        /**
         * Delete every file stored under
         * cdn/v1/{truckModel}/harnesses/{harnessId}/.
         */
        public abstract void deleteHarness(String truckModel, String harnessId) throws Exception;

        /**
         * Delete the single file at
         * cdn/v1/{truckModel}/harnesses/{harnessId}/{category}/{fileName}.
         */
        public abstract void deleteHarnessFile(String truckModel, String harnessId,
                        String category, String fileName) throws Exception;

        /**
         * Delete every file under cdn/v1/{truckModel}/GLTF/ (both model and icon).
         */
        public abstract void deleteGltfFolder(String truckModel) throws Exception;

        /**
         * Delete every file under cdn/v1/{truckModel}/faultcodes/.
         */
        public abstract void deleteAllFaultCodeFiles(String truckModel) throws Exception;

        /**
         * Delete every file under cdn/v1/{truckModel}/enddevices/.
         */
        public abstract void deleteAllEndDeviceFiles(String truckModel) throws Exception;

        // ── WorkshopManual (per-truck multi-file slot at cdn/v1/{truckModel}/workshopmanual/) ──

        /**
         * Upload a workshop manual file.
         * Stored at {@code cdn/v1/{truckModel}/workshopmanual/{originalFilename}}.
         */
        public abstract void uploadWorkshopManualFile(String truckModel, MultipartFile file) throws Exception;

        /**
         * Return all file keys in the WorkshopManual folder for the given truck model.
         */
        public abstract List<String> getWorkshopManualFiles(String truckModel);

        /**
         * Delete a specific workshop manual file.
         */
        public abstract void deleteWorkshopManualFile(String truckModel, String fileName) throws Exception;

        /**
         * Delete every file under cdn/v1/{truckModel}/workshopmanual/.
         */
        public abstract void deleteAllWorkshopManualFiles(String truckModel) throws Exception;

        // ── 3D Model (per-harness slot at cdn/v1/{truckModel}/harnesses/{harnessId}/) ──

        /**
         * Upload (and replace if existing) a 3D model file for the given harness.
         * Stored at {@code cdn/v1/{truckModel}/harnesses/{harnessId}/{harnessId}.{ext}}.
         */
        public abstract com.harness.dtos.UploadedFileResponse upload3dModel(
                        String truckModel, String harnessId, MultipartFile file) throws Exception;
}
