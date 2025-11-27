package com.skse.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class RateHead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @Enumerated(EnumType.STRING)
    private VendorRole operationType; // Cutting, Printing, or Stitching
    
    private Double cost;
    
    private boolean active = true;
    
    // Getters and Setters
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
    
    public VendorRole getOperationType() {
        return operationType;
    }
    
    public void setOperationType(VendorRole operationType) {
        this.operationType = operationType;
    }
    
    public Double getCost() {
        return cost;
    }
    
    public void setCost(Double cost) {
        this.cost = cost;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
}

