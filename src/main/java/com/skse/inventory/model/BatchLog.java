package com.skse.inventory.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class BatchLog {
    private String size;
    private int quantity;
    private LocalDate startDate;
    private LocalDate endDate;
}
