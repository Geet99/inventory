package com.skse.inventory.controller;

import com.skse.inventory.model.RateHead;
import com.skse.inventory.model.RateHeadPrice;
import com.skse.inventory.model.VendorRole;
import com.skse.inventory.service.PlanService;
import com.skse.inventory.service.RateHeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/rateheads")
public class RateHeadViewController {

    @Autowired
    private RateHeadService rateHeadService;

    @Autowired
    private PlanService planService;

    @GetMapping
    public String listRateHeads(@RequestParam(required = false) VendorRole operationType, Model model) {
        model.addAttribute("title", "Rate Heads");
        model.addAttribute("operationTypes", VendorRole.values());
        model.addAttribute("currentFilter", operationType);
        List<RateHead> rateHeads = operationType == null
                ? rateHeadService.getAllRateHeads()
                : rateHeadService.getRateHeadsByOperationType(operationType);
        model.addAttribute("rateHeads", rateHeads);

        // Build a map of rateHeadId -> latest effectiveFrom date
        Map<Long, LocalDate> latestEffectiveDates = new HashMap<>();
        for (RateHead rh : rateHeads) {
            List<RateHeadPrice> prices = rateHeadService.getPriceHistory(rh);
            if (!prices.isEmpty()) {
                latestEffectiveDates.put(rh.getId(), prices.get(0).getEffectiveFrom());
            }
        }
        model.addAttribute("latestEffectiveDates", latestEffectiveDates);

        // Show seed button if any rate head has cost but no price entries
        boolean needsSeeding = rateHeads.stream().anyMatch(rh ->
                rh.getCost() != null && !latestEffectiveDates.containsKey(rh.getId()));
        model.addAttribute("needsSeeding", needsSeeding);

        return "rateheads/list";
    }

    @GetMapping("/operation/{operationType}")
    public String listRateHeadsByOperationRedirect(@PathVariable VendorRole operationType) {
        return "redirect:/rateheads?operationType=" + operationType.name();
    }

    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("title", "Add Rate Head");
        model.addAttribute("rateHead", new RateHead());
        model.addAttribute("operationTypes", VendorRole.values());
        return "rateheads/add";
    }

    @PostMapping
    public String saveRateHead(@ModelAttribute RateHead rateHead,
                               @RequestParam(required = false) Double initialCost,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate initialEffectiveFrom) {
        // Set the initial cost on the rate head for legacy compat
        if (initialCost != null) {
            rateHead.setCost(initialCost);
        }
        RateHead saved = rateHeadService.createRateHead(rateHead);
        // Create the first price entry if provided
        if (initialCost != null && initialEffectiveFrom != null) {
            rateHeadService.addPriceEntry(saved.getId(), initialCost, initialEffectiveFrom);
        }
        return "redirect:/rateheads";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        RateHead rateHead = rateHeadService.getRateHeadById(id);
        if (rateHead == null) {
            return "redirect:/rateheads";
        }
        List<RateHeadPrice> priceHistory = rateHeadService.getPriceHistory(rateHead);
        model.addAttribute("title", "Edit Rate Head");
        model.addAttribute("rateHead", rateHead);
        model.addAttribute("priceHistory", priceHistory);
        model.addAttribute("operationTypes", VendorRole.values());
        return "rateheads/edit";
    }

    @PostMapping("/update/{id}")
    public String updateRateHead(@PathVariable Long id, @ModelAttribute RateHead rateHead, RedirectAttributes redirectAttributes) {
        try {
            rateHeadService.updateRateHead(id, rateHead);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/rateheads";
    }

    @PostMapping("/{id}/prices/add")
    public String addPriceEntry(@PathVariable Long id,
                                @RequestParam Double cost,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
                                RedirectAttributes redirectAttributes) {
        try {
            rateHeadService.addPriceEntry(id, cost, effectiveFrom);
            planService.recalculatePaymentsForRateHead(rateHeadService.getRateHeadById(id));
            redirectAttributes.addFlashAttribute("success", "Price entry added. Affected plan payments recalculated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rateheads/edit/" + id;
    }

    @PostMapping("/{id}/prices/{priceId}/update")
    public String updatePriceEntry(@PathVariable Long id,
                                   @PathVariable Long priceId,
                                   @RequestParam Double cost,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
                                   RedirectAttributes redirectAttributes) {
        try {
            rateHeadService.updatePriceEntry(priceId, cost, effectiveFrom);
            planService.recalculatePaymentsForRateHead(rateHeadService.getRateHeadById(id));
            redirectAttributes.addFlashAttribute("success", "Price entry updated. Affected plan payments recalculated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rateheads/edit/" + id;
    }

    @PostMapping("/{id}/prices/{priceId}/delete")
    public String deletePriceEntry(@PathVariable Long id,
                                   @PathVariable Long priceId,
                                   RedirectAttributes redirectAttributes) {
        try {
            rateHeadService.deletePriceEntry(priceId);
            planService.recalculatePaymentsForRateHead(rateHeadService.getRateHeadById(id));
            redirectAttributes.addFlashAttribute("success", "Price entry deleted. Affected plan payments recalculated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rateheads/edit/" + id;
    }

    @PostMapping("/delete/{id}")
    public String deleteRateHead(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rateHeadService.deleteRateHead(id);
            redirectAttributes.addFlashAttribute("success", "Rate head deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete rate head: " + ex.getMessage());
        }
        return "redirect:/rateheads";
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateRateHead(@PathVariable Long id) {
        rateHeadService.deactivateRateHead(id);
        return "redirect:/rateheads";
    }

    /**
     * Seeds an initial price entry for every rate head that has a cost but no price entries.
     * Uses the provided effective date. Run once after deploying the price history feature
     * to migrate existing rate heads into the new system.
     */
    @PostMapping("/seed-prices")
    public String seedInitialPrices(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
            RedirectAttributes redirectAttributes) {
        int seeded = 0;
        for (RateHead rh : rateHeadService.getAllRateHeads()) {
            if (rh.getCost() != null && rateHeadService.getPriceHistory(rh).isEmpty()) {
                rateHeadService.addPriceEntry(rh.getId(), rh.getCost(), effectiveFrom);
                seeded++;
            }
        }
        if (seeded > 0) {
            redirectAttributes.addFlashAttribute("success",
                    "Seeded price entries for " + seeded + " rate head(s) with effective date " + effectiveFrom + ".");
        } else {
            redirectAttributes.addFlashAttribute("success", "All rate heads already have price entries. Nothing to seed.");
        }
        return "redirect:/rateheads";
    }

    /**
     * Recalculates vendor payments for all plans across all rate heads.
     * Use after seeding price entries or bulk price changes.
     */
    @PostMapping("/recalculate-all")
    public String recalculateAllPayments(RedirectAttributes redirectAttributes) {
        for (RateHead rh : rateHeadService.getAllRateHeads()) {
            planService.recalculatePaymentsForRateHead(rh);
        }
        redirectAttributes.addFlashAttribute("success", "All plan payments recalculated.");
        return "redirect:/rateheads";
    }
}
