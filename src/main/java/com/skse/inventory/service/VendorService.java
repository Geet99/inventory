package com.skse.inventory.service;

import com.skse.inventory.model.*;
import com.skse.inventory.repository.PlanRepository;
import com.skse.inventory.repository.VendorMonthlyPaymentRepository;
import com.skse.inventory.repository.VendorOrderHistoryRepository;
import com.skse.inventory.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.YearMonth;
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
    
    @Autowired
    private VendorMonthlyPaymentRepository vendorMonthlyPaymentRepository;

    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    public Vendor getVendorById(Long id) {
        return vendorRepository.findById(id).orElse(null);
    }

    public Vendor createVendor(Vendor vendor) {
        return vendorRepository.save(vendor);
    }

    public Vendor updateVendor(Long id, String name, VendorRole role, boolean active) {
        Vendor existingVendor = getVendorById(id);
        if (existingVendor != null) {
            // Update only the editable fields, preserve orderHistory and paymentDue
            existingVendor.setName(name);
            existingVendor.setRole(role);
            existingVendor.setActive(active);
            // Don't update orderHistory, paymentDue - these are managed by the system
            return vendorRepository.save(existingVendor);
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
                record.put("role", h.getVendor().getRole().toString());
                record.put("planNumber", h.getPlanNumber());
                record.put("orderDate", h.getOrderDate());
                record.put("paymentDate", h.getPaymentDate());
                record.put("amount", h.getAmount());
                record.put("pendingAmount", h.getOrderDate() != null && h.getPaymentDate() == null ? h.getAmount() : 0.0);
                return record;
            })
            .collect(Collectors.toList());
    }
    
    // ========== MONTHLY PAYMENT TRACKING METHODS ==========
    
    /**
     * Records vendor order payment to specific month based on completion date
     * This is called when work is completed (e.g., cutting finished on 2nd Feb -> Feb month)
     */
    public void recordVendorOrderToMonth(Long vendorId, String planNumber, double amount, 
                                         VendorRole role, LocalDate completionDate) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor != null) {
            // Determine month-year based on completion date
            String monthYear = VendorMonthlyPayment.getMonthYearString(completionDate);
            
            // Get or create monthly payment record
            VendorMonthlyPayment monthlyPayment = vendorMonthlyPaymentRepository
                .findByVendorAndMonthYearAndOperationType(vendor, monthYear, role)
                .orElse(new VendorMonthlyPayment());
            
            if (monthlyPayment.getId() == null) {
                // New monthly record
                monthlyPayment.setVendor(vendor);
                monthlyPayment.setMonthYear(monthYear);
                monthlyPayment.setOperationType(role);
                monthlyPayment.setTotalDue(0);
                monthlyPayment.setPaidAmount(0);
                monthlyPayment.setStatus(PaymentStatus.PENDING);
                monthlyPayment.setCreatedDate(LocalDate.now());
            }
            
            // Add amount to total due for this month
            monthlyPayment.setTotalDue(monthlyPayment.getTotalDue() + amount);
            monthlyPayment.setLastUpdatedDate(LocalDate.now());
            
            // Update status
            if (monthlyPayment.getPaidAmount() >= monthlyPayment.getTotalDue()) {
                monthlyPayment.setStatus(PaymentStatus.PAID);
            } else if (monthlyPayment.getPaidAmount() > 0) {
                monthlyPayment.setStatus(PaymentStatus.PARTIAL);
            } else {
                monthlyPayment.setStatus(PaymentStatus.PENDING);
            }
            
            vendorMonthlyPaymentRepository.save(monthlyPayment);
            
            // Also update legacy total (for backward compatibility)
            vendor.setPaymentDue(vendor.getPaymentDue() + amount);
            vendorRepository.save(vendor);
            
            // Record order history
            VendorOrderHistory orderRecord = new VendorOrderHistory();
            orderRecord.setVendor(vendor);
            orderRecord.setPlanNumber(planNumber);
            orderRecord.setAmount(amount);
            orderRecord.setOrderDate(completionDate);
            orderRecord.setType("ORDER");
            orderRecord.setRole(role);
            vendorOrderHistoryRepository.save(orderRecord);
        }
    }
    
    /**
     * Record payment for a specific month
     */
    public void recordMonthlyPayment(Long vendorId, String monthYear, VendorRole operationType, 
                                     double amount, String planNumber) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor != null) {
            VendorMonthlyPayment monthlyPayment = vendorMonthlyPaymentRepository
                .findByVendorAndMonthYearAndOperationType(vendor, monthYear, operationType)
                .orElse(null);
            
            if (monthlyPayment != null) {
                // Add to paid amount
                monthlyPayment.setPaidAmount(monthlyPayment.getPaidAmount() + amount);
                monthlyPayment.setLastUpdatedDate(LocalDate.now());
                
                // Update status
                if (monthlyPayment.getPaidAmount() >= monthlyPayment.getTotalDue()) {
                    monthlyPayment.setStatus(PaymentStatus.PAID);
                } else if (monthlyPayment.getPaidAmount() > 0) {
                    monthlyPayment.setStatus(PaymentStatus.PARTIAL);
                }
                
                vendorMonthlyPaymentRepository.save(monthlyPayment);
                
                // Update legacy total
                vendor.setPaymentDue(vendor.getPaymentDue() - amount);
                vendorRepository.save(vendor);
                
                // Record payment history
                VendorOrderHistory paymentRecord = new VendorOrderHistory();
                paymentRecord.setVendor(vendor);
                paymentRecord.setPlanNumber(planNumber != null ? planNumber : "Monthly Settlement " + monthYear);
                paymentRecord.setAmount(amount);
                paymentRecord.setPaymentDate(LocalDate.now());
                paymentRecord.setType("PAYMENT");
                paymentRecord.setRole(operationType);
                vendorOrderHistoryRepository.save(paymentRecord);
            }
        }
    }
    
    /**
     * Get current month's total due for a vendor
     */
    public double getCurrentMonthDue(Long vendorId) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor == null) return 0;
        
        String currentMonthYear = VendorMonthlyPayment.getCurrentMonthYear();
        List<VendorMonthlyPayment> currentMonthPayments = vendorMonthlyPaymentRepository
            .findByVendorAndMonthYearAndOperationType(vendor, currentMonthYear, vendor.getRole())
            .stream().toList();
        
        // Sum all operation types for current month
        return getAllVendorMonthlyPayments(vendorId, currentMonthYear).stream()
            .mapToDouble(VendorMonthlyPayment::getBalance)
            .sum();
    }
    
    /**
     * Get previous month's total due for a vendor
     */
    public double getPreviousMonthDue(Long vendorId) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor == null) return 0;
        
        String previousMonthYear = VendorMonthlyPayment.getPreviousMonthYear();
        return getAllVendorMonthlyPayments(vendorId, previousMonthYear).stream()
            .mapToDouble(VendorMonthlyPayment::getBalance)
            .sum();
    }
    
    /**
     * Get all monthly payment records for a vendor for a specific month
     */
    public List<VendorMonthlyPayment> getAllVendorMonthlyPayments(Long vendorId, String monthYear) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor == null) return new ArrayList<>();
        
        List<VendorMonthlyPayment> payments = new ArrayList<>();
        for (VendorRole role : VendorRole.values()) {
            vendorMonthlyPaymentRepository
                .findByVendorAndMonthYearAndOperationType(vendor, monthYear, role)
                .ifPresent(payments::add);
        }
        return payments;
    }
    
    /**
     * Get all monthly payment history for a vendor
     */
    public List<VendorMonthlyPayment> getVendorMonthlyPaymentHistory(Long vendorId) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor == null) return new ArrayList<>();
        
        return vendorMonthlyPaymentRepository.findByVendorOrderByMonthYearDesc(vendor);
    }
    
    /**
     * Get monthly payment summary for all vendors (current month)
     */
    public Map<String, Object> getMonthlyPaymentSummary() {
        Map<String, Object> summary = new HashMap<>();
        String currentMonthYear = VendorMonthlyPayment.getCurrentMonthYear();
        
        List<VendorMonthlyPayment> currentMonthPayments = 
            vendorMonthlyPaymentRepository.findByMonthYear(currentMonthYear);
        
        double totalDue = currentMonthPayments.stream()
            .mapToDouble(VendorMonthlyPayment::getBalance)
            .sum();
        
        Map<VendorRole, Double> dueByRole = new HashMap<>();
        for (VendorRole role : VendorRole.values()) {
            double roleDue = currentMonthPayments.stream()
                .filter(p -> p.getOperationType() == role)
                .mapToDouble(VendorMonthlyPayment::getBalance)
                .sum();
            dueByRole.put(role, roleDue);
        }
        
        summary.put("currentMonth", currentMonthYear);
        summary.put("totalDue", totalDue);
        summary.put("dueByRole", dueByRole);
        summary.put("payments", currentMonthPayments);
        
        return summary;
    }
    
    /**
     * Get vendor summary with monthly breakdown
     */
    public Map<String, Object> getVendorMonthlySummary(Long vendorId) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor == null) return null;
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("vendor", vendor);
        
        // Current month dues
        String currentMonthYear = VendorMonthlyPayment.getCurrentMonthYear();
        List<VendorMonthlyPayment> currentMonthPayments = 
            getAllVendorMonthlyPayments(vendorId, currentMonthYear);
        summary.put("currentMonthPayments", currentMonthPayments);
        summary.put("currentMonthTotal", currentMonthPayments.stream()
            .mapToDouble(VendorMonthlyPayment::getBalance).sum());
        
        // Previous month dues
        String previousMonthYear = VendorMonthlyPayment.getPreviousMonthYear();
        List<VendorMonthlyPayment> previousMonthPayments = 
            getAllVendorMonthlyPayments(vendorId, previousMonthYear);
        summary.put("previousMonthPayments", previousMonthPayments);
        summary.put("previousMonthTotal", previousMonthPayments.stream()
            .mapToDouble(VendorMonthlyPayment::getBalance).sum());
        
        // All monthly history
        List<VendorMonthlyPayment> allHistory = getVendorMonthlyPaymentHistory(vendorId);
        summary.put("monthlyHistory", allHistory);
        
        // Recent transactions
        List<VendorOrderHistory> recentHistory = getVendorOrderHistory(vendorId);
        summary.put("recentHistory", recentHistory);
        
        return summary;
    }
    
    public int deleteSettledPaymentsFromPreviousMonth() {
        String previousMonthYear = VendorMonthlyPayment.getPreviousMonthYear();
        
        List<VendorMonthlyPayment> settledPayments = vendorMonthlyPaymentRepository.findAll().stream()
            .filter(payment -> {
                return previousMonthYear.equals(payment.getMonthYear()) && 
                       PaymentStatus.PAID.equals(payment.getStatus());
            })
            .toList();
        
        int count = settledPayments.size();
        vendorMonthlyPaymentRepository.deleteAll(settledPayments);
        return count;
    }
}
