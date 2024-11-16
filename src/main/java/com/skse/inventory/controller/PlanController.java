package com.skse.inventory.controller;

import com.skse.inventory.model.Plan;
import com.skse.inventory.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    @Autowired
    private PlanService planService;

    @PostMapping
    public ResponseEntity<Plan> createPlan(@RequestBody Plan plan) {
        Plan createdPlan = planService.createPlan(plan);
        return ResponseEntity.ok(createdPlan);
    }

    @PutMapping("/{id}/machine-process")
    public ResponseEntity<Plan> recordMachineProcessing(@PathVariable Long id, @RequestParam String processedPairs, @RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        Plan updatedPlan = planService.recordMachineProcessing(id, processedPairs, startDate, endDate);
        return ResponseEntity.ok(updatedPlan);
    }

    @PutMapping("/{id}/process-batch")
    public ResponseEntity<Plan> processBatchToFinishedStock(
            @PathVariable Long id,
            @RequestParam String processedPairs
    ) {
        Plan updatedPlan = planService.recordMachineBatch(id, processedPairs);
        return ResponseEntity.ok(updatedPlan);
    }
}