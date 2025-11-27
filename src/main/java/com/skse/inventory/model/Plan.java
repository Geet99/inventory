package com.skse.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Entity
@Data
public class Plan {
    @Id
    private String planNumber; // Unique plan identifier
    private String articleName;
    private String color;
    private String planSize;
    private int total;

    @Column(length = 1000) // Increase column size to handle larger pairs
    private String sizeQuantityPairs; // Example: "6:50, 7:30, 8:20" //ToDo make it map

    @ManyToOne
    @JoinColumn(name = "cutting_vendor_id")
    private Vendor cuttingVendor;
    
    private CuttingType cuttingType;
    
    @ManyToOne
    @JoinColumn(name = "printing_vendor_id")
    private Vendor printingVendor;
    
    @ManyToOne
    @JoinColumn(name = "printing_rate_head_id")
    private RateHead printingRateHead;
    
    @ManyToOne
    @JoinColumn(name = "stitching_vendor_id")
    private Vendor stitchingVendor;

    @Enumerated(EnumType.STRING)
    private PlanStatus status;

    private LocalDate createDate;
    private LocalDate cuttingStartDate;
    private LocalDate cuttingEndDate;
    private LocalDate printingStartDate;
    private LocalDate printingEndDate;
    private LocalDate stitchingStartDate;
    private LocalDate stitchingEndDate;
    private LocalDate machineProcessingDate; // When sent to machine

    private double cuttingVendorPaymentDue;
    private double printingVendorPaymentDue;
    private double stitchingVendorPaymentDue;

    public String getPlanNumber() {
        return planNumber;
    }

    public void setPlanNumber(String planNumber) {
        this.planNumber = planNumber;
    }

    public String getArticleName() {
        return articleName;
    }

    public void setArticleName(String articleName) {
        this.articleName = articleName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getPlanSize() {
        return planSize;
    }

    public void setPlanSize(String planSize) {
        this.planSize = planSize;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getSizeQuantityPairs() {
        return sizeQuantityPairs;
    }

    public void setSizeQuantityPairs(String sizeQuantityPairs) {
        this.sizeQuantityPairs = sizeQuantityPairs;
    }

    public Vendor getCuttingVendor() {
        return cuttingVendor;
    }

    public void setCuttingVendor(Vendor cuttingVendor) {
        this.cuttingVendor = cuttingVendor;
    }

    public CuttingType getCuttingType() {
        return cuttingType;
    }

    public void setCuttingType(CuttingType cuttingType) {
        this.cuttingType = cuttingType;
    }

    public Vendor getPrintingVendor() {
        return printingVendor;
    }

    public void setPrintingVendor(Vendor printingVendor) {
        this.printingVendor = printingVendor;
    }

    public RateHead getPrintingRateHead() {
        return printingRateHead;
    }

    public void setPrintingRateHead(RateHead printingRateHead) {
        this.printingRateHead = printingRateHead;
    }

    public Vendor getStitchingVendor() {
        return stitchingVendor;
    }

    public void setStitchingVendor(Vendor stitchingVendor) {
        this.stitchingVendor = stitchingVendor;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }

    public LocalDate getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDate createDate) {
        this.createDate = createDate;
    }

    public LocalDate getCuttingStartDate() {
        return cuttingStartDate;
    }

    public void setCuttingStartDate(LocalDate cuttingStartDate) {
        this.cuttingStartDate = cuttingStartDate;
    }

    public LocalDate getCuttingEndDate() {
        return cuttingEndDate;
    }

    public void setCuttingEndDate(LocalDate cuttingEndDate) {
        this.cuttingEndDate = cuttingEndDate;
    }

    public LocalDate getPrintingStartDate() {
        return printingStartDate;
    }

    public void setPrintingStartDate(LocalDate printingStartDate) {
        this.printingStartDate = printingStartDate;
    }

    public LocalDate getPrintingEndDate() {
        return printingEndDate;
    }

    public void setPrintingEndDate(LocalDate printingEndDate) {
        this.printingEndDate = printingEndDate;
    }

    public LocalDate getStitchingStartDate() {
        return stitchingStartDate;
    }

    public void setStitchingStartDate(LocalDate stitchingStartDate) {
        this.stitchingStartDate = stitchingStartDate;
    }

    public LocalDate getStitchingEndDate() {
        return stitchingEndDate;
    }

    public void setStitchingEndDate(LocalDate stitchingEndDate) {
        this.stitchingEndDate = stitchingEndDate;
    }

    public double getCuttingVendorPaymentDue() {
        return cuttingVendorPaymentDue;
    }

    public void setCuttingVendorPaymentDue(double cuttingVendorPaymentDue) {
        this.cuttingVendorPaymentDue = cuttingVendorPaymentDue;
    }

    public double getPrintingVendorPaymentDue() {
        return printingVendorPaymentDue;
    }

    public void setPrintingVendorPaymentDue(double printingVendorPaymentDue) {
        this.printingVendorPaymentDue = printingVendorPaymentDue;
    }

    public double getStitchingVendorPaymentDue() {
        return stitchingVendorPaymentDue;
    }

    public void setStitchingVendorPaymentDue(double stitchingVendorPaymentDue) {
        this.stitchingVendorPaymentDue = stitchingVendorPaymentDue;
    }


    public LocalDate getMachineProcessingDate() {
        return machineProcessingDate;
    }

    public void setMachineProcessingDate(LocalDate machineProcessingDate) {
        this.machineProcessingDate = machineProcessingDate;
    }
}
