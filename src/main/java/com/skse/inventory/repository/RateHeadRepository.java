package com.skse.inventory.repository;

import com.skse.inventory.model.RateHead;
import com.skse.inventory.model.VendorRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateHeadRepository extends JpaRepository<RateHead, Long> {
    List<RateHead> findByOperationType(VendorRole operationType);
    List<RateHead> findByOperationTypeAndActiveTrue(VendorRole operationType);
    Optional<RateHead> findByNameAndOperationType(String name, VendorRole operationType);
}

