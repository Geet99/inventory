package com.skse.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Entity
@Data
public class VendorMonthlyPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;
    
    // Format: "MMyyyy" e.g., "012025" for January 2025
    private String monthYear;
    
    @Enumerated(EnumType.STRING)
    private VendorRole operationType;
    
    // Total amount due for this month
    private double totalDue;
    
    // Amount already paid
    private double paidAmount;
    
    // Status: PENDING, PARTIAL, PAID
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    // When this month's account was created
    private LocalDate createdDate;
    
    // When payment was last updated
    private LocalDate lastUpdatedDate;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Vendor getVendor() {
        return vendor;
    }
    
    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }
    
    public String getMonthYear() {
        return monthYear;
    }
    
    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }
    
    public VendorRole getOperationType() {
        return operationType;
    }
    
    public void setOperationType(VendorRole operationType) {
        this.operationType = operationType;
    }
    
    public double getTotalDue() {
        return totalDue;
    }
    
    public void setTotalDue(double totalDue) {
        this.totalDue = totalDue;
    }
    
    public double getPaidAmount() {
        return paidAmount;
    }
    
    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
    
    public LocalDate getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }
    
    public LocalDate getLastUpdatedDate() {
        return lastUpdatedDate;
    }
    
    public void setLastUpdatedDate(LocalDate lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }
    
    // Helper method to get balance
    public double getBalance() {
        return totalDue - paidAmount;
    }
    
    // Helper method to format month year for display
    public String getFormattedMonthYear() {
        if (monthYear != null && monthYear.length() == 6) {
            String month = monthYear.substring(0, 2);
            String year = monthYear.substring(2);
            return getMonthName(month) + " " + year;
        }
        return monthYear;
    }
    
    private String getMonthName(String month) {
        return switch (month) {
            case "01" -> "January";
            case "02" -> "February";
            case "03" -> "March";
            case "04" -> "April";
            case "05" -> "May";
            case "06" -> "June";
            case "07" -> "July";
            case "08" -> "August";
            case "09" -> "September";
            case "10" -> "October";
            case "11" -> "November";
            case "12" -> "December";
            default -> month;
        };
    }
    
    // Static helper to generate monthYear string from date
    public static String getMonthYearString(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMyyyy");
        return date.format(formatter);
    }
    
    // Static helper to get current month year
    public static String getCurrentMonthYear() {
        return getMonthYearString(LocalDate.now());
    }
    
    // Static helper to get previous month year
    public static String getPreviousMonthYear() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        return String.format("%02d%d", lastMonth.getMonthValue(), lastMonth.getYear());
    }
}

