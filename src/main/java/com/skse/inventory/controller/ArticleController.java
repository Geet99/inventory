package com.skse.inventory.controller;

import com.skse.inventory.model.Article;
import com.skse.inventory.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@Tag(name = "Articles", description = "APIs to manage Articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Operation(summary = "Add a new article", description = "Creates a new article in the system.")
    @PostMapping
    public Article createArticle(@RequestBody Article article) {
        return articleService.createArticle(article);
    }

    @Operation(summary = "Get all articles", description = "Retrieves a list of all articles in the system.")
    @GetMapping
    public List<Article> getAllArticles() {
        return articleService.getAllArticles();
    }

    @Operation(summary = "Get article by ID", description = "Retrieves an article based on the provided ID.")
    @GetMapping("/{id}")
    public ResponseEntity<Article> getArticleById(@PathVariable Long id) {
        Article article = articleService.getArticleById(id);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(article);
    }

    @Operation(summary = "Get article by name", description = "Retrieves an article based on the provided name.")
    @GetMapping("/name/{articleName}")
    public ResponseEntity<Article> getArticleByName(@PathVariable String articleName) {
        Article article = articleService.getArticleByName(articleName);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(article);
    }

    @Operation(summary = "Update an article", description = "Updates the details of an existing article.")
    @PutMapping("/{id}")
    public ResponseEntity<Article> updateArticle(@PathVariable Long id, @RequestBody Article updatedArticle) {
        Article article = articleService.updateArticle(id, updatedArticle);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(article);
    }

    @Operation(summary = "Delete an article", description = "Deletes an article from the system.")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.ok("Article deleted successfully");
    }
}
