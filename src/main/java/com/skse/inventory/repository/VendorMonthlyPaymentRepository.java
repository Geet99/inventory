package com.skse.inventory.repository;

import com.skse.inventory.model.PaymentStatus;
import com.skse.inventory.model.Vendor;
import com.skse.inventory.model.VendorMonthlyPayment;
import com.skse.inventory.model.VendorRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorMonthlyPaymentRepository extends JpaRepository<VendorMonthlyPayment, Long> {
    
    // Find by vendor and month-year
    Optional<VendorMonthlyPayment> findByVendorAndMonthYearAndOperationType(
        Vendor vendor, String monthYear, VendorRole operationType);
    
    // Find all payments for a vendor
    List<VendorMonthlyPayment> findByVendorOrderByMonthYearDesc(Vendor vendor);
    
    // Find all payments for a specific month across all vendors
    List<VendorMonthlyPayment> findByMonthYear(String monthYear);
    
    // Find all payments for a vendor and operation type
    List<VendorMonthlyPayment> findByVendorAndOperationTypeOrderByMonthYearDesc(
        Vendor vendor, VendorRole operationType);
    
    // Find pending payments for a vendor
    List<VendorMonthlyPayment> findByVendorAndStatusOrderByMonthYearDesc(
        Vendor vendor, PaymentStatus status);
}

