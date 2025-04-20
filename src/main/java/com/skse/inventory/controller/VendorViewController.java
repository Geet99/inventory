package com.skse.inventory.controller;

import com.skse.inventory.model.Vendor;
import com.skse.inventory.model.VendorRole;
import com.skse.inventory.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/vendors")
public class VendorViewController {

    @Autowired
    private VendorService vendorService;

    @GetMapping
    public String listVendors(@RequestParam(name = "role", required = false) VendorRole role, Model model) {
        List<Vendor> vendors = (role != null)
                ? vendorService.getVendorsByRole(role)
                : vendorService.getVendorsByRole(VendorRole.Cutting); // default view

        model.addAttribute("vendors", vendors);
        model.addAttribute("roles", VendorRole.values());
        model.addAttribute("selectedRole", role != null ? role : VendorRole.Cutting);
        return "vendors/list";
    }

    @GetMapping("/summary")
    public String vendorSummary(Model model) {
        Map<String, Object> summary = vendorService.getVendorSummary();
        model.addAttribute("summary", summary);
        return "vendors/summary";
    }

    @GetMapping("/payments-due")
    public String paymentsDue(Model model) {
        Map<String, Double> payments = vendorService.getVendorPaymentsDue();
        model.addAttribute("payments", payments);
        return "vendors/payments-due";
    }

    @GetMapping("/edit/{id}")
    public String editVendorForm(@PathVariable Long id, Model model) {
        Vendor vendor = vendorService.getVendorById(id).orElseThrow(); // Make sure this method exists in service
        model.addAttribute("vendor", vendor);
        model.addAttribute("roles", VendorRole.values());
        return "vendors/edit";
    }

    @PostMapping("/add")
    public String addVendor(@RequestParam String name,
                            @RequestParam VendorRole role,
                            Model model) {
        Vendor vendor = new Vendor();
        vendor.setName(name);
        vendor.setRole(role);
        vendorService.addVendor(vendor);
        return "redirect:/vendors?role=" + role; // Refresh list after adding
    }

    @PostMapping("/payment")
    public String recordPayment(@RequestParam Long vendorId,
                                @RequestParam double payment,
                                Model model) {
        vendorService.recordVendorPayment(vendorId, payment);
        return "redirect:/vendors";
    }

    @PostMapping("/edit/{id}")
    public String updateVendor(@PathVariable Long id, @ModelAttribute Vendor vendor) {
        vendorService.updateVendor(id, vendor);
        return "redirect:/vendors?role=" + vendor.getRole();
    }
}
