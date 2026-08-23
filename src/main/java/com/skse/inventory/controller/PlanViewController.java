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

    private static PlanStatus parsePlanStatusOrNull(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        String normalized = rawStatus.trim();
        for (PlanStatus status : PlanStatus.values()) {
            if (status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        return null;
    }

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
                           @RequestParam(required = false) String articleName,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createDateFrom,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createDateTo,
                           Model model) {
        model.addAttribute("title", "Plans");
        PlanStatus statusFilter = parsePlanStatusOrNull(status);
        List<Plan> plans = planService.getPlansFiltered(planNumber, articleName, statusFilter, createDateFrom, createDateTo);
        model.addAttribute("plans", plans);
        model.addAttribute("filterPlanNumber", planNumber != null ? planNumber : "");
        model.addAttribute("filterArticleName", articleName != null ? articleName : "");
        model.addAttribute("filterStatus", statusFilter);
        model.addAttribute("planStatuses", PlanStatus.values());
        if (status != null && !status.isBlank() && statusFilter == null) {
            model.addAttribute("error", "Invalid status filter ignored: " + status);
        }
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
        model.addAttribute("printingRateHeads", rateHeadService.getActiveRateHeadsByOperationType(VendorRole.Printing));
        return "plans/new";
    }

    /**
     * Plan numbers live in query or form fields (not path segments) so characters such as ';'
     * are not interpreted as matrix parameters by Spring MVC's PathPattern parser.
     */
    private String editPlanModel(String planNumber, Model model) {
        Plan plan = planService.getPlanByNumber(planNumber);
        if (plan == null) {
            return "redirect:/plans?error=" + URLEncoder.encode("Plan not found: " + planNumber, StandardCharsets.UTF_8);
        }
        model.addAttribute("title", "Edit Plan");
        model.addAttribute("plan", plan);
        model.addAttribute("effectiveStatus", planService.getEffectiveStatus(plan));
        model.addAttribute("articles", articleService.getAllArticles());
        model.addAttribute("colors", colorService.getAllColors());
        model.addAttribute("cuttingTypes", CuttingType.values());
        model.addAttribute("printingRateHeads", rateHeadService.getActiveRateHeadsByOperationType(VendorRole.Printing));
        return "plans/edit";
    }

    @GetMapping("/edit")
    public String editPlanQuery(@RequestParam("planNumber") String planNumber, Model model) {
        return editPlanModel(planNumber, model);
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
            model.addAttribute("printingRateHeads", rateHeadService.getActiveRateHeadsByOperationType(VendorRole.Printing));
            model.addAttribute("error", ex.getMessage());
            return "plans/new";
        }
    }

    @PostMapping("/update")
    public String updatePlan(@ModelAttribute Plan updatedPlan,
                             @RequestParam(required = false) Long printingRateHeadId,
                             Model model) {
        String planNumber = updatedPlan.getPlanNumber();
        if (printingRateHeadId != null) {
            updatedPlan.setPrintingRateHead(rateHeadService.getRateHeadById(printingRateHeadId));
        } else {
            updatedPlan.setPrintingRateHead(null);
        }
        try {
            planService.updatePlan(planNumber, updatedPlan);
            return redirectPlansWithFocus(planNumber);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Plan plan = updatedPlan;
            model.addAttribute("title", "Edit Plan");
            model.addAttribute("plan", plan);
            model.addAttribute("effectiveStatus", planService.getEffectiveStatus(plan));
            model.addAttribute("articles", articleService.getAllArticles());
            model.addAttribute("colors", colorService.getAllColors());
            model.addAttribute("cuttingTypes", CuttingType.values());
            model.addAttribute("printingRateHeads", rateHeadService.getActiveRateHeadsByOperationType(VendorRole.Printing));
            model.addAttribute("error", ex.getMessage());
            return "plans/edit";
        }
    }

    @PostMapping("/delete")
    public String deletePlan(@RequestParam("planNumber") String planNumber, RedirectAttributes redirectAttributes) {
        try {
            planService.deletePlan(planNumber);
            redirectAttributes.addFlashAttribute("success",
                    "Plan deleted. Vendor payment records reversed where applicable.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectPlansWithFocus(planNumber);
    }

    @PostMapping("/force-cleanup")
    public String forceCleanupPlan(@RequestParam("planNumber") String planNumber, RedirectAttributes redirectAttributes) {
        try {
            planService.forceCleanupPlan(planNumber);
            redirectAttributes.addFlashAttribute("success",
                    "Plan removed. Vendor payment records were cleaned up.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectPlansWithFocus(planNumber);
    }

    @GetMapping("/assign-vendor")
    public String assignVendorForm(@RequestParam("planNumber") String planNumber, Model model) {
        Plan plan = planService.getPlanByNumber(planNumber);
        if (plan == null) {
            return "redirect:/plans?error=" + URLEncoder.encode("Plan not found: " + planNumber, StandardCharsets.UTF_8);
        }
        model.addAttribute("title", "Assign Vendor");
        model.addAttribute("plan", plan);
        model.addAttribute("vendors", vendorService.getActiveVendors());
        model.addAttribute("roles", VendorRole.values());
        return "plans/assign-vendor";
    }

    @PostMapping("/assign-vendor")
    public String assignVendor(@RequestParam("planNumber") String planNumber,
                               @RequestParam(required = false) Long cuttingVendorId,
                               @RequestParam(required = false) Long printingVendorId,
                               @RequestParam(required = false) Long stitchingVendorId) {
        try {
            planService.assignVendorsToPlan(planNumber, cuttingVendorId, printingVendorId, stitchingVendorId);
            return redirectPlansWithFocus(planNumber);
        } catch (Exception ex) {
            return redirectPlansWithErrorAndFocus(planNumber, ex.getMessage());
        }
    }

    @GetMapping("/confirm-next-state")
    public String confirmNextState(@RequestParam("planNumber") String planNumber, Model model) {
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

    @PostMapping("/move-to-next")
    public String moveToNextState(@RequestParam("planNumber") String planNumber,
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
    
    @GetMapping("/send-to-machine")
    public String sendToMachineForm(@RequestParam("planNumber") String planNumber, Model model) {
        Plan plan = planService.getPlanByNumber(planNumber);
        if (plan == null) {
            return "redirect:/plans?error=" + URLEncoder.encode("Plan not found: " + planNumber, StandardCharsets.UTF_8);
        }
        if (plan.getStatus() != PlanStatus.Completed) {
            return redirectPlansWithErrorAndFocus(planNumber, "Plan must be completed before sending to machine");
        }
        model.addAttribute("title", "Send to Machine");
        model.addAttribute("plan", plan);
        return "plans/send-to-machine";
    }
    
    @PostMapping("/send-to-machine")
    public String sendToMachine(@RequestParam("planNumber") String planNumber) {
        try {
            planService.sendToMachine(planNumber);
            return redirectPlansWithFocus(planNumber);
        } catch (Exception ex) {
            return redirectPlansWithErrorAndFocus(planNumber, ex.getMessage());
        }
    }
    
    @GetMapping("/dashboard")
    public String planDashboard(Model model) {
        model.addAttribute("title", "Plan Dashboard");
        Map<String, Integer> activeOrders = planService.getActiveOrdersByState();
        model.addAttribute("activeOrders", activeOrders);
        return "plans/dashboard";
    }
    
    @GetMapping("/print")
    public String printPlan(@RequestParam("planNumber") String planNumber, Model model) {
        Plan plan = planService.getPlanByNumber(planNumber);
        if (plan == null) {
            return "redirect:/plans?error=" + URLEncoder.encode("Plan not found: " + planNumber, StandardCharsets.UTF_8);
        }
        Article article = articleService.getArticleByName(plan.getArticleName());
        model.addAttribute("plan", plan);
        model.addAttribute("article", article);
        return "plans/print";
    }

    /* Legacy path-style routes (plan numbers without ';' etc. still work). */
    @GetMapping("/{planNumber}/edit")
    public String editPlanPath(@PathVariable String planNumber, Model model) {
        return editPlanModel(planNumber, model);
    }

    @PostMapping("/{planNumber}/update")
    public String updatePlanPath(@PathVariable String planNumber,
                                 @ModelAttribute Plan updatedPlan,
                                 @RequestParam(required = false) Long printingRateHeadId,
                                 Model model) {
        updatedPlan.setPlanNumber(planNumber);
        return updatePlan(updatedPlan, printingRateHeadId, model);
    }

    @PostMapping("/{planNumber}/delete")
    public String deletePlanPath(@PathVariable String planNumber, RedirectAttributes redirectAttributes) {
        return deletePlan(planNumber, redirectAttributes);
    }

    @PostMapping("/{planNumber}/force-cleanup")
    public String forceCleanupPlanPath(@PathVariable String planNumber, RedirectAttributes redirectAttributes) {
        return forceCleanupPlan(planNumber, redirectAttributes);
    }

    @GetMapping("/{planNumber}/assign-vendor")
    public String assignVendorFormPath(@PathVariable String planNumber, Model model) {
        return assignVendorForm(planNumber, model);
    }

    @PostMapping("/{planNumber}/assign-vendor")
    public String assignVendorPath(@PathVariable String planNumber,
                                   @RequestParam(required = false) Long cuttingVendorId,
                                   @RequestParam(required = false) Long printingVendorId,
                                   @RequestParam(required = false) Long stitchingVendorId) {
        return assignVendor(planNumber, cuttingVendorId, printingVendorId, stitchingVendorId);
    }

    @GetMapping("/{planNumber}/confirm-next-state")
    public String confirmNextStatePath(@PathVariable String planNumber, Model model) {
        return confirmNextState(planNumber, model);
    }

    @PostMapping("/{planNumber}/move-to-next")
    public String moveToNextStatePath(@PathVariable String planNumber,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate transitionDate) {
        return moveToNextState(planNumber, transitionDate);
    }

    @GetMapping("/{planNumber}/send-to-machine")
    public String sendToMachineFormPath(@PathVariable String planNumber, Model model) {
        return sendToMachineForm(planNumber, model);
    }

    @PostMapping("/{planNumber}/send-to-machine")
    public String sendToMachinePath(@PathVariable String planNumber) {
        return sendToMachine(planNumber);
    }

    @GetMapping("/{planNumber}/print")
    public String printPlanPath(@PathVariable String planNumber, Model model) {
        return printPlan(planNumber, model);
    }
}
