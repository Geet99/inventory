package com.skse.inventory.controller;

import com.skse.inventory.model.RateHead;
import com.skse.inventory.model.VendorRole;
import com.skse.inventory.service.RateHeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/rateheads")
public class RateHeadViewController {
    
    @Autowired
    private RateHeadService rateHeadService;
    
    @GetMapping
    public String listRateHeads(Model model) {
        model.addAttribute("title", "Rate Heads");
        model.addAttribute("rateHeads", rateHeadService.getAllRateHeads());
        return "rateheads/list";
    }
    
    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("title", "Add Rate Head");
        model.addAttribute("rateHead", new RateHead());
        model.addAttribute("operationTypes", VendorRole.values());
        return "rateheads/add";
    }
    
    @PostMapping
    public String saveRateHead(@ModelAttribute RateHead rateHead) {
        rateHeadService.createRateHead(rateHead);
        return "redirect:/rateheads";
    }
    
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        RateHead rateHead = rateHeadService.getRateHeadById(id);
        if (rateHead == null) {
            return "redirect:/rateheads";
        }
        model.addAttribute("title", "Edit Rate Head");
        model.addAttribute("rateHead", rateHead);
        model.addAttribute("operationTypes", VendorRole.values());
        return "rateheads/edit";
    }
    
    @PostMapping("/update/{id}")
    public String updateRateHead(@PathVariable Long id, @ModelAttribute RateHead rateHead) {
        rateHeadService.updateRateHead(id, rateHead);
        return "redirect:/rateheads";
    }
    
    @PostMapping("/delete/{id}")
    public String deleteRateHead(@PathVariable Long id) {
        rateHeadService.deleteRateHead(id);
        return "redirect:/rateheads";
    }
    
    @PostMapping("/deactivate/{id}")
    public String deactivateRateHead(@PathVariable Long id) {
        rateHeadService.deactivateRateHead(id);
        return "redirect:/rateheads";
    }
    
    @GetMapping("/operation/{operationType}")
    public String listRateHeadsByOperation(@PathVariable VendorRole operationType, Model model) {
        List<RateHead> rateHeads = rateHeadService.getRateHeadsByOperationType(operationType);
        model.addAttribute("rateHeads", rateHeads);
        model.addAttribute("operationType", operationType);
        model.addAttribute("title", operationType + " Rate Heads");
        return "rateheads/by-operation";
    }
}

