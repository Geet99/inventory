package com.skse.inventory.controller;

import com.skse.inventory.model.Color;
import com.skse.inventory.service.ColorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/colors")
public class ColorController {

    @Autowired
    private ColorService colorService;

    @PostMapping
    public Color createColor(@RequestBody Color color) {
        return colorService.addColor(color);
    }

    @GetMapping
    public List<Color> getAllColors() {
        return colorService.getAllColors();
    }
}
