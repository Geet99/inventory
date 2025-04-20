package com.skse.inventory.controller;

import com.skse.inventory.model.Vendor;
import com.skse.inventory.model.VendorRole;
import com.skse.inventory.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendors")
@Tag(name = "Vendors", description = "APIs to manage Vendors (Cutting, Printing, Stitching)")
public class VendorController {

    @Autowired
    private VendorService vendorService;

    @Operation(summary = "Add a new vendor",
            description = "Creates a new vendor entry for Cutting, Printing, or Stitching tasks.")
    @PostMapping
    public Vendor addVendor(@RequestBody Vendor vendor) {
        return vendorService.addVendor(vendor);
    }

    @Operation(summary = "Update an existing vendor",
            description = "Updates the details of a vendor using their ID.")
    @PutMapping("/{id}")
    public Vendor updateVendor(@PathVariable Long id, @RequestBody Vendor vendor) {
        return vendorService.updateVendor(id, vendor);
    }

    @Operation(summary = "Get vendors by role",
            description = "Retrieves all vendors based on their role (Cutting, Printing, or Stitching).")
    @GetMapping()
    public List<Vendor> getVendorsByRole(@Parameter(description = "Vendor Role", schema = @Schema(implementation = VendorRole.class)) @RequestParam VendorRole role) {
        return vendorService.getVendorsByRole(role);
    }

    @Operation(summary = "Get vendor summary",
            description = "Provides a summary of all active vendors and their assigned orders for the current month.")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getVendorSummary() {
        return ResponseEntity.ok(vendorService.getVendorSummary());
    }

    @Operation(summary = "Get total pending payments",
            description = "Returns a list of vendors and the total amount due for payment.")
    @GetMapping("/vendors/payments-due")
    public ResponseEntity<Map<String, Double>> getVendorPaymentsDue() {
        return ResponseEntity.ok(vendorService.getVendorPaymentsDue());
    }

    @Operation(summary = "Record a payment for a vendor",
            description = "Updates the payment records for a vendor, reducing their pending dues.")
    @PostMapping("/{vendorId}/payment")
    public ResponseEntity<String> recordVendorPayment(@Parameter(description = "Vendor ID to record payment for")
                                                          @PathVariable Long vendorId,
                                                      @Parameter(description = "Amount to be paid")
                                                          @RequestParam double payment) {
        return ResponseEntity.ok(vendorService.recordVendorPayment(vendorId, payment));
    }
}
