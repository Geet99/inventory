package com.skse.inventory.controller;

import com.skse.inventory.model.Plan;
import com.skse.inventory.model.Vendor;
import com.skse.inventory.model.VendorRole;
import com.skse.inventory.service.PlanService;
import com.skse.inventory.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/plans")
public class PlanViewController {

    @Autowired
    private PlanService planService;

    @Autowired
    private VendorService vendorService;

    @GetMapping
    public String listPlans(Model model) {
        List<Plan> plans = planService.getAllPlans(); // Make sure this method exists
        model.addAttribute("plans", plans);
        return "plans/list";
    }

    @GetMapping("/new")
    public String newPlanForm(Model model) {
        model.addAttribute("plan", new Plan());
        model.addAttribute("roles", VendorRole.values());
        return "plans/new";
    }

    @PostMapping("/add")
    public String addPlan(@ModelAttribute Plan plan) {
        planService.createPlan(plan);
        return "redirect:/plans";
    }

    @GetMapping("/{planNumber}/edit")
    public String editPlan(@PathVariable String planNumber, Model model) {
        Plan plan = planService.getPlanByNumber(planNumber); // Make sure this exists
        model.addAttribute("plan", plan);
        return "plans/edit";
    }

    @PostMapping("/{planNumber}/update")
    public String updatePlan(@PathVariable String planNumber, @ModelAttribute Plan updatedPlan) {
        planService.updatePlan(planNumber, updatedPlan);
        return "redirect:/plans";
    }

    @PostMapping("/{planNumber}/delete")
    public String deletePlan(@PathVariable String planNumber) {
        planService.deletePlan(planNumber);
        return "redirect:/plans";
    }

    @GetMapping("/{planNumber}/assign-vendor")
    public String assignVendorForm(@PathVariable String planNumber, Model model) {
        Plan plan = planService.getPlanByNumber(planNumber);
        model.addAttribute("plan", plan);
        model.addAttribute("vendors", vendorService.getAllVendors()); // Ensure this exists
        model.addAttribute("roles", VendorRole.values());
        return "plans/assign-vendor";
    }

    @PostMapping("/{planNumber}/assign-vendor")
    public String assignVendor(@PathVariable String planNumber,
                               @RequestParam VendorRole role,
                               @RequestParam Long vendorId) {
//        planService.assignVendorToPlan(planNumber, role, vendorId); // You may need to adapt this
        return "redirect:/plans";
    }

    @PostMapping("/{planNumber}/move-to-next")
    public String moveToNextState(@PathVariable String planNumber) {
        planService.moveToNextState(planNumber);
        return "redirect:/plans";
    }
}
