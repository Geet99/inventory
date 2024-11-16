package com.skse.inventory.controller;

import com.skse.inventory.model.Vendor;
import com.skse.inventory.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    @Autowired
    private VendorService vendorService;

    @PostMapping
    public Vendor addVendor(@RequestBody Vendor vendor) {
        return vendorService.addVendor(vendor);
    }

    @GetMapping("/role/{role}")
    public List<Vendor> getVendorsByRole(@PathVariable String role) {
        return vendorService.getVendorsByRole(role);
    }
}
