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

    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    public Vendor getVendorById(Long id) {
        return vendorRepository.findById(id).orElse(null);
    }

    public Vendor createVendor(Vendor vendor) {
        return vendorRepository.save(vendor);
    }

    public Vendor updateVendor(Long id, Vendor vendor) {
        Vendor existingVendor = getVendorById(id);
        if (existingVendor != null) {
            vendor.setId(id);
            return vendorRepository.save(vendor);
        }
        return null;
    }

    public void deleteVendor(Long id) {
        vendorRepository.deleteById(id);
    }

    public List<Vendor> getVendorsByRole(VendorRole role) {
        return vendorRepository.findByRole(role);
    }

    public List<Vendor> getActiveVendors() {
        return vendorRepository.findByIsActiveTrue();
    }

    public void updateVendorPaymentDue(Long vendorId, double amount) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor != null) {
            vendor.setPaymentDue(vendor.getPaymentDue() + amount);
            vendorRepository.save(vendor);
        }
    }

    public void recordVendorPayment(Long vendorId, double amount, String planNumber) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor != null) {
            // Reduce payment due
            vendor.setPaymentDue(vendor.getPaymentDue() - amount);
            vendorRepository.save(vendor);
            
            // Record payment history
            VendorOrderHistory paymentRecord = new VendorOrderHistory();
            paymentRecord.setVendor(vendor);
            paymentRecord.setPlanNumber(planNumber);
            paymentRecord.setAmount(amount);
            paymentRecord.setPaymentDate(LocalDate.now());
            paymentRecord.setType("PAYMENT");
            vendorOrderHistoryRepository.save(paymentRecord);
        }
    }

    public void recordVendorOrder(Long vendorId, String planNumber, double amount, VendorRole role) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor != null) {
            // Add to payment due
            vendor.setPaymentDue(vendor.getPaymentDue() + amount);
            vendorRepository.save(vendor);
            
            // Record order history
            VendorOrderHistory orderRecord = new VendorOrderHistory();
            orderRecord.setVendor(vendor);
            orderRecord.setPlanNumber(planNumber);
            orderRecord.setAmount(amount);
            orderRecord.setOrderDate(LocalDate.now());
            orderRecord.setType("ORDER");
            orderRecord.setRole(role);
            vendorOrderHistoryRepository.save(orderRecord);
        }
    }

    public List<VendorOrderHistory> getVendorOrderHistory(Long vendorId) {
        return vendorOrderHistoryRepository.findByVendorIdOrderByOrderDateDesc(vendorId);
    }

    public Map<String, Object> getVendorPaymentSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // Total payment due across all vendors
        double totalPaymentDue = vendorRepository.findAll().stream()
            .mapToDouble(Vendor::getPaymentDue)
            .sum();
        summary.put("totalPaymentDue", totalPaymentDue);
        
        // Payment due by vendor role
        Map<VendorRole, Double> paymentByRole = new HashMap<>();
        for (VendorRole role : VendorRole.values()) {
            double rolePayment = vendorRepository.findByRole(role).stream()
                .mapToDouble(Vendor::getPaymentDue)
                .sum();
            paymentByRole.put(role, rolePayment);
        }
        summary.put("paymentByRole", paymentByRole);
        
        // Top vendors by payment due
        List<Vendor> topVendors = vendorRepository.findAll().stream()
            .filter(v -> v.getPaymentDue() > 0)
            .sorted((v1, v2) -> Double.compare(v2.getPaymentDue(), v1.getPaymentDue()))
            .limit(10)
            .collect(Collectors.toList());
        summary.put("topVendors", topVendors);
        
        return summary;
    }

    public Map<String, Object> getVendorDetailedSummary(Long vendorId) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor == null) {
            return null;
        }
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("vendor", vendor);
        
        // Recent order history
        List<VendorOrderHistory> recentHistory = getVendorOrderHistory(vendorId);
        summary.put("recentHistory", recentHistory);
        
        // Total orders and payments this month
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        List<VendorOrderHistory> monthlyHistory = vendorOrderHistoryRepository
            .findByVendorIdAndOrderDateGreaterThanEqual(vendorId, startOfMonth);
        
        double monthlyOrders = monthlyHistory.stream()
            .filter(h -> "ORDER".equals(h.getType()))
            .mapToDouble(VendorOrderHistory::getAmount)
            .sum();
        
        double monthlyPayments = monthlyHistory.stream()
            .filter(h -> "PAYMENT".equals(h.getType()))
            .mapToDouble(VendorOrderHistory::getAmount)
            .sum();
        
        summary.put("monthlyOrders", monthlyOrders);
        summary.put("monthlyPayments", monthlyPayments);
        
        return summary;
    }

    public List<Map<String, Object>> getVendorPaymentReport(LocalDate startDate, LocalDate endDate) {
        List<VendorOrderHistory> history = vendorOrderHistoryRepository
            .findByOrderDateBetween(startDate, endDate);
        
        return history.stream()
            .map(h -> {
                Map<String, Object> record = new HashMap<>();
                record.put("vendorName", h.getVendor().getName());
                record.put("vendorRole", h.getVendor().getRole());
                record.put("planNumber", h.getPlanNumber());
                record.put("amount", h.getAmount());
                record.put("type", h.getType());
                record.put("date", h.getOrderDate() != null ? h.getOrderDate() : h.getPaymentDate());
                return record;
            })
            .collect(Collectors.toList());
    }
}
