package com.skse.inventory.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class CleanupService {
    
    @Autowired
    private PlanService planService;
    
    @Autowired
    private VendorService vendorService;
    
    @Transactional
    public Map<String, Integer> deleteRecordsFromPreviousMonth() {
        Map<String, Integer> result = new HashMap<>();
        
        // Delete completed plans from previous month
        int deletedPlans = planService.deleteCompletedPlansFromPreviousMonth();
        result.put("deletedPlans", deletedPlans);
        
        // Delete settled payment records from previous month
        int deletedPayments = vendorService.deleteSettledPaymentsFromPreviousMonth();
        result.put("deletedPayments", deletedPayments);
        
        return result;
    }
}

