package com.skse.inventory.config;

import com.skse.inventory.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills {@code name_normalized} for rows created before that column existed. If two articles differ
 * only by case, the unique index will fail until one is renamed manually.
 */
@Component
@Order(1)
public class ArticleNameNormalizedBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ArticleNameNormalizedBackfill.class);

    private final ArticleRepository articleRepository;

    public ArticleNameNormalizedBackfill(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = articleRepository.backfillNameNormalizedFromName();
        if (updated > 0) {
            log.info("Backfilled name_normalized for {} article row(s).", updated);
        }
    }
}
