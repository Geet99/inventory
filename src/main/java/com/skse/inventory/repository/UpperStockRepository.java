package com.skse.inventory.repository;

import com.skse.inventory.model.UpperStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UpperStockRepository extends JpaRepository<UpperStock, Long> {
    Optional<UpperStock> findByArticleNameAndSizeAndColor(String articleName, String size, String color);

    @Query("SELECT u.size AS size, u.color AS color, SUM(u.quantity) AS totalQuantity FROM UpperStock u GROUP BY u.size, u.color")
    List<Object[]> getUpperStockSummary();
}
