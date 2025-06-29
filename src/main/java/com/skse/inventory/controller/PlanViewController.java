package com.skse.inventory.controller;

import com.skse.inventory.model.*;
import com.skse.inventory.service.PlanService;
import com.skse.inventory.service.VendorService;
import com.skse.inventory.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/plans")
public class PlanViewController {

    @Autowired
    private PlanService planService;

    @Autowired
    private VendorService vendorService;
    
    @Autowired
    private ArticleService articleService;

    @GetMapping
    public String listPlans(Model model) {
        List<Plan> plans = planService.getAllPlans();
        model.addAttribute("plans", plans);
        return "plans/list";
    }

    @GetMapping("/new")
    public String newPlanForm(Model model) {
        model.addAttribute("plan", new Plan());
        model.addAttribute("roles", VendorRole.values());
        model.addAttribute("articles", articleService.getAllArticles());
        model.addAttribute("cuttingTypes", CuttingType.values());
        model.addAttribute("printingTypes", PrintingType.values());
        return "plans/new";
    }

    @PostMapping("/add")
    public String addPlan(@ModelAttribute Plan plan) {
        planService.createPlan(plan);
        return "redirect:/plans";
    }

    @GetMapping("/{planNumber}/edit")
    public String editPlan(@PathVariable String planNumber, Model model) {
        Plan plan = planService.getPlanByNumber(planNumber);
        model.addAttribute("plan", plan);
        model.addAttribute("articles", articleService.getAllArticles());
        model.addAttribute("cuttingTypes", CuttingType.values());
        model.addAttribute("printingTypes", PrintingType.values());
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
        model.addAttribute("vendors", vendorService.getAllVendors());
        model.addAttribute("roles", VendorRole.values());
        return "plans/assign-vendor";
    }

    @PostMapping("/{planNumber}/assign-vendor")
    public String assignVendor(@PathVariable String planNumber,
                               @RequestParam VendorRole role,
                               @RequestParam Long vendorId) {
        Vendor vendor = vendorService.getVendorById(vendorId);
        VendorAssignmentRequest request = new VendorAssignmentRequest();
        request.setRole(role);
        request.setVendor(vendor);
        planService.assignVendorToPlan(planNumber, request);
        return "redirect:/plans";
    }

    @PostMapping("/{planNumber}/move-to-next")
    public String moveToNextState(@PathVariable String planNumber) {
        planService.moveToNextState(planNumber);
        return "redirect:/plans";
    }
    
    @GetMapping("/{planNumber}/final-quantity")
    public String finalQuantityForm(@PathVariable String planNumber, Model model) {
        Plan plan = planService.getPlanByNumber(planNumber);
        model.addAttribute("plan", plan);
        return "plans/final-quantity";
    }
    
    @PostMapping("/{planNumber}/final-quantity")
    public String updateFinalQuantity(@PathVariable String planNumber, 
                                     @RequestParam int finalQuantity) {
        planService.updateFinalQuantity(planNumber, finalQuantity);
        return "redirect:/plans";
    }
    
    @GetMapping("/{planNumber}/send-to-machine")
    public String sendToMachineForm(@PathVariable String planNumber, Model model) {
        Plan plan = planService.getPlanByNumber(planNumber);
        if (plan.getStatus() != PlanStatus.Completed) {
            return "redirect:/plans?error=Plan must be completed before sending to machine";
        }
        model.addAttribute("plan", plan);
        return "plans/send-to-machine";
    }
    
    @PostMapping("/{planNumber}/send-to-machine")
    public String sendToMachine(@PathVariable String planNumber, 
                               @RequestParam int finalQuantity) {
        planService.sendToMachine(planNumber, finalQuantity);
        return "redirect:/plans";
    }
    
    @GetMapping("/dashboard")
    public String planDashboard(Model model) {
        Map<String, Integer> activeOrders = planService.getActiveOrdersByState();
        model.addAttribute("activeOrders", activeOrders);
        return "plans/dashboard";
    }
}
