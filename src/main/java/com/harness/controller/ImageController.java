package com.harness.controller;

import com.harness.model.ImageMetadata;
import com.harness.repository.ImageMetadataRepository;
import com.harness.service.S3ServiceBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Image Management API Controller
 *
 * Base path: /images
 *
 * Endpoints:
 * POST /images/upload — Upload (and replace) the single image stored at
 * cdn/v1/images/
 * GET /images/download — Download the current image
 * GET /images/info — Check whether an image exists (JSON: { exists, key })
 */
@RestController
@RequestMapping("/images")
@Log4j2
@Tag(name = "Image Management", description = "Endpoints for managing global application images")
public class ImageController {

    private final S3ServiceBase storage;
    private final ImageMetadataRepository metadataRepository;

    public ImageController(S3ServiceBase storage, ImageMetadataRepository metadataRepository) {
        this.storage = storage;
        this.metadataRepository = metadataRepository;
    }

    // ── POST /images/upload ────────────────────────────────────────────────────

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload / replace image", description = "Uploads an image to cdn/v1/images/. Any previously stored image is deleted first.", responses = {
            @ApiResponse(responseCode = "200", description = "Image uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "No file provided"),
            @ApiResponse(responseCode = "500", description = "Upload failed")
    })
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("image") MultipartFile image,
            @Parameter(description = "Image type: onroad or offroad") @RequestParam(value = "type", required = false) String type) {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "No image file provided"));
        }

        try {
            storage.uploadImage(image);
            String key = storage.getImageKey();

            // Store metadata in H2
            if (type != null && !type.isEmpty()) {
                Optional<ImageMetadata> existing = metadataRepository.findByImageKey(key);
                ImageMetadata metadata = existing.orElse(new ImageMetadata());
                metadata.setImageKey(key);
                metadata.setType(type);
                metadataRepository.save(metadata);
                log.info("Stored metadata for image key={}: type={}", key, type);
            }

            String url = storage.getPublicUrl(key);
            log.info("Image uploaded successfully, key={}, url={}", key, url);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Image uploaded successfully",
                    "key", key != null ? key : "",
                    "url", url != null ? url : "",
                    "type", type != null ? type : ""));
        } catch (Exception e) {
            log.error("Failed to upload image", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Upload failed: " + e.getMessage()));
        }
    }

    // ── GET /images/download ───────────────────────────────────────────────────

    @GetMapping("/download")
    @Operation(summary = "Download the current image", description = "Returns the raw image bytes with the correct Content-Type header.", responses = {
            @ApiResponse(responseCode = "200", description = "Image returned"),
            @ApiResponse(responseCode = "404", description = "No image stored"),
            @ApiResponse(responseCode = "500", description = "Download failed")
    })
    public ResponseEntity<byte[]> downloadImage() {
        String key = storage.getImageKey();
        if (key == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] bytes = storage.downloadImage();
            MediaType mediaType = resolveMediaType(key);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(bytes);
        } catch (Exception e) {
            log.error("Failed to download image", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── GET /images/info ───────────────────────────────────────────────────────

    @GetMapping("/info")
    @Operation(summary = "Check whether an image exists", description = "Returns JSON with 'exists' boolean, 'key', and 'url' of the stored image.", responses = {
            @ApiResponse(responseCode = "200", description = "Info returned")
    })
    public ResponseEntity<Map<String, Object>> imageInfo() {
        String key = storage.getImageKey();
        if (key == null) {
            return ResponseEntity.ok(Map.of("exists", false, "key", "", "url", "", "type", ""));
        }
        String url = storage.getPublicUrl(key);

        Map<String, Object> response = new HashMap<>();
        response.put("exists", true);
        response.put("key", key);
        response.put("url", url);

        metadataRepository.findByImageKey(key).ifPresent(m -> response.put("type", m.getType()));
        if (!response.containsKey("type")) {
            response.put("type", "");
        }

        return ResponseEntity.ok(response);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private MediaType resolveMediaType(String key) {
        String lowerKey = key.toLowerCase();
        if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg"))
            return MediaType.IMAGE_JPEG;
        if (lowerKey.endsWith(".png"))
            return MediaType.IMAGE_PNG;
        if (lowerKey.endsWith(".gif"))
            return MediaType.IMAGE_GIF;
        if (lowerKey.endsWith(".webp"))
            return MediaType.valueOf("image/webp");
        if (lowerKey.endsWith(".bmp"))
            return MediaType.valueOf("image/bmp");
        if (lowerKey.endsWith(".svg"))
            return MediaType.valueOf("image/svg+xml");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
