package com.harness.controller;

import com.harness.service.HarnessService;
import com.harness.dtos.UploadHarnessRequest;
import com.harness.dtos.UploadHarnessResponse;
import com.harness.dtos.UploadedFileResponse;
import com.harness.dtos.GetHarnessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/truckModel")
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
                        @Parameter(description = "Truck model", example = "ACTROS") @PathVariable String truckModel,
                        @Parameter(description = "Harness ID", example = "HARN123") @PathVariable String harnessId,
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
                        @Parameter(description = "Truck model", example = "ACTROS") @PathVariable String truckModel,
                        @Parameter(description = "Harness ID", example = "HARN123") @PathVariable String harnessId,
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
                        @Parameter(description = "Harness ID to search for", example = "HARN123") @PathVariable String harnessId,
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
}
