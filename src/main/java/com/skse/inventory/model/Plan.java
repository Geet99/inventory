package com.skse.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;

@Entity
@Data
public class Plan {
    private String planNumber; // Unique plan identifier
    private String articleName;
    private String color;
    private String planSize;
    private int total;

    @Column(length = 1000) // Increase column size to handle larger pairs
    private String sizeQuantityPairs; // Example: "38:50, 39:30"

    private String cuttingVendor;
    private String printingVendor;
    private String stitchingVendor;

    @Enumerated(EnumType.STRING)
    private PlanStatus status;

    private LocalDate createDate;
    private LocalDate cuttingStartDate;
    private LocalDate cuttingEndDate;
    private LocalDate printingStartDate;
    private LocalDate printingEndDate;
    private LocalDate stitchingStartDate;
    private LocalDate stitchingEndDate;
    private LocalDate machineStartDate;
    private LocalDate machineEndDate;

    @Column(length = 2000)
    private String machineBatchLogs;
}
