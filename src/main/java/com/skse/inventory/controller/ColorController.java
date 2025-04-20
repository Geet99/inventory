package com.skse.inventory.controller;

import com.skse.inventory.model.Color;
import com.skse.inventory.service.ColorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/colors")
@Tag(name = "Colors", description = "APIs to manage Colors")
public class ColorController {

    @Autowired
    private ColorService colorService;

    @Operation(summary = "Add a new color", description = "Creates a new color in the system.")
    @PostMapping
    public Color addColor(@RequestBody Color color) {
        return colorService.addColor(color);
    }

    @Operation(summary = "Get all colors", description = "Retrieves a list of all colors in the system.")
    @GetMapping
    public List<Color> getAllColors() {
        return colorService.getAllColors();
    }
}
