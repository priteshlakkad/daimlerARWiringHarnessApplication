package com.harness.controller;

import com.harness.service.S3ServiceBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@Log4j2
@Tag(name = "Admin", description = "Administrative and diagnostic endpoints")
public class AdminController {

    private final S3ServiceBase storage;

    public AdminController(S3ServiceBase storage) {
        this.storage = storage;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get storage statistics",
            description = "Returns per-truck-model file counts and total file count across all truck models.")
    public ResponseEntity<Map<String, Object>> getStats() {
        List<String> allFiles = storage.listAllFiles();
        Map<String, Long> perTruck = allFiles.stream()
                .filter(key -> key.startsWith("cdn/v1/"))
                .collect(Collectors.groupingBy(
                        key -> {
                            String[] parts = key.split("/");
                            return parts.length > 2 ? parts[2] : "unknown";
                        },
                        Collectors.counting()));
        log.info("Admin stats requested: totalFiles={}", allFiles.size());
        return ResponseEntity.ok(Map.of(
                "totalFiles", allFiles.size(),
                "perTruckModel", perTruck));
    }
}
