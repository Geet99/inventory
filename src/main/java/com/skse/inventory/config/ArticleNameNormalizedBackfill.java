package com.skse.inventory.config;

import com.skse.inventory.model.Article;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fills {@code name_normalized} for legacy rows. Uses native updates so {@code @PreUpdate} does not
 * overwrite assigned values. When several rows share the same case-insensitive name, the lowest id
 * keeps the canonical key; others get a {@code __dup__} plus id suffix so the unique constraint holds
 * and the application can start (log warns so operators can merge or rename).
 */
@Component
@Order(1)
public class ArticleNameNormalizedBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ArticleNameNormalizedBackfill.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        @SuppressWarnings("unchecked")
        List<String> existingKeys = entityManager
                .createNativeQuery("select name_normalized from article where name_normalized is not null")
                .getResultList();
        Set<String> used = new HashSet<>();
        for (String k : existingKeys) {
            if (k != null && !k.isBlank()) {
                used.add(k);
            }
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager
                .createNativeQuery(
                        "select id, name, name_normalized from article where name is not null order by id")
                .getResultList();

        int updated = 0;
        for (Object[] row : rows) {
            Long id = ((Number) row[0]).longValue();
            String name = (String) row[1];
            String currentNorm = row[2] != null ? String.valueOf(row[2]) : null;

            if (currentNorm != null && !currentNorm.isBlank()) {
                continue;
            }

            String nk = Article.normalizeNameKey(name);
            if (nk == null) {
                log.warn("Skipping article id={}: name is blank after trim.", id);
                continue;
            }

            String assign = nk;
            if (used.contains(nk)) {
                assign = nk + "__dup__" + id;
                log.warn(
                        "Article id={} name='{}' conflicts with another row on case-insensitive name; "
                                + "assigned temporary name_normalized='{}'. Rename or merge this article when convenient.",
                        id, name, assign);
            }

            entityManager
                    .createNativeQuery("update article set name_normalized = ? where id = ?")
                    .setParameter(1, assign)
                    .setParameter(2, id)
                    .executeUpdate();
            used.add(assign);
            updated++;
        }

        if (updated > 0) {
            log.info("Backfilled name_normalized for {} article row(s).", updated);
        }
    }
}
