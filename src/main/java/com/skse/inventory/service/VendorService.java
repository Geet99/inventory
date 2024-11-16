package com.skse.inventory.service;

import com.skse.inventory.model.Vendor;
import com.skse.inventory.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorService {
    @Autowired
    private VendorRepository vendorRepository;

    public Vendor addVendor(Vendor vendor) {
        return vendorRepository.save(vendor);
    }

    public List<Vendor> getVendorsByRole(String role) {
        return vendorRepository.findByRole(role);
    }
}
