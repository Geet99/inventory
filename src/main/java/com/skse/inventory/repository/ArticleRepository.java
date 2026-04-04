package com.skse.inventory.repository;

import com.skse.inventory.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findByName(String name);

    List<Article> findAllByOrderByIdAsc();

    List<Article> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByIdAsc(String name, String description);
}
