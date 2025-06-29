package com.skse.inventory.model;

import lombok.Data;

@Data
public class VendorAssignmentRequest {
    private VendorRole role;
    private Vendor vendor;

    public VendorRole getRole() {
        return role;
    }

    public void setRole(VendorRole role) {
        this.role = role;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }
}
