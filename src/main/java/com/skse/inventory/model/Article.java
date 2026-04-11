package com.skse.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Locale;

@Entity
@Data
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /**
     * Lowercase ({@link Locale#ROOT}) trimmed form of {@link #name}; unique for case-insensitive name uniqueness.
     */
    /** Always set by {@link #syncNameNormalized()}; nullable in DDL so Hibernate can add the column to existing tables. */
    @Column(name = "name_normalized", unique = true, length = 255)
    private String nameNormalized;
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

    /**
     * Canonical key for comparing article names without case or leading/trailing whitespace.
     */
    public static String normalizeNameKey(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    @PrePersist
    @PreUpdate
    private void syncNameNormalized() {
        String key = normalizeNameKey(this.name);
        if (key == null) {
            throw new IllegalStateException("Article name must be non-blank.");
        }
        this.nameNormalized = key;
    }

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
}
