package com.harness.controller;

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

/**
 * GLTF & Truck Icon Management API
 *
 * Base path: /gltf/{truckModel}
 *
 * Endpoints:
 * POST /gltf/{truckModel}/upload — Upload GLTF model file and/or truck icon.
 * At least one file must be provided. Files are replaced if they already exist.
 * Storage paths:
 * cdn/v1/{truckModel}/GLTF/{truckModel}_model.gltf
 * cdn/v1/{truckModel}/GLTF/{originalIconFilename}
 *
 * GET /gltf/{truckModel}/info — Return S3 keys and public URLs for both files
 * (null/empty if a file has not been uploaded yet).
 */
@RestController
@RequestMapping("/gltf/{truckModel}")
@Log4j2
@Tag(name = "GLTF & Icons", description = "Endpoints for managing GLTF 3D models and truck icons")
public class GltfController {

    private final S3ServiceBase storage;

    public GltfController(S3ServiceBase storage) {
        this.storage = storage;
    }

    // ── POST /gltf/{truckModel}/upload ────────────────────────────────────────

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload GLTF model and/or truck icon", description = "Uploads a GLTF model file and/or a truck icon for the given truck model. "
            + "At least one file must be supplied. Existing files at the target paths are replaced. "
            + "GLTF is stored at cdn/v1/{truckModel}/GLTF/{truckModel}_model.gltf; "
            + "icon is stored at cdn/v1/{truckModel}/GLTF/{originalFilename}.", responses = {
                    @ApiResponse(responseCode = "200", description = "File(s) uploaded successfully"),
                    @ApiResponse(responseCode = "400", description = "No files provided"),
                    @ApiResponse(responseCode = "500", description = "Upload failed")
            })
    public ResponseEntity<Map<String, Object>> uploadGltf(
            @Parameter(description = "Truck model identifier") @PathVariable String truckModel,
            @Parameter(description = "GLTF model file (.gltf or .glb) — optional") @RequestParam(value = "gltf", required = false) MultipartFile gltf,
            @Parameter(description = "Truck icon image file — optional") @RequestParam(value = "icon", required = false) MultipartFile icon) {

        boolean hasGltf = gltf != null && !gltf.isEmpty();
        boolean hasIcon = icon != null && !icon.isEmpty();

        if (!hasGltf && !hasIcon) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false,
                            "message", "At least one file (gltf or icon) must be provided"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("truckModel", truckModel);

        try {
            if (hasGltf) {
                storage.uploadGltf(truckModel, gltf);
                String gltfKey = "cdn/v1/" + truckModel + "/GLTF/" + truckModel + "_model.gltf";
                response.put("gltfKey", gltfKey);
                response.put("gltfUrl", storage.getPublicUrl(gltfKey));
                log.info("GLTF uploaded for truckModel={}, key={}", truckModel, gltfKey);
            }

            if (hasIcon) {
                storage.uploadTruckIcon(truckModel, icon);
                // Re-derive the key from what was just stored
                String iconKey = storage.getGltfKeys(truckModel).get("iconKey");
                response.put("iconKey", iconKey);
                response.put("iconUrl", iconKey != null ? storage.getPublicUrl(iconKey) : null);
                log.info("Truck icon uploaded for truckModel={}, key={}", truckModel, iconKey);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to upload GLTF/icon for truckModel={}", truckModel, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Upload failed: " + e.getMessage()));
        }
    }

    // ── GET /gltf/{truckModel}/info ───────────────────────────────────────────

    @GetMapping("/info")
    @Operation(summary = "Get GLTF and truck icon info", description = "Returns the S3 storage keys and public URLs for the GLTF model and truck icon "
            + "associated with the given truck model. Fields are null/empty if the file hasn't been uploaded.", responses = {
                    @ApiResponse(responseCode = "200", description = "Info returned")
            })
    public ResponseEntity<Map<String, Object>> gltfInfo(
            @Parameter(description = "Truck model identifier") @PathVariable String truckModel) {

        Map<String, String> keys = storage.getGltfKeys(truckModel);
        String gltfKey = keys.get("gltfKey");
        String iconKey = keys.get("iconKey");

        Map<String, Object> response = new HashMap<>();
        response.put("truckModel", truckModel);

        response.put("gltfKey", gltfKey != null ? gltfKey : "");
        response.put("gltfUrl", gltfKey != null ? storage.getPublicUrl(gltfKey) : "");
        response.put("gltfExists", gltfKey != null);

        response.put("iconKey", iconKey != null ? iconKey : "");
        response.put("iconUrl", iconKey != null ? storage.getPublicUrl(iconKey) : "");
        response.put("iconExists", iconKey != null);

        return ResponseEntity.ok(response);
    }
}
