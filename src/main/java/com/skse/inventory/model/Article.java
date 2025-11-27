package com.skse.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    
    // Rate Head references - centralized cost management
    @ManyToOne
    @JoinColumn(name = "cutting_rate_head_id")
    private RateHead cuttingRateHead;
    
    @ManyToOne
    @JoinColumn(name = "printing_rate_head_id")
    private RateHead printingRateHead;
    
    @ManyToOne
    @JoinColumn(name = "stitching_rate_head_id")
    private RateHead stitchingRateHead;
    
    // Legacy cost fields - kept for backward compatibility
    private Double cuttingCost;
    private Double printingCost;
    private Double stitchingCost;
    
    private Double slipperCost;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RateHead getCuttingRateHead() {
        return cuttingRateHead;
    }

    public void setCuttingRateHead(RateHead cuttingRateHead) {
        this.cuttingRateHead = cuttingRateHead;
    }

    public RateHead getPrintingRateHead() {
        return printingRateHead;
    }

    public void setPrintingRateHead(RateHead printingRateHead) {
        this.printingRateHead = printingRateHead;
    }

    public RateHead getStitchingRateHead() {
        return stitchingRateHead;
    }

    public void setStitchingRateHead(RateHead stitchingRateHead) {
        this.stitchingRateHead = stitchingRateHead;
    }

    // Get current costs from rate heads (or fallback to legacy costs)
    public Double getCuttingCost() {
        if (cuttingRateHead != null) {
            return cuttingRateHead.getCost();
        }
        return cuttingCost;
    }

    public void setCuttingCost(Double cuttingCost) {
        this.cuttingCost = cuttingCost;
    }

    public Double getPrintingCost() {
        if (printingRateHead != null) {
            return printingRateHead.getCost();
        }
        return printingCost;
    }

    public void setPrintingCost(Double printingCost) {
        this.printingCost = printingCost;
    }

    public Double getStitchingCost() {
        if (stitchingRateHead != null) {
            return stitchingRateHead.getCost();
        }
        return stitchingCost;
    }

    public void setStitchingCost(Double stitchingCost) {
        this.stitchingCost = stitchingCost;
    }

    public Double getSlipperCost() {
        return slipperCost;
    }

    public void setSlipperCost(Double slipperCost) {
        this.slipperCost = slipperCost;
    }
}
