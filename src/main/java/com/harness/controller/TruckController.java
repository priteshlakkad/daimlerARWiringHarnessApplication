package com.harness.controller;

import com.harness.dtos.TruckRequest;
import com.harness.model.Truck;
import com.harness.service.TruckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/trucks")
@Tag(name = "Truck Management", description = "CRUD endpoints for managing truck records")
@RequiredArgsConstructor
@Log4j2
public class TruckController {

    private final TruckService truckService;

    @PostMapping
    @Operation(summary = "Create a truck", description = "Add a new truck record with VIN, truck name, and model")
    public ResponseEntity<?> create(@RequestBody TruckRequest request) {
        try {
            Truck truck = truckService.create(request);
            return ResponseEntity.ok(truck);
        } catch (Exception e) {
            log.error("Error creating truck: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all trucks", description = "Retrieve all truck records")
    public ResponseEntity<List<Truck>> getAll() {
        return ResponseEntity.ok(truckService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get truck by ID", description = "Retrieve a truck record by its database ID")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return truckService.getById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Truck not found with id: " + id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a truck", description = "Update an existing truck record by ID")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TruckRequest request) {
        try {
            return truckService.update(id, request)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(404).body(Map.of("error", "Truck not found with id: " + id)));
        } catch (Exception e) {
            log.error("Error updating truck {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a truck", description = "Delete a truck record by ID")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (truckService.delete(id)) {
            return ResponseEntity.ok(Map.of("message", "Truck deleted successfully"));
        }
        return ResponseEntity.status(404).body(Map.of("error", "Truck not found with id: " + id));
    }
}
