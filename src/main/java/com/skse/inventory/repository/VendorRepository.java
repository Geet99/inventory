package com.skse.inventory.repository;

import com.skse.inventory.model.Vendor;
import com.skse.inventory.model.VendorRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    
    List<Vendor> findByRole(VendorRole role);
    
    List<Vendor> findByIsActiveTrue();
    
    List<Vendor> findByIsActive(boolean isActive);
}
