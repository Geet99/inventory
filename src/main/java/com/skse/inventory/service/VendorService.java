package com.skse.inventory.service;

import com.skse.inventory.model.*;
import com.skse.inventory.repository.ArticleRepository;
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
    private ArticleRepository articleRepository;
    
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
            // Check if this is a monthly settlement
            if ("MONTHLY_SETTLEMENT".equals(planNumber)) {
                // Settle all previous month's payments
                settlePreviousMonthPayments(vendorId);
            } else {
                // Old-style payment recording (for backward compatibility)
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
    }
    
    /**
     * Settle all previous month's payments for a vendor
     */
    private void settlePreviousMonthPayments(Long vendorId) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor == null) return;
        
        String previousMonthYear = VendorMonthlyPayment.getPreviousMonthYear();
        List<VendorMonthlyPayment> previousMonthPayments = 
            vendorMonthlyPaymentRepository.findByMonthYear(previousMonthYear).stream()
                .filter(p -> p.getVendor().getId().equals(vendorId))
                .filter(p -> p.getStatus() != PaymentStatus.PAID)
                .collect(Collectors.toList());
        
        double totalSettled = 0.0;
        
        for (VendorMonthlyPayment payment : previousMonthPayments) {
            double amountToSettle = payment.getTotalDue() - payment.getPaidAmount();
            
            // Mark as fully paid
            payment.setPaidAmount(payment.getTotalDue());
            payment.setStatus(PaymentStatus.PAID);
            payment.setLastUpdatedDate(LocalDate.now());
            vendorMonthlyPaymentRepository.save(payment);
            
            // Record payment history for this settlement
            VendorOrderHistory paymentRecord = new VendorOrderHistory();
            paymentRecord.setVendor(vendor);
            paymentRecord.setPlanNumber("Settlement " + payment.getFormattedMonthYear() + " - " + payment.getOperationType());
            paymentRecord.setAmount(amountToSettle);
            paymentRecord.setPaymentDate(LocalDate.now());
            paymentRecord.setType("PAYMENT");
            paymentRecord.setRole(payment.getOperationType());
            vendorOrderHistoryRepository.save(paymentRecord);
            
            totalSettled += amountToSettle;
        }
        
        // Update legacy payment due (reduce by the amount settled)
        vendor.setPaymentDue(Math.max(0, vendor.getPaymentDue() - totalSettled));
        vendorRepository.save(vendor);
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
    
    // ========== PRINT/RECEIPT METHODS ==========
    
    /**
     * Get vendor tasks for printing task slip (Previous Month Only)
     */
    public List<Map<String, Object>> getVendorTasksForSlip(Long vendorId) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor == null) {
            return List.of();
        }
        
        String previousMonthYear = VendorMonthlyPayment.getPreviousMonthYear();
        
        // Get all plans assigned to this vendor
        List<Plan> allPlans = planRepository.findAll();
        List<Map<String, Object>> tasks = new java.util.ArrayList<>();
        
        for (Plan plan : allPlans) {
            boolean isAssigned = false;
            String status = "Not Started";
            LocalDate startDate = null;
            LocalDate endDate = null;
            double paymentDue = 0.0;
            boolean isFromPreviousMonth = false;
            String operationType = "";
            String rateHeadName = "";
            double rateHeadCost = 0.0;
            
            // Check if vendor is assigned to cutting
            if (plan.getCuttingVendor() != null && plan.getCuttingVendor().getId().equals(vendorId)) {
                isAssigned = true;
                operationType = "Cutting";
                // Get cutting rate head name from article
                Article article = articleRepository.findByName(plan.getArticleName()).orElse(null);
                if (article != null && article.getCuttingRateHead() != null) {
                    rateHeadName = article.getCuttingRateHead().getName();
                    rateHeadCost = article.getCuttingRateHead().getCost();
                }
                startDate = plan.getCuttingStartDate();
                endDate = plan.getCuttingEndDate();
                paymentDue = plan.getCuttingVendorPaymentDue();
                
                // Check if this task is from previous month
                if (endDate != null) {
                    status = "Completed";
                    isFromPreviousMonth = previousMonthYear.equals(VendorMonthlyPayment.getMonthYearString(endDate));
                } else if (startDate != null) {
                    status = "In Progress";
                    isFromPreviousMonth = previousMonthYear.equals(VendorMonthlyPayment.getMonthYearString(startDate));
                }
            }
            // Check if vendor is assigned to printing
            else if (plan.getPrintingVendor() != null && plan.getPrintingVendor().getId().equals(vendorId)) {
                isAssigned = true;
                operationType = "Printing";
                // Get printing rate head name from plan
                if (plan.getPrintingRateHead() != null) {
                    rateHeadName = plan.getPrintingRateHead().getName();
                    rateHeadCost = plan.getPrintingRateHead().getCost();
                }
                startDate = plan.getPrintingStartDate();
                endDate = plan.getPrintingEndDate();
                paymentDue = plan.getPrintingVendorPaymentDue();
                
                // Check if this task is from previous month
                if (endDate != null) {
                    status = "Completed";
                    isFromPreviousMonth = previousMonthYear.equals(VendorMonthlyPayment.getMonthYearString(endDate));
                } else if (startDate != null) {
                    status = "In Progress";
                    isFromPreviousMonth = previousMonthYear.equals(VendorMonthlyPayment.getMonthYearString(startDate));
                }
            }
            // Check if vendor is assigned to stitching
            else if (plan.getStitchingVendor() != null && plan.getStitchingVendor().getId().equals(vendorId)) {
                isAssigned = true;
                operationType = "Stitching";
                // Get stitching rate head name from article
                Article article = articleRepository.findByName(plan.getArticleName()).orElse(null);
                if (article != null && article.getStitchingRateHead() != null) {
                    rateHeadName = article.getStitchingRateHead().getName();
                    rateHeadCost = article.getStitchingRateHead().getCost();
                }
                startDate = plan.getStitchingStartDate();
                endDate = plan.getStitchingEndDate();
                paymentDue = plan.getStitchingVendorPaymentDue();
                
                // Check if this task is from previous month
                if (endDate != null) {
                    status = "Completed";
                    isFromPreviousMonth = previousMonthYear.equals(VendorMonthlyPayment.getMonthYearString(endDate));
                } else if (startDate != null) {
                    status = "In Progress";
                    isFromPreviousMonth = previousMonthYear.equals(VendorMonthlyPayment.getMonthYearString(startDate));
                }
            }
            
            // Only include tasks from previous month
            if (isAssigned && isFromPreviousMonth) {
                Map<String, Object> task = new HashMap<>();
                task.put("planNumber", plan.getPlanNumber());
                task.put("articleName", plan.getArticleName());
                task.put("color", plan.getColor());
                task.put("sizeQuantityPairs", plan.getSizeQuantityPairs());
                task.put("quantity", plan.getTotal());
                task.put("status", status);
                task.put("operationType", operationType);
                task.put("rateHeadName", rateHeadName);
                task.put("rateHeadCost", rateHeadCost);
                task.put("startDate", startDate);
                task.put("endDate", endDate);
                task.put("paymentDue", paymentDue);
                tasks.add(task);
            }
        }
        
        return tasks;
    }
    
    /**
     * Get payment receipt data for printing
     */
    public Map<String, Object> getPaymentReceiptData(Long vendorId) {
        Vendor vendor = getVendorById(vendorId);
        Map<String, Object> data = new HashMap<>();
        
        if (vendor == null) {
            return data;
        }
        
        String previousMonthYear = VendorMonthlyPayment.getPreviousMonthYear();
        List<VendorMonthlyPayment> previousMonthPayments = 
            vendorMonthlyPaymentRepository.findByMonthYear(previousMonthYear).stream()
                .filter(p -> p.getVendor().getId().equals(vendorId))
                .collect(Collectors.toList());
        
        // Get work details - find all plans completed in previous month for this vendor
        List<Map<String, Object>> workDetails = new java.util.ArrayList<>();
        List<Plan> allPlans = planRepository.findAll();
        
        int totalQuantity = 0;
        double totalDue = 0.0;
        double previousPayment = 0.0;
        
        for (Plan plan : allPlans) {
            // Check cutting
            if (plan.getCuttingVendor() != null && 
                plan.getCuttingVendor().getId().equals(vendorId) &&
                plan.getCuttingEndDate() != null) {
                
                String completionMonthYear = VendorMonthlyPayment.getMonthYearString(plan.getCuttingEndDate());
                if (previousMonthYear.equals(completionMonthYear)) {
                    Map<String, Object> work = new HashMap<>();
                    work.put("planNumber", plan.getPlanNumber());
                    work.put("articleName", plan.getArticleName());
                    work.put("color", plan.getColor());
                    work.put("quantity", plan.getTotal());
                    work.put("ratePerUnit", plan.getCuttingVendorPaymentDue() / plan.getTotal());
                    work.put("completionDate", plan.getCuttingEndDate());
                    work.put("amount", plan.getCuttingVendorPaymentDue());
                    workDetails.add(work);
                    
                    totalQuantity += plan.getTotal();
                    totalDue += plan.getCuttingVendorPaymentDue();
                }
            }
            
            // Check printing
            if (plan.getPrintingVendor() != null && 
                plan.getPrintingVendor().getId().equals(vendorId) &&
                plan.getPrintingEndDate() != null) {
                
                String completionMonthYear = VendorMonthlyPayment.getMonthYearString(plan.getPrintingEndDate());
                if (previousMonthYear.equals(completionMonthYear)) {
                    Map<String, Object> work = new HashMap<>();
                    work.put("planNumber", plan.getPlanNumber());
                    work.put("articleName", plan.getArticleName());
                    work.put("color", plan.getColor());
                    work.put("quantity", plan.getTotal());
                    work.put("ratePerUnit", plan.getPrintingVendorPaymentDue() / plan.getTotal());
                    work.put("completionDate", plan.getPrintingEndDate());
                    work.put("amount", plan.getPrintingVendorPaymentDue());
                    workDetails.add(work);
                    
                    totalQuantity += plan.getTotal();
                    totalDue += plan.getPrintingVendorPaymentDue();
                }
            }
            
            // Check stitching
            if (plan.getStitchingVendor() != null && 
                plan.getStitchingVendor().getId().equals(vendorId) &&
                plan.getStitchingEndDate() != null) {
                
                String completionMonthYear = VendorMonthlyPayment.getMonthYearString(plan.getStitchingEndDate());
                if (previousMonthYear.equals(completionMonthYear)) {
                    Map<String, Object> work = new HashMap<>();
                    work.put("planNumber", plan.getPlanNumber());
                    work.put("articleName", plan.getArticleName());
                    work.put("color", plan.getColor());
                    work.put("quantity", plan.getTotal());
                    work.put("ratePerUnit", plan.getStitchingVendorPaymentDue() / plan.getTotal());
                    work.put("completionDate", plan.getStitchingEndDate());
                    work.put("amount", plan.getStitchingVendorPaymentDue());
                    workDetails.add(work);
                    
                    totalQuantity += plan.getTotal();
                    totalDue += plan.getStitchingVendorPaymentDue();
                }
            }
        }
        
        // Calculate previous payments
        for (VendorMonthlyPayment payment : previousMonthPayments) {
            previousPayment += payment.getPaidAmount();
        }
        
        double amountSettled = totalDue - previousPayment;
        
        // Format settlement period
        String formattedMonth = previousMonthPayments.isEmpty() ? 
            VendorMonthlyPayment.getPreviousMonthYear() : 
            previousMonthPayments.get(0).getFormattedMonthYear();
        
        data.put("settlementPeriod", formattedMonth);
        data.put("workDetails", workDetails);
        data.put("totalQuantity", totalQuantity);
        data.put("totalDue", totalDue);
        data.put("previousPayment", previousPayment);
        data.put("amountSettled", amountSettled);
        data.put("amountInWords", convertAmountToWords(amountSettled));
        
        return data;
    }
    
    /**
     * Convert amount to words (Indian Rupees)
     */
    private String convertAmountToWords(double amount) {
        if (amount == 0) {
            return "Zero Rupees Only";
        }
        
        int rupees = (int) amount;
        int paise = (int) Math.round((amount - rupees) * 100);
        
        String result = convertNumberToWords(rupees) + " Rupees";
        if (paise > 0) {
            result += " and " + convertNumberToWords(paise) + " Paise";
        }
        result += " Only";
        
        return result;
    }
    
    private String convertNumberToWords(int number) {
        if (number == 0) return "Zero";
        
        String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"};
        String[] teens = {"Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
        
        if (number < 10) return units[number];
        if (number < 20) return teens[number - 10];
        if (number < 100) return tens[number / 10] + (number % 10 != 0 ? " " + units[number % 10] : "");
        if (number < 1000) return units[number / 100] + " Hundred" + (number % 100 != 0 ? " " + convertNumberToWords(number % 100) : "");
        if (number < 100000) return convertNumberToWords(number / 1000) + " Thousand" + (number % 1000 != 0 ? " " + convertNumberToWords(number % 1000) : "");
        if (number < 10000000) return convertNumberToWords(number / 100000) + " Lakh" + (number % 100000 != 0 ? " " + convertNumberToWords(number % 100000) : "");
        
        return convertNumberToWords(number / 10000000) + " Crore" + (number % 10000000 != 0 ? " " + convertNumberToWords(number % 10000000) : "");
    }
    
    /**
     * Get formatted previous month year (e.g., "November 2024")
     */
    public String getFormattedPreviousMonthYear() {
        String monthYear = VendorMonthlyPayment.getPreviousMonthYear();
        if (monthYear != null && monthYear.length() == 6) {
            String month = monthYear.substring(0, 2);
            String year = monthYear.substring(2);
            return getMonthName(month) + " " + year;
        }
        return monthYear;
    }
    
    private String getMonthName(String month) {
        return switch (month) {
            case "01" -> "January";
            case "02" -> "February";
            case "03" -> "March";
            case "04" -> "April";
            case "05" -> "May";
            case "06" -> "June";
            case "07" -> "July";
            case "08" -> "August";
            case "09" -> "September";
            case "10" -> "October";
            case "11" -> "November";
            case "12" -> "December";
            default -> month;
        };
    }
}
