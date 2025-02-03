package com.skse.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Vendor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @Enumerated(EnumType.STRING)
    private VendorRole role;

    private String contactInfo;
    private String address;
    private boolean isActive;
    private double paymentDue;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendorOrderHistory> orderHistory;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VendorRole getRole() {
        return role;
    }

    public void setRole(VendorRole role) {
        this.role = role;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public double getPaymentDue() {
        return paymentDue;
    }

    public void setPaymentDue(double paymentDue) {
        this.paymentDue = paymentDue;
    }

    public List<VendorOrderHistory> getOrderHistory() {
        return orderHistory;
    }

    public void setOrderHistory(List<VendorOrderHistory> orderHistory) {
        this.orderHistory = orderHistory;
    }
}