package com.skse.inventory.controller;

import com.skse.inventory.model.Plan;
import com.skse.inventory.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

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
            @PathVariable String planNumber,
            @RequestParam String processedPairs
    ) {
        Plan updatedPlan = planService.recordMachineProcessing(planNumber, processedPairs, LocalDate.now(), LocalDate.now());
        return ResponseEntity.ok(updatedPlan);
    }

    @GetMapping("/active-orders-by-state")
    public ResponseEntity<Map<String, Integer>> getActiveOrdersByState() {
        return ResponseEntity.ok(planService.getActiveOrdersByState());
    }
}