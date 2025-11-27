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

import java.time.LocalDate;
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
    public Vendor createVendor(@RequestBody Vendor vendor) {
        return vendorService.createVendor(vendor);
    }

    @Operation(summary = "Update an existing vendor",
            description = "Updates the details of a vendor using their ID.")
    @PutMapping("/{id}")
    public Vendor updateVendor(@PathVariable Long id, @RequestBody Vendor vendor) {
        return vendorService.updateVendor(id, vendor.getName(), vendor.getRole(), vendor.isActive());
    }

    @Operation(summary = "Get vendor by ID",
            description = "Retrieves a vendor by their ID.")
    @GetMapping("/{id}")
    public Vendor getVendorById(@PathVariable Long id) {
        return vendorService.getVendorById(id);
    }

    @Operation(summary = "Get all vendors",
            description = "Retrieves all vendors in the system.")
    @GetMapping
    public List<Vendor> getAllVendors() {
        return vendorService.getAllVendors();
    }

    @Operation(summary = "Get vendors by role",
            description = "Retrieves all vendors based on their role (Cutting, Printing, or Stitching).")
    @GetMapping("/by-role/{role}")
    public List<Vendor> getVendorsByRole(@Parameter(description = "Vendor Role", schema = @Schema(implementation = VendorRole.class)) 
                                        @PathVariable VendorRole role) {
        return vendorService.getVendorsByRole(role);
    }

    @Operation(summary = "Get vendor payment summary",
            description = "Provides a summary of all vendors and their payment information.")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getVendorPaymentSummary() {
        return ResponseEntity.ok(vendorService.getVendorPaymentSummary());
    }

    @Operation(summary = "Get vendor detailed summary",
            description = "Provides detailed summary for a specific vendor including order history.")
    @GetMapping("/{vendorId}/summary")
    public ResponseEntity<Map<String, Object>> getVendorDetailedSummary(@PathVariable Long vendorId) {
        Map<String, Object> summary = vendorService.getVendorDetailedSummary(vendorId);
        if (summary == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Record a payment for a vendor",
            description = "Updates the payment records for a vendor, reducing their pending dues.")
    @PostMapping("/{vendorId}/payment")
    public ResponseEntity<String> recordVendorPayment(@Parameter(description = "Vendor ID to record payment for")
                                                          @PathVariable Long vendorId,
                                                      @Parameter(description = "Amount to be paid")
                                                          @RequestParam double amount,
                                                      @Parameter(description = "Plan number reference")
                                                          @RequestParam(required = false) String planNumber) {
        vendorService.recordVendorPayment(vendorId, amount, planNumber != null ? planNumber : "");
        return ResponseEntity.ok("Payment recorded successfully");
    }

    @Operation(summary = "Get vendor payment report",
            description = "Retrieves payment report for vendors within a date range.")
    @GetMapping("/report")
    public ResponseEntity<List<Map<String, Object>>> getVendorPaymentReport(
            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam(required = false) String endDate) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        List<Map<String, Object>> report = vendorService.getVendorPaymentReport(start, end);
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Delete a vendor",
            description = "Deletes a vendor from the system.")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteVendor(@PathVariable Long id) {
        vendorService.deleteVendor(id);
        return ResponseEntity.ok("Vendor deleted successfully");
    }
}
