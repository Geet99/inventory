package com.skse.inventory.controller;

import com.skse.inventory.model.Article;
import com.skse.inventory.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Operation(summary = "Add a new article", description = "Creates a new article in the system.")
    @PostMapping
    public Article addArticle(@RequestBody Article article) {
        return articleService.addArticle(article);
    }

    @Operation(summary = "Get all articles", description = "Retrieves a list of all articles in the system.")
    @GetMapping
    public List<Article> getAllArticles() {
        return articleService.getAllArticles();
    }

    @Operation(summary = "Get article by name", description = "Retrieves an article based on the provided name.")
    @GetMapping("/name/{articleName}")
    public Optional<Article> getArticleByName(@PathVariable String articleName) {
        return articleService.getArticleByName(articleName);
    }

    @Operation(summary = "Update an article", description = "Updates the details of an existing article.")
    @PutMapping("/{id}")
    public Article updateArticle(@PathVariable Long id, @RequestBody Article updatedArticle) {
        return articleService.updateArticle(id, updatedArticle);
    }
}
