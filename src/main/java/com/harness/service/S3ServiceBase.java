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
}
