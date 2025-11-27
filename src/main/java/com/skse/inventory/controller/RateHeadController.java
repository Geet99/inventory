package com.skse.inventory.controller;

import com.skse.inventory.model.RateHead;
import com.skse.inventory.model.VendorRole;
import com.skse.inventory.service.RateHeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rateheads")
@Tag(name = "Rate Heads", description = "APIs to manage Rate Heads for operations")
public class RateHeadController {
    
    @Autowired
    private RateHeadService rateHeadService;
    
    @Operation(summary = "Create a new rate head", description = "Creates a new rate head for Cutting, Printing, or Stitching operations.")
    @PostMapping
    public ResponseEntity<RateHead> createRateHead(@RequestBody RateHead rateHead) {
        RateHead created = rateHeadService.createRateHead(rateHead);
        return ResponseEntity.ok(created);
    }
    
    @Operation(summary = "Get all rate heads", description = "Retrieves all rate heads in the system.")
    @GetMapping
    public ResponseEntity<List<RateHead>> getAllRateHeads() {
        return ResponseEntity.ok(rateHeadService.getAllRateHeads());
    }
    
    @Operation(summary = "Get rate head by ID", description = "Retrieves a specific rate head by ID.")
    @GetMapping("/{id}")
    public ResponseEntity<RateHead> getRateHeadById(@PathVariable Long id) {
        RateHead rateHead = rateHeadService.getRateHeadById(id);
        if (rateHead == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rateHead);
    }
    
    @Operation(summary = "Get rate heads by operation type", description = "Retrieves rate heads for a specific operation type.")
    @GetMapping("/operation/{operationType}")
    public ResponseEntity<List<RateHead>> getRateHeadsByOperationType(@PathVariable VendorRole operationType) {
        return ResponseEntity.ok(rateHeadService.getRateHeadsByOperationType(operationType));
    }
    
    @Operation(summary = "Get active rate heads by operation type", description = "Retrieves only active rate heads for a specific operation type.")
    @GetMapping("/operation/{operationType}/active")
    public ResponseEntity<List<RateHead>> getActiveRateHeadsByOperationType(@PathVariable VendorRole operationType) {
        return ResponseEntity.ok(rateHeadService.getActiveRateHeadsByOperationType(operationType));
    }
    
    @Operation(summary = "Update rate head", description = "Updates an existing rate head.")
    @PutMapping("/{id}")
    public ResponseEntity<RateHead> updateRateHead(@PathVariable Long id, @RequestBody RateHead rateHead) {
        RateHead updated = rateHeadService.updateRateHead(id, rateHead);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }
    
    @Operation(summary = "Delete rate head", description = "Deletes a rate head.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRateHead(@PathVariable Long id) {
        rateHeadService.deleteRateHead(id);
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "Deactivate rate head", description = "Marks a rate head as inactive instead of deleting.")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<RateHead> deactivateRateHead(@PathVariable Long id) {
        RateHead deactivated = rateHeadService.deactivateRateHead(id);
        if (deactivated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(deactivated);
    }
}

