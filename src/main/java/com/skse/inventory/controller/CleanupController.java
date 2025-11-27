package com.skse.inventory.controller;

import com.skse.inventory.service.CleanupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cleanup")
@Tag(name = "Cleanup", description = "Record cleanup operations")
public class CleanupController {
    
    @Autowired
    private CleanupService cleanupService;
    
    @Operation(summary = "Delete records from previous month",
            description = "Deletes completed plans and settled payment records from the previous month")
    @PostMapping("/previous-month")
    public ResponseEntity<Map<String, Integer>> deletePreviousMonthRecords() {
        Map<String, Integer> result = cleanupService.deleteRecordsFromPreviousMonth();
        return ResponseEntity.ok(result);
    }
}

