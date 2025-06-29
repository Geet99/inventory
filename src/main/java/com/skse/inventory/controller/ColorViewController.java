package com.skse.inventory.controller;

import com.skse.inventory.model.Color;
import com.skse.inventory.service.ColorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/colors")
public class ColorViewController {

    @Autowired
    private ColorService colorService;

    @GetMapping
    public String listColors(Model model) {
        model.addAttribute("title", "Colors");
        model.addAttribute("colors", colorService.getAllColors());
        return "colors/list";
    }

    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("title", "Add Color");
        model.addAttribute("color", new Color());
        return "colors/add";
    }

    @PostMapping
    public String saveColor(@ModelAttribute Color color) {
        colorService.addColor(color);
        return "redirect:/colors";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Color color = colorService.getColorById(id);
        if (color == null) {
            return "redirect:/colors";
        }
        model.addAttribute("title", "Edit Color");
        model.addAttribute("color", color);
        return "colors/edit";
    }

    @PostMapping("/update/{id}")
    public String updateColor(@PathVariable Long id, @ModelAttribute Color color) {
        colorService.updateColor(id, color);
        return "redirect:/colors";
    }

    @PostMapping("/delete/{id}")
    public String deleteColor(@PathVariable Long id) {
        colorService.deleteColor(id);
        return "redirect:/colors";
    }
} 