package com.skse.inventory.repository;

import com.skse.inventory.model.Article;
import com.skse.inventory.model.FinishedStock;
import com.skse.inventory.model.UpperStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinishedStockRepository extends JpaRepository<UpperStock, Long> {
    Optional<FinishedStock> findByArticleAndSizeAndColor(Article article, String size, String color);
}
