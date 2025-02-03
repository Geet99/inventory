package com.skse.inventory.repository;

import com.skse.inventory.model.Article;
import com.skse.inventory.model.FinishedStock;
import com.skse.inventory.model.UpperStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinishedStockRepository extends JpaRepository<FinishedStock, Long> {
    Optional<FinishedStock> findByArticleNameAndSizeAndColor(String articleName, String size, String color);

    @Query("SELECT f.size, f.color, SUM(f.quantity) FROM FinishedStock f GROUP BY f.size, f.color")
    List<Object[]> getFinishedStockSummary();
}
