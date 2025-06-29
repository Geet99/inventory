package com.skse.inventory.repository;

import com.skse.inventory.model.StockMovementRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovementRequest, Long> {
    
    List<StockMovementRequest> findByMovementDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<StockMovementRequest> findByMovementType(String movementType);
    
    @Query("SELECT sm FROM StockMovementRequest sm WHERE sm.articleName = ?1 AND sm.color = ?2 AND sm.size = ?3")
    List<StockMovementRequest> findByArticleAndColorAndSize(String articleName, String color, String size);
} 