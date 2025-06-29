package com.skse.inventory.repository;

import com.skse.inventory.model.VendorOrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VendorOrderHistoryRepository extends JpaRepository<VendorOrderHistory, Long> {
    List<VendorOrderHistory> findByOrderDateBefore(LocalDate date);
    
    List<VendorOrderHistory> findByVendorIdOrderByOrderDateDesc(Long vendorId);
    
    List<VendorOrderHistory> findByVendorIdAndOrderDateGreaterThanEqual(Long vendorId, LocalDate startDate);
    
    List<VendorOrderHistory> findByOrderDateBetween(LocalDate startDate, LocalDate endDate);
}
