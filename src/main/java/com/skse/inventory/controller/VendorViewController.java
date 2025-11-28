package com.skse.inventory.controller;

import com.skse.inventory.model.Vendor;
import com.skse.inventory.model.VendorRole;
import com.skse.inventory.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/vendors")
public class VendorViewController {

    @Autowired
    private VendorService vendorService;

    @GetMapping
    public String listVendors(Model model) {
        List<Vendor> vendors = vendorService.getAllVendors();
        model.addAttribute("vendors", vendors);
        return "vendors/list";
    }

    @GetMapping("/new")
    public String newVendorForm(Model model) {
        model.addAttribute("vendor", new Vendor());
        model.addAttribute("roles", VendorRole.values());
        return "vendors/add";
    }

    @PostMapping("/add")
    public String addVendor(@ModelAttribute Vendor vendor) {
        vendorService.createVendor(vendor);
        return "redirect:/vendors";
    }

    @GetMapping("/{id}/edit")
    public String editVendor(@PathVariable Long id, Model model) {
        Vendor vendor = vendorService.getVendorById(id);
        model.addAttribute("vendor", vendor);
        model.addAttribute("roles", VendorRole.values());
        return "vendors/edit";
    }

    @PostMapping("/{id}/update")
    public String updateVendor(@PathVariable Long id, 
                               @RequestParam String name,
                               @RequestParam VendorRole role,
                               @RequestParam(required = false, defaultValue = "true") boolean active) {
        vendorService.updateVendor(id, name, role, active);
        return "redirect:/vendors";
    }

    @PostMapping("/{id}/delete")
    public String deleteVendor(@PathVariable Long id) {
        vendorService.deleteVendor(id);
        return "redirect:/vendors";
    }

    @GetMapping("/summary")
    public String vendorSummary(Model model) {
        // Use new monthly payment summary
        Map<String, Object> monthlySummary = vendorService.getMonthlyPaymentSummary();
        Map<String, Object> legacySummary = vendorService.getVendorPaymentSummary();
        
        model.addAttribute("monthlySummary", monthlySummary);
        model.addAttribute("summary", legacySummary); // For backward compatibility
        return "vendors/summary";
    }

    @GetMapping("/{id}/details")
    public String vendorDetails(@PathVariable Long id, Model model) {
        // Use new monthly summary
        Map<String, Object> monthlyDetails = vendorService.getVendorMonthlySummary(id);
        Map<String, Object> legacyDetails = vendorService.getVendorDetailedSummary(id);
        
        if (monthlyDetails == null) {
            return "redirect:/vendors";
        }
        
        model.addAttribute("monthlyDetails", monthlyDetails);
        model.addAttribute("details", legacyDetails); // For backward compatibility
        return "vendors/details";
    }

    @GetMapping("/{id}/payment")
    public String vendorPaymentForm(@PathVariable Long id, Model model) {
        Vendor vendor = vendorService.getVendorById(id);
        // Get previous month's payment details
        Map<String, Object> monthlyDetails = vendorService.getVendorMonthlySummary(id);
        
        model.addAttribute("vendor", vendor);
        model.addAttribute("monthlyDetails", monthlyDetails);
        return "vendors/payment";
    }

    @PostMapping("/{id}/payment")
    public String recordPayment(@PathVariable Long id, 
                               @RequestParam double amount,
                               @RequestParam(required = false) String planNumber) {
        // For monthly settlements, planNumber is not needed
        if (planNumber == null || planNumber.isEmpty()) {
            planNumber = "MONTHLY_SETTLEMENT";
        }
        vendorService.recordVendorPayment(id, amount, planNumber);
        
        // Redirect to payment receipt for printing
        return "redirect:/vendors/" + id + "/payment-receipt";
    }

    @GetMapping("/report")
    public String vendorReport(Model model,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        List<Map<String, Object>> report = vendorService.getVendorPaymentReport(start, end);
        model.addAttribute("report", report);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        
        return "vendors/report";
    }

    @GetMapping("/by-role/{role}")
    public String vendorsByRole(@PathVariable VendorRole role, Model model) {
        List<Vendor> vendors = vendorService.getVendorsByRole(role);
        model.addAttribute("vendors", vendors);
        model.addAttribute("role", role);
        return "vendors/by-role";
    }
    
    @GetMapping("/{id}/task-slip")
    public String printTaskSlip(@PathVariable Long id, Model model) {
        Vendor vendor = vendorService.getVendorById(id);
        List<Map<String, Object>> tasks = vendorService.getVendorTasksForSlip(id);
        
        int totalQuantity = tasks.stream()
            .mapToInt(task -> (Integer) task.get("quantity"))
            .sum();
        
        double totalPaymentDue = tasks.stream()
            .mapToDouble(task -> (Double) task.get("paymentDue"))
            .sum();
        
        // Get formatted period (e.g., "November 2024")
        String period = vendorService.getFormattedPreviousMonthYear();
        
        model.addAttribute("vendor", vendor);
        model.addAttribute("tasks", tasks);
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("totalPaymentDue", totalPaymentDue);
        model.addAttribute("period", period);
        
        return "vendors/task-slip";
    }
    
    @GetMapping("/{id}/payment-receipt")
    public String printPaymentReceipt(@PathVariable Long id, Model model) {
        Vendor vendor = vendorService.getVendorById(id);
        Map<String, Object> receiptData = vendorService.getPaymentReceiptData(id);
        
        model.addAttribute("vendor", vendor);
        model.addAttribute("settlementPeriod", receiptData.get("settlementPeriod"));
        model.addAttribute("workDetails", receiptData.get("workDetails"));
        model.addAttribute("totalQuantity", receiptData.get("totalQuantity"));
        model.addAttribute("totalDue", receiptData.get("totalDue"));
        model.addAttribute("previousPayment", receiptData.get("previousPayment"));
        model.addAttribute("amountSettled", receiptData.get("amountSettled"));
        model.addAttribute("amountInWords", receiptData.get("amountInWords"));
        
        return "vendors/payment-receipt";
    }
}
