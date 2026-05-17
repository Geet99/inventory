package com.skse.inventory.dto;

import com.skse.inventory.model.VendorMonthlyPayment;
import com.skse.inventory.model.VendorRole;

/**
 * Aggregated payment totals for a single calendar month (all roles combined).
 */
public class MonthPaymentDueSummary {

    private String monthYear;
    private String formattedMonthYear;
    private double totalDue;
    private double totalPaid;
    private double balance;
    private double cuttingBalance;
    private double printingBalance;
    private double stitchingBalance;

    public static MonthPaymentDueSummary from(VendorMonthlyPayment payment) {
        MonthPaymentDueSummary summary = new MonthPaymentDueSummary();
        summary.monthYear = payment.getMonthYear();
        summary.formattedMonthYear = payment.getFormattedMonthYear();
        summary.addPayment(payment);
        return summary;
    }

    public void addPayment(VendorMonthlyPayment payment) {
        if (monthYear == null) {
            monthYear = payment.getMonthYear();
            formattedMonthYear = payment.getFormattedMonthYear();
        }
        totalDue += payment.getTotalDue();
        totalPaid += payment.getPaidAmount();
        double lineBalance = payment.getBalance();
        balance += lineBalance;
        if (payment.getOperationType() == VendorRole.Cutting) {
            cuttingBalance += lineBalance;
        } else if (payment.getOperationType() == VendorRole.Printing) {
            printingBalance += lineBalance;
        } else if (payment.getOperationType() == VendorRole.Stitching) {
            stitchingBalance += lineBalance;
        }
    }

    public String getMonthYear() {
        return monthYear;
    }

    public String getFormattedMonthYear() {
        return formattedMonthYear;
    }

    public double getTotalDue() {
        return totalDue;
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    public double getBalance() {
        return balance;
    }

    public double getCuttingBalance() {
        return cuttingBalance;
    }

    public double getPrintingBalance() {
        return printingBalance;
    }

    public double getStitchingBalance() {
        return stitchingBalance;
    }

    public boolean hasOutstandingBalance() {
        return balance > 1e-6;
    }
}
