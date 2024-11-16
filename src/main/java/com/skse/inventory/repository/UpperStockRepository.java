package com.skse.inventory.repository;

import com.skse.inventory.model.Article;
import com.skse.inventory.model.UpperStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UpperStockRepository extends JpaRepository<UpperStock, Long> {
    Optional<UpperStock> findByArticleAndSizeAndColor(Article article, String size, String color);
}
