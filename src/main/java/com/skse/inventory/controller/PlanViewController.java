package com.skse.inventory.controller;

import com.skse.inventory.model.*;
import com.skse.inventory.service.PlanService;
import com.skse.inventory.service.VendorService;
import com.skse.inventory.service.ArticleService;
import com.skse.inventory.service.ColorService;
import com.skse.inventory.service.RateHeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.format.annotation.DateTimeFormat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/plans")
public class PlanViewController {

    private static String redirectPlansWithFocus(String planNumber) {
        return "redirect:/plans?focus=" + URLEncoder.encode(planNumber, StandardCharsets.UTF_8);
    }

    private static String redirectPlansWithErrorAndFocus(String planNumber, String message) {
        return "redirect:/plans?error=" + URLEncoder.encode(message, StandardCharsets.UTF_8)
                + "&focus=" + URLEncoder.encode(planNumber, StandardCharsets.UTF_8);
    }

    @Autowired
    private PlanService planService;

    @Autowired
    private VendorService vendorService;
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private ColorService colorService;
    
    @Autowired
    private RateHeadService rateHeadService;

    @GetMapping
    public String listPlans(@RequestParam(required = false) String planNumber,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createDateFrom,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createDateTo,
                           Model model) {
        model.addAttribute("title", "Plans");
        List<Plan> plans = planService.getPlansFiltered(planNumber, createDateFrom, createDateTo);
        model.addAttribute("plans", plans);
        model.addAttribute("filterPlanNumber", planNumber != null ? planNumber : "");
        model.addAttribute("filterCreateDateFrom", createDateFrom);
        model.addAttribute("filterCreateDateTo", createDateTo);
        return "plans/list";
    }

    @GetMapping("/new")
    public String newPlanForm(Model model) {
        Plan plan = new Plan();
        plan.setCreateDate(LocalDate.now());
        model.addAttribute("title", "Create New Plan");
        model.addAttribute("plan", plan);
        model.addAttribute("roles", VendorRole.values());
        model.addAttribute("articles", articleService.getAllArticles());
        model.addAttribute("colors", colorService.getAllColors());
        model.addAttribute("cuttingTypes", CuttingType.values());
        model.addAttribute("printingRateHeads", rateHeadService.getRateHeadsByOperationType(VendorRole.Printing));
        return "plans/new";
    }

    @PostMapping("/add")
    public String addPlan(@ModelAttribute Plan plan,
                          @RequestParam(required = false) Long printingRateHeadId,
                          Model model) {
        if (printingRateHeadId != null) {
            plan.setPrintingRateHead(rateHeadService.getRateHeadById(printingRateHeadId));
        } else {
            plan.setPrintingRateHead(null);
        }
        try {
            planService.createPlan(plan);
            return "redirect:/plans";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("title", "Create New Plan");
            model.addAttribute("plan", plan);
            model.addAttribute("roles", VendorRole.values());
            model.addAttribute("articles", articleService.getAllArticles());
            model.addAttribute("colors", colorService.getAllColors());
            model.addAttribute("cuttingTypes", CuttingType.values());
            model.addAttribute("printingRateHeads", rateHeadService.getRateHeadsByOperationType(VendorRole.Printing));
            model.addAttribute("error", ex.getMessage());
            return "plans/new";
        }
    }

    @GetMapping("/{planNumber}/edit")
    public String editPlan(@PathVariable String planNumber, Model model) {
        Plan plan = planService.getPlanByNumber(planNumber);
        model.addAttribute("title", "Edit Plan");
        model.addAttribute("plan", plan);
        model.addAttribute("effectiveStatus", planService.getEffectiveStatus(plan));
        model.addAttribute("articles", articleService.getAllArticles());
        model.addAttribute("colors", colorService.getAllColors());
        model.addAttribute("cuttingTypes", CuttingType.values());
        model.addAttribute("printingRateHeads", rateHeadService.getRateHeadsByOperationType(VendorRole.Printing));
        return "plans/edit";
    }

    @PostMapping("/{planNumber}/update")
    public String updatePlan(@PathVariable String planNumber,
                             @ModelAttribute Plan updatedPlan,
                             @RequestParam(required = false) Long printingRateHeadId,
                             Model model) {
        if (printingRateHeadId != null) {
            updatedPlan.setPrintingRateHead(rateHeadService.getRateHeadById(printingRateHeadId));
        } else {
            updatedPlan.setPrintingRateHead(null);
        }
        try {
            planService.updatePlan(planNumber, updatedPlan);
            return redirectPlansWithFocus(planNumber);
        } catch (IllegalArgumentException ex) {
            Plan plan = updatedPlan;
            model.addAttribute("title", "Edit Plan");
            model.addAttribute("plan", plan);
            model.addAttribute("effectiveStatus", planService.getEffectiveStatus(plan));
            model.addAttribute("articles", articleService.getAllArticles());
            model.addAttribute("colors", colorService.getAllColors());
            model.addAttribute("cuttingTypes", CuttingType.values());
            model.addAttribute("printingRateHeads", rateHeadService.getRateHeadsByOperationType(VendorRole.Printing));
            model.addAttribute("error", ex.getMessage());
            return "plans/edit";
        }
    }

    @PostMapping("/{planNumber}/delete")
    public String deletePlan(@PathVariable String planNumber) {
        try {
            planService.deletePlan(planNumber);
        } catch (IllegalArgumentException ex) {
            return "redirect:/plans?error=" + URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
        }
        return "redirect:/plans";
    }

    @PostMapping("/{planNumber}/force-cleanup")
    public String forceCleanupPlan(@PathVariable String planNumber, RedirectAttributes redirectAttributes) {
        try {
            planService.forceCleanupPlan(planNumber);
            redirectAttributes.addFlashAttribute("success",
                    "Plan removed. Vendor pricing and stock balances were reversed where applicable.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/plans";
    }

    @GetMapping("/{planNumber}/assign-vendor")
    public String assignVendorForm(@PathVariable String planNumber, Model model) {
        Plan plan = planService.getPlanByNumber(planNumber);
        model.addAttribute("title", "Assign Vendor");
        model.addAttribute("plan", plan);
        model.addAttribute("vendors", vendorService.getAllVendors());
        model.addAttribute("roles", VendorRole.values());
        return "plans/assign-vendor";
    }

    @PostMapping("/{planNumber}/assign-vendor")
    public String assignVendor(@PathVariable String planNumber,
                               @RequestParam(required = false) Long cuttingVendorId,
                               @RequestParam(required = false) Long printingVendorId,
                               @RequestParam(required = false) Long stitchingVendorId) {
        planService.assignVendorsToPlan(planNumber, cuttingVendorId, printingVendorId, stitchingVendorId);
        return redirectPlansWithFocus(planNumber);
    }

    @GetMapping("/{planNumber}/confirm-next-state")
    public String confirmNextState(@PathVariable String planNumber, Model model) {
        try {
            Plan plan = planService.getPlanByNumber(planNumber);
            if (plan == null) {
                return "redirect:/plans?error=" + URLEncoder.encode("Plan not found: " + planNumber, StandardCharsets.UTF_8);
            }
            PlanStatus next = planService.getNextStatus(plan);
            if (next == null) {
                return redirectPlansWithErrorAndFocus(planNumber, "Plan is already completed.");
            }
            model.addAttribute("plan", plan);
            model.addAttribute("effectiveStatus", planService.getEffectiveStatus(plan));
            model.addAttribute("nextStatus", next);
            model.addAttribute("title", "Confirm transition");
            return "plans/confirm-next-state";
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            if (msg.length() > 500) {
                msg = msg.substring(0, 500) + "…";
            }
            return redirectPlansWithErrorAndFocus(planNumber, msg);
        }
    }

    @PostMapping("/{planNumber}/move-to-next")
    public String moveToNextState(@PathVariable String planNumber,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate transitionDate) {
        Plan plan = planService.getPlanByNumber(planNumber);
        if (plan == null) {
            String message = "Plan not found: " + planNumber;
            return "redirect:/plans?error=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
        }
        LocalDate date = transitionDate != null ? transitionDate : LocalDate.now();
        try {
            planService.moveToNextState(planNumber, date);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return redirectPlansWithErrorAndFocus(planNumber, ex.getMessage());
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            if (msg.length() > 500) {
                msg = msg.substring(0, 500) + "…";
            }
            return redirectPlansWithErrorAndFocus(planNumber, msg);
        }
        return redirectPlansWithFocus(planNumber);
    }
    
    @GetMapping("/{planNumber}/send-to-machine")
    public String sendToMachineForm(@PathVariable String planNumber, Model model) {
        Plan plan = planService.getPlanByNumber(planNumber);
        if (plan.getStatus() != PlanStatus.Completed) {
            return redirectPlansWithErrorAndFocus(planNumber, "Plan must be completed before sending to machine");
        }
        model.addAttribute("title", "Send to Machine");
        model.addAttribute("plan", plan);
        return "plans/send-to-machine";
    }
    
    @PostMapping("/{planNumber}/send-to-machine")
    public String sendToMachine(@PathVariable String planNumber) {
        planService.sendToMachine(planNumber);
        return redirectPlansWithFocus(planNumber);
    }
    
    @GetMapping("/dashboard")
    public String planDashboard(Model model) {
        model.addAttribute("title", "Plan Dashboard");
        Map<String, Integer> activeOrders = planService.getActiveOrdersByState();
        model.addAttribute("activeOrders", activeOrders);
        return "plans/dashboard";
    }
    
    @GetMapping("/{planNumber}/print")
    public String printPlan(@PathVariable String planNumber, Model model) {
        Plan plan = planService.getPlanByNumber(planNumber);
        Article article = articleService.getArticleByName(plan.getArticleName());
        model.addAttribute("plan", plan);
        model.addAttribute("article", article);
        return "plans/print";
    }
}
