package com.skse.inventory.controller;

import com.skse.inventory.model.Plan;
import com.skse.inventory.model.VendorAssignmentRequest;
import com.skse.inventory.model.VendorRole;
import com.skse.inventory.service.ArticleService;
import com.skse.inventory.service.PlanService;
import com.skse.inventory.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Create a new Plan", description = "Creates a new Plan with its details like article, size, color, and vendors assigned for each task.")
    @PostMapping
    public ResponseEntity<Plan> createPlan(@RequestBody Plan plan) {
        Plan createdPlan = planService.createPlan(plan);
        return ResponseEntity.ok(createdPlan);
    }

    @Operation(summary = "Assign vendor to a Plan", description = "Assigns cutting, printing, or stitching vendor to the plan.")
    @PostMapping("/{planNumber}/assign-vendor")
    public ResponseEntity<String> assignVendorsToPlan(
            @PathVariable String planNumber,
            @RequestBody VendorAssignmentRequest vendorAssignmentRequest) {
        planService.assignVendorToPlan(planNumber, vendorAssignmentRequest);
        return ResponseEntity.ok("Vendor assigned successfully");
    }

    @Operation(summary = "Update Plan details", description = "Updates the Plan details such as Total and Size-Quantity pairs.")
    @PutMapping("/{planNumber}/update")
    public ResponseEntity<Plan> updatePlan(@PathVariable String planNumber, @RequestBody Plan updatedPlan) {
        Plan plan = planService.updatePlan(planNumber, updatedPlan);
        return ResponseEntity.ok(plan);
    }

    @Operation(summary = "Delete a Plan", description = "Deletes the Plan and removes its associated data.")
    @DeleteMapping("/{planNumber}/delete")
    public ResponseEntity<String> deletePlan(@PathVariable String planNumber) {
        planService.deletePlan(planNumber);
        return ResponseEntity.ok("Plan deleted successfully");
    }

    @Operation(summary = "Move Plan to next state", description = "Moves the Plan to the next status, updating timestamps and stock accordingly.")
    @PostMapping("/{planNumber}/move-to-next-state")
    public ResponseEntity<Plan> moveToNextState(@PathVariable String planNumber) {
        Plan updatedPlan = planService.moveToNextState(planNumber);
        return ResponseEntity.ok(updatedPlan);
    }

    @Operation(summary = "Get active orders by state", description = "Returns a count of active orders for each state of the Plan.")
    @GetMapping("/active-orders-by-state")
    public ResponseEntity<Map<String, Integer>> getActiveOrdersByState() {
        return ResponseEntity.ok(planService.getActiveOrdersByState());
    }
}