package com.skse.inventory.repository;

import com.skse.inventory.model.UpperStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UpperStockRepository extends JpaRepository<UpperStock, Long> {
    boolean existsByArticle_Id(Long articleId);

    @Query("""
            SELECT u FROM UpperStock u
            JOIN u.article a
            WHERE lower(trim(a.name)) = lower(trim(:articleName))
              AND u.size = :size
              AND lower(trim(u.color)) = lower(trim(:color))
            ORDER BY u.id ASC
            """)
    List<UpperStock> findAllByArticleNameAndSizeAndColor(
            @Param("articleName") String articleName,
            @Param("size") String size,
            @Param("color") String color);

    /**
     * Returns the first matching UpperStock (oldest by id).
     * Custom @Query ignores the findFirst prefix, so we wrap a List query manually.
     */
    default Optional<UpperStock> findFirstByArticleNameAndSizeAndColorOrderByIdAsc(
            String articleName, String size, String color) {
        List<UpperStock> results = findAllByArticleNameAndSizeAndColor(articleName, size, color);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Query("SELECT u.size AS size, u.color AS color, SUM(u.quantity) AS totalQuantity FROM UpperStock u GROUP BY u.size, u.color")
    List<Object[]> getUpperStockSummary();
}
