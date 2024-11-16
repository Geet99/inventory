package com.skse.inventory.service;

import com.skse.inventory.model.Vendor;
import com.skse.inventory.repository.PlanRepository;
import com.skse.inventory.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VendorService {
    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private PlanRepository planRepository;

    public Vendor addVendor(Vendor vendor) {
        return vendorRepository.save(vendor);
    }

    public List<Vendor> getVendorsByRole(String role) {
        return vendorRepository.findByRole(role);
    }

    public Map<String, Object> getVendorSummary() {
        Map<String, Object> summary = new HashMap<>();

        List<Object[]> cuttingSummary = planRepository.getCuttingVendorSummary();
        List<Object[]> printingSummary = planRepository.getPrintingVendorSummary();
        List<Object[]> stitchingSummary = planRepository.getStitchingVendorSummary();

        summary.put("Cutting", formatVendorSummary(cuttingSummary));
        summary.put("Printing", formatVendorSummary(printingSummary));
        summary.put("Stitching", formatVendorSummary(stitchingSummary));

        return summary;
    }

    private List<Map<String, Object>> formatVendorSummary(List<Object[]> results) {
        List<Map<String, Object>> summaryList = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> summary = new HashMap<>();
            summary.put("vendor", row[0]);
            summary.put("activeOrders", row[1]);
            summary.put("totalPay", row[2]);
            summaryList.add(summary);
        }
        return summaryList;
    }
}
