package com.skse.inventory.controller;

import com.skse.inventory.service.CleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/cleanup")
public class CleanupViewController {
    
    @Autowired
    private CleanupService cleanupService;
    
    @GetMapping
    public String showCleanupPage(Model model) {
        return "cleanup/index";
    }
    
    @PostMapping("/previous-month")
    public String deletePreviousMonthRecords(RedirectAttributes redirectAttributes) {
        try {
            Map<String, Integer> result = cleanupService.deleteRecordsFromPreviousMonth();
            int deletedPlans = result.get("deletedPlans");
            int deletedPayments = result.get("deletedPayments");
            
            redirectAttributes.addFlashAttribute("success", 
                String.format("Successfully deleted %d completed plans and %d settled payment records from previous month.", 
                    deletedPlans, deletedPayments));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Failed to delete records: " + e.getMessage());
        }
        
        return "redirect:/cleanup";
    }
}

