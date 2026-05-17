package com.skse.inventory.repository;

import com.skse.inventory.model.Article;
import com.skse.inventory.model.FinishedStock;
import com.skse.inventory.model.UpperStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinishedStockRepository extends JpaRepository<FinishedStock, Long> {
    boolean existsByArticle_Id(Long articleId);

    @Query("""
            SELECT f FROM FinishedStock f
            JOIN f.article a
            WHERE lower(trim(a.name)) = lower(trim(:articleName))
              AND f.size = :size
              AND lower(trim(f.color)) = lower(trim(:color))
            ORDER BY f.id ASC
            """)
    Optional<FinishedStock> findFirstByArticleNameAndSizeAndColorOrderByIdAsc(
            @Param("articleName") String articleName,
            @Param("size") String size,
            @Param("color") String color);

    @Query("SELECT f.size, f.color, SUM(f.quantity) FROM FinishedStock f GROUP BY f.size, f.color")
    List<Object[]> getFinishedStockSummary();
}
