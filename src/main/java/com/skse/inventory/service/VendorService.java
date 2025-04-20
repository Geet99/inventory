package com.skse.inventory.service;

import com.skse.inventory.model.Vendor;
import com.skse.inventory.model.VendorRole;
import com.skse.inventory.model.VendorOrderHistory;
import com.skse.inventory.repository.PlanRepository;
import com.skse.inventory.repository.VendorOrderHistoryRepository;
import com.skse.inventory.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorOrderHistoryRepository vendorOrderHistoryRepository;

    @Autowired
    private PlanRepository planRepository;

    // Add a new vendor
    public Vendor addVendor(Vendor vendor) {
        return vendorRepository.save(vendor);
    }

    // Update an existing vendor
    public Vendor updateVendor(Long id, Vendor updatedVendor) {
        return vendorRepository.findById(id).map(existingVendor -> {
            existingVendor.setName(updatedVendor.getName());
            existingVendor.setRole(updatedVendor.getRole());
            existingVendor.setContactInfo(updatedVendor.getContactInfo());
            existingVendor.setAddress(updatedVendor.getAddress());
            existingVendor.setActive(updatedVendor.isActive());
            existingVendor.setPaymentDue(updatedVendor.getPaymentDue());
            return vendorRepository.save(existingVendor);
        }).orElseThrow(() -> new RuntimeException("Vendor not found with ID: " + id));
    }

    // Get vendors by role
    public List<Vendor> getVendorsByRole(VendorRole role) {
        return vendorRepository.findByRole(role);
    }

    // Get Vendor Summary (Including Monthly Order History)
    public Map<String, Object> getVendorSummary() {
        List<Vendor> vendors = vendorRepository.findAll();
        Map<String, Object> summary = new HashMap<>();

        for (Vendor vendor : vendors) {
            List<VendorOrderHistory> recentOrders = vendor.getOrderHistory().stream()
                    .filter(order -> order.getOrderDate().isAfter(LocalDate.now().minusMonths(1)))
                    .toList();

            summary.put(vendor.getName(), Map.of(
                    "role", vendor.getRole(),
                    "activeOrders", recentOrders.size(),
                    "paymentDue", vendor.getPaymentDue()
            ));
        }

        return summary;
    }

    // Delete Vendor Orders Older Than One Month
    public void cleanupOldVendorOrders() {
        List<VendorOrderHistory> oldOrders = vendorOrderHistoryRepository.findByOrderDateBefore(LocalDate.now().minusMonths(1));
        vendorOrderHistoryRepository.deleteAll(oldOrders);
    }

    // Get Vendor Payments Due
    public Map<String, Double> getVendorPaymentsDue() {
        return vendorRepository.findAll().stream()
                .collect(Collectors.toMap(Vendor::getName, Vendor::getPaymentDue));
    }

    // Record Vendor Payment
    public String recordVendorPayment(Long vendorId, double payment) {
        Optional<Vendor> vendorOpt = vendorRepository.findById(vendorId);

        if (vendorOpt.isPresent()) {
            Vendor vendor = vendorOpt.get();
            double newDue = Math.max(0, vendor.getPaymentDue() - payment);
            vendor.setPaymentDue(newDue);
            vendorRepository.save(vendor);
            return "Payment of " + payment + " recorded for Vendor: " + vendor.getName() + ". New due amount: " + newDue;
        }
        return "Vendor not found!";
    }

    public void incrementVendorPaymentDue(Long vendorId, double payment) {
        Optional<Vendor> vendorOptional = vendorRepository.findById(vendorId);
        if (vendorOptional.isEmpty()) {
            throw new IllegalArgumentException("Vendor not found: " + vendorId);
        }
        Vendor vendor = vendorOptional.get();
        vendor.setPaymentDue(vendor.getPaymentDue() + payment);
        vendorRepository.save(vendor);
    }

    public Optional<Vendor> getVendorById(Long id) {
        return vendorRepository.findById(id);
    }

    public Object getAllVendors() {
        return vendorRepository.findAll();
    }
}
