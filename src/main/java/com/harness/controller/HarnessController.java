package com.harness.controller;

import com.harness.dtos.GetHarnessResponse;
import com.harness.dtos.HarnessValidationResult;
import com.harness.dtos.TruckFilesResponse;
import com.harness.dtos.UploadHarnessRequest;
import com.harness.dtos.UploadHarnessResponse;
import com.harness.dtos.UploadedFileResponse;
import com.harness.service.HarnessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * Harness File Management API Controller
 *
 * Base path: /truckModel
 *
 * Endpoints:
 * POST /truckModel/{truckModel}/harnesses/{harnessId} — Upload harness files
 * GET /truckModel/{truckModel}/harnesses/{harnessId} — Get all files for a
 * specific harness under a truck model
 * GET /truckModel/{truckModel}/harnesses/downloadable-files — (removed,
 * truckModel no longer needed)
 * GET /truckModel/downloadable-files — Get all troubleshooting + powerflow
 * files across ALL truck models
 * GET /truckModel/harnesses/{harnessId} — Search harness ID across ALL truck
 * models, return first match
 * GET /truckModel/all-files — Get every file across ALL truck model folders
 *
 * The underlying storage backend is determined by the 'app.use-s3' property:
 * - app.use-s3=true → Files stored in AWS S3 bucket
 * - app.use-s3=false → Files stored locally on the filesystem (default)
 */
@RestController
@RequestMapping("/api/v1/truckModel")
@Log4j2
@Tag(name = "Harness Management", description = "Endpoints for managing harness files (PDFs, Videos, etc.) across truck models")
public class HarnessController {

        @Autowired
        HarnessService harnessService;

        // ─── POST /truckModel/{truckModel}/harnesses/{harnessId} ───────────────────

        @PostMapping(value = "/{truckModel}/harnesses/{harnessId}", consumes = "multipart/form-data")
        @Operation(summary = "Upload harness files", description = "Uploads PDFs, videos, JSON and bundle files for a harness under a specific truck model", responses = {
                        @ApiResponse(responseCode = "200", description = "Files uploaded successfully"),
                        @ApiResponse(responseCode = "500", description = "Upload failed")
        })
        public ResponseEntity<UploadHarnessResponse> upload(
                        @Parameter(name = "truckModel", description = "Truck model", example = "ACTROS") @PathVariable("truckModel") String truckModel,
                        @Parameter(name = "harnessId", description = "Harness ID", example = "HARN123") @PathVariable("harnessId") String harnessId,
                        @RequestBody(content = @Content(mediaType = "multipart/form-data", schema = @Schema(implementation = UploadHarnessRequest.class)))
                        @ModelAttribute UploadHarnessRequest request) {
                try {
                        List<UploadedFileResponse> uploadedFiles = harnessService.uploadFiles(truckModel, harnessId,
                                        request);
                        List<String> uploadedKeys = uploadedFiles.stream().map(UploadedFileResponse::getKey)
                                        .collect(java.util.stream.Collectors.toList());
                        return ResponseEntity.ok(
                                        new UploadHarnessResponse(truckModel, harnessId, uploadedKeys, uploadedFiles));
                } catch (Exception e) {
                        return ResponseEntity.internalServerError()
                                        .body(new UploadHarnessResponse(truckModel, harnessId, List.of(), List.of()));
                }
        }

        // ─── GET /truckModel/{truckModel}/harnesses/{harnessId} ────────────────────

        @GetMapping("/{truckModel}/harnesses/{harnessId}")
        @Operation(summary = "Get harness files by truck model and harness ID", description = "Returns all uploaded files for a specific harness under a specific truck model, with presigned/local URLs")
        public ResponseEntity<GetHarnessResponse> getHarness(
                        @Parameter(name = "truckModel", description = "Truck model", example = "ACTROS") @PathVariable("truckModel") String truckModel,
                        @Parameter(name = "harnessId", description = "Harness ID", example = "HARN123") @PathVariable("harnessId") String harnessId,
                        @RequestParam(defaultValue = "900") int ttlSeconds) {
                List<UploadedFileResponse> files = harnessService.getHarnessFiles(truckModel, harnessId, ttlSeconds);
                return ResponseEntity.ok(new GetHarnessResponse(truckModel, harnessId, files));
        }

        // ─── GET /truckModel/downloadable-files ────────────────────────────────────
        // NOTE: kept out of /harnesses/* to avoid conflict with /harnesses/{harnessId}

        @GetMapping("/downloadable-files")
        @Operation(summary = "Get all downloadable files across every truck model", description = "Returns all troubleshooting and powerflow files across ALL truck model folders — no truck ID required")
        public ResponseEntity<GetHarnessResponse> getAllDownloadableFiles(
                        @RequestParam(defaultValue = "900") int ttlSeconds) {
                List<UploadedFileResponse> files = harnessService.getAllDownloadableFiles(ttlSeconds);
                return ResponseEntity.ok(new GetHarnessResponse(null, "all", files));
        }

        // ─── GET /truckModel/harnesses/{harnessId} ─────────────────────────────────

        @GetMapping("/harnesses/{harnessId}")
        @Operation(summary = "Find harness files by harness ID (search across all truck models)", description = "Searches for the given harness ID across ALL truck model folders and returns files from the first match found")
        public ResponseEntity<GetHarnessResponse> findHarnessById(
                        @Parameter(name = "harnessId", description = "Harness ID to search for", example = "HARN123") @PathVariable("harnessId") String harnessId,
                        @RequestParam(defaultValue = "900") int ttlSeconds) {
                List<UploadedFileResponse> files = harnessService.findHarnessById(harnessId, ttlSeconds);
                return ResponseEntity.ok(new GetHarnessResponse(null, harnessId, files));
        }

        // ─── GET /truckModel/all-files ──────────────────────────────────────────
        // NOTE: kept out of /harnesses/* to avoid conflict with /harnesses/{harnessId}

        @GetMapping("/all-files")
        @Operation(summary = "Get all files across every truck model folder", description = "Returns every file stored under all truck model folders in CDN without requiring a truck ID")
        public ResponseEntity<GetHarnessResponse> getAllFiles(
                        @RequestParam(defaultValue = "900") int ttlSeconds) {
                List<UploadedFileResponse> files = harnessService.getAllFiles(ttlSeconds);
                return ResponseEntity.ok(new GetHarnessResponse(null, "all", files));
        }

        // ─── GET /truckModel/{truckModel}/files ─────────────────────────────────────

        @GetMapping("/{truckModel}/files")
        @Operation(summary = "Get full file tree for a truck model", description = "Returns all folders and files under the given truck model as a nested JSON structure. File URLs are presigned/public links. Returns 404 if the truck model has no files.", responses = {
                        @ApiResponse(responseCode = "200", description = "File tree returned successfully"),
                        @ApiResponse(responseCode = "404", description = "Truck not found")
        })
        public ResponseEntity<?> getTruckFiles(
                        @Parameter(name = "truckModel", description = "Truck model ID", example = "ACTROS") @PathVariable("truckModel") String truckModel,
                        @RequestParam(defaultValue = "900") int ttlSeconds) {
                TruckFilesResponse response = harnessService.getTruckFiles(truckModel, ttlSeconds);
                if (response == null) {
                        return ResponseEntity.status(404).body(Map.of("error", "Truck not found: " + truckModel));
                }
                return ResponseEntity.ok(response);
        }

        // ─── GET /truckModel ─────────────────────────────────────────────────────────

        @GetMapping
        @Operation(summary = "List all truck models", description = "Returns a distinct list of all truck model names that have files in storage.")
        public ResponseEntity<Map<String, Object>> listTruckModels() {
                List<String> models = harnessService.listTruckModels();
                return ResponseEntity.ok(Map.of("truckModels", models, "count", models.size()));
        }

        // ─── GET /truckModel/{truckModel}/harnesses ──────────────────────────────────

        @GetMapping("/{truckModel}/harnesses")
        @Operation(summary = "List all harness IDs for a truck model", description = "Returns all harness folder IDs under cdn/v1/{truckModel}/harnesses/.")
        public ResponseEntity<Map<String, Object>> listHarnessIds(
                        @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel) {
                List<String> ids = harnessService.listHarnessIds(truckModel);
                return ResponseEntity.ok(Map.of("truckModel", truckModel, "harnessIds", ids, "count", ids.size()));
        }

        // ─── DELETE /truckModel/{truckModel} ─────────────────────────────────────────

        @DeleteMapping("/{truckModel}")
        @Operation(summary = "Delete an entire truck model", description = "Deletes all files under cdn/v1/{truckModel}/ including harnesses, GLTF, enddevices, and faultcodes.", responses = {
                        @ApiResponse(responseCode = "200", description = "Truck model deleted"),
                        @ApiResponse(responseCode = "500", description = "Deletion failed")
        })
        public ResponseEntity<Map<String, Object>> deleteTruck(
                        @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel) {
                try {
                        harnessService.deleteTruck(truckModel);
                        log.info("Truck deleted: truckModel={}", truckModel);
                        return ResponseEntity.ok(Map.of("success", true, "message", "Truck model deleted: " + truckModel));
                } catch (Exception e) {
                        log.error("Failed to delete truck: truckModel={}", truckModel, e);
                        return ResponseEntity.internalServerError()
                                        .body(Map.of("success", false, "message", "Deletion failed: " + e.getMessage()));
                }
        }

        // ─── DELETE /truckModel/{truckModel}/harnesses/{harnessId} ───────────────────

        @DeleteMapping("/{truckModel}/harnesses/{harnessId}")
        @Operation(summary = "Delete an entire harness", description = "Deletes all files under cdn/v1/{truckModel}/harnesses/{harnessId}/.", responses = {
                        @ApiResponse(responseCode = "200", description = "Harness deleted"),
                        @ApiResponse(responseCode = "500", description = "Deletion failed")
        })
        public ResponseEntity<Map<String, Object>> deleteHarness(
                        @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
                        @Parameter(name = "harnessId", description = "Harness ID") @PathVariable("harnessId") String harnessId) {
                try {
                        harnessService.deleteHarness(truckModel, harnessId);
                        log.info("Harness deleted: truckModel={}, harnessId={}", truckModel, harnessId);
                        return ResponseEntity.ok(Map.of("success", true, "message", "Harness deleted: " + harnessId));
                } catch (Exception e) {
                        log.error("Failed to delete harness: truckModel={}, harnessId={}", truckModel, harnessId, e);
                        return ResponseEntity.internalServerError()
                                        .body(Map.of("success", false, "message", "Deletion failed: " + e.getMessage()));
                }
        }

        // ─── DELETE /truckModel/{truckModel}/harnesses/{harnessId}/{category}/{fileName}

        @DeleteMapping("/{truckModel}/harnesses/{harnessId}/{category}/{fileName}")
        @Operation(summary = "Delete a single harness file", description = "Deletes cdn/v1/{truckModel}/harnesses/{harnessId}/{category}/{fileName}.", responses = {
                        @ApiResponse(responseCode = "200", description = "File deleted"),
                        @ApiResponse(responseCode = "404", description = "File not found"),
                        @ApiResponse(responseCode = "500", description = "Deletion failed")
        })
        public ResponseEntity<Map<String, Object>> deleteHarnessFile(
                        @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
                        @Parameter(name = "harnessId", description = "Harness ID") @PathVariable("harnessId") String harnessId,
                        @Parameter(name = "category", description = "File category (e.g. info, wiring)") @PathVariable("category") String category,
                        @Parameter(name = "fileName", description = "File name") @PathVariable("fileName") String fileName) {
                try {
                        harnessService.deleteHarnessFile(truckModel, harnessId, category, fileName);
                        log.info("Harness file deleted: {}/{}/{}/{}", truckModel, harnessId, category, fileName);
                        return ResponseEntity.ok(Map.of("success", true, "message", "File deleted: " + fileName));
                } catch (java.io.IOException e) {
                        log.warn("Harness file not found: {}", fileName);
                        return ResponseEntity.status(404)
                                        .body(Map.of("success", false, "message", e.getMessage()));
                } catch (Exception e) {
                        log.error("Failed to delete harness file: {}", fileName, e);
                        return ResponseEntity.internalServerError()
                                        .body(Map.of("success", false, "message", "Deletion failed: " + e.getMessage()));
                }
        }

        // ─── PUT /truckModel/{truckModel}/harnesses/{harnessId} ──────────────────────

        @PutMapping(value = "/{truckModel}/harnesses/{harnessId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Replace a single harness file", description = "Replaces one file in a harness. The category param determines the sub-folder and naming convention. "
                        + "Accepted categories: info, troubleshooting, wiring, powerflow, repair.", responses = {
                                        @ApiResponse(responseCode = "200", description = "File replaced"),
                                        @ApiResponse(responseCode = "500", description = "Upload failed")
                        })
        public ResponseEntity<Map<String, Object>> replaceHarnessFile(
                        @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
                        @Parameter(name = "harnessId", description = "Harness ID") @PathVariable("harnessId") String harnessId,
                        @RequestParam("category") String category,
                        @RequestParam("file") MultipartFile file) {
                try {
                        UploadedFileResponse result = harnessService.replaceHarnessFile(truckModel, harnessId, category, file);
                        log.info("Harness file replaced: truckModel={}, harnessId={}, category={}", truckModel, harnessId, category);
                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "truckModel", truckModel,
                                        "harnessId", harnessId,
                                        "key", result.getKey(),
                                        "url", result.getUrl()));
                } catch (Exception e) {
                        log.error("Failed to replace harness file: truckModel={}, harnessId={}", truckModel, harnessId, e);
                        return ResponseEntity.internalServerError()
                                        .body(Map.of("success", false, "message", "Upload failed: " + e.getMessage()));
                }
        }

        // ─── POST /truckModel/{truckModel}/harnesses/{harnessId}/3d-model ────────────

        @PostMapping(value = "/{truckModel}/harnesses/{harnessId}/3d-model", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Upload 3D model for a harness", description = "Uploads a 3D model file (e.g. .glb, .gltf, .fbx) for a harness. "
                        + "Stored at cdn/v1/{truckModel}/harnesses/{harnessId}/{harnessId}.{ext}.", responses = {
                                        @ApiResponse(responseCode = "200", description = "3D model uploaded successfully"),
                                        @ApiResponse(responseCode = "400", description = "No file provided"),
                                        @ApiResponse(responseCode = "500", description = "Upload failed")
                        })
        public ResponseEntity<Map<String, Object>> upload3dModel(
                        @Parameter(name = "truckModel", description = "Truck model", example = "ACTROS") @PathVariable("truckModel") String truckModel,
                        @Parameter(name = "harnessId", description = "Harness ID", example = "HARN123") @PathVariable("harnessId") String harnessId,
                        @RequestParam("file") MultipartFile file) {
                if (file == null || file.isEmpty()) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("success", false, "message", "No file provided"));
                }
                try {
                        UploadedFileResponse result = harnessService.upload3dModel(truckModel, harnessId, file);
                        log.info("3D model uploaded: truckModel={}, harnessId={}, key={}", truckModel, harnessId, result.getKey());
                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "truckModel", truckModel,
                                        "harnessId", harnessId,
                                        "key", result.getKey(),
                                        "url", result.getUrl()));
                } catch (Exception e) {
                        log.error("Failed to upload 3D model: truckModel={}, harnessId={}", truckModel, harnessId, e);
                        return ResponseEntity.internalServerError()
                                        .body(Map.of("success", false, "message", "Upload failed: " + e.getMessage()));
                }
        }

        // ─── POST /truckModel/{truckModel}/harnesses/{harnessId}/validate ────────────

        @PostMapping("/{truckModel}/harnesses/{harnessId}/validate")
        @Operation(summary = "Validate harness has required files", description = "Checks that the harness contains all three required files: info PDF, troubleshooting PDF, and wiring PDF.")
        public ResponseEntity<HarnessValidationResult> validateHarness(
                        @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
                        @Parameter(name = "harnessId", description = "Harness ID") @PathVariable("harnessId") String harnessId) {
                HarnessValidationResult result = harnessService.validateHarness(truckModel, harnessId);
                return ResponseEntity.ok(result);
        }
}
