package com.skse.inventory.service;

import com.skse.inventory.model.PaymentStatus;
import com.skse.inventory.model.VendorMonthlyPayment;
import com.skse.inventory.repository.VendorMonthlyPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CleanupService {
    
    @Autowired
    private PlanService planService;
    
    @Autowired
    private VendorService vendorService;
    
    @Autowired
    private VendorMonthlyPaymentRepository vendorMonthlyPaymentRepository;
    
    /**
     * Check if cleanup is allowed (all last month's payments must be settled)
     */
    public Map<String, Object> checkCleanupEligibility() {
        Map<String, Object> result = new HashMap<>();
        
        String previousMonthYear = VendorMonthlyPayment.getPreviousMonthYear();
        List<VendorMonthlyPayment> previousMonthPayments = 
            vendorMonthlyPaymentRepository.findByMonthYear(previousMonthYear);
        
        long unsettledCount = previousMonthPayments.stream()
            .filter(p -> p.getStatus() != PaymentStatus.PAID)
            .count();
        
        result.put("isEligible", unsettledCount == 0);
        result.put("unsettledCount", unsettledCount);
        result.put("totalPayments", previousMonthPayments.size());
        result.put("previousMonth", previousMonthYear);
        
        return result;
    }
    
    @Transactional
    public Map<String, Integer> deleteRecordsFromPreviousMonth() {
        Map<String, Integer> result = new HashMap<>();
        
        // First check if cleanup is allowed
        Map<String, Object> eligibility = checkCleanupEligibility();
        if (!(Boolean) eligibility.get("isEligible")) {
            throw new IllegalStateException(
                "Cannot perform cleanup. " + eligibility.get("unsettledCount") + 
                " payment(s) from previous month are still unsettled. Please settle all payments first.");
        }
        
        // Delete completed plans from previous month
        int deletedPlans = planService.deleteCompletedPlansFromPreviousMonth();
        result.put("deletedPlans", deletedPlans);
        
        // Delete settled payment records from previous month
        int deletedPayments = vendorService.deleteSettledPaymentsFromPreviousMonth();
        result.put("deletedPayments", deletedPayments);
        
        return result;
    }
}

