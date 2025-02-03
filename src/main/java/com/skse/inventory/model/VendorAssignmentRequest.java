package com.skse.inventory.model;

import lombok.Data;

@Data
public class VendorAssignmentRequest {
    private VendorRole role;
    private String vendorId;

    public VendorRole getRole() {
        return role;
    }

    public void setRole(VendorRole role) {
        this.role = role;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }
}
