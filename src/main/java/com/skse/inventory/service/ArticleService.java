package com.skse.inventory.service;

import com.skse.inventory.model.Article;
import com.skse.inventory.model.Plan;
import com.skse.inventory.model.UpperStock;
import com.skse.inventory.repository.ArticleRepository;
import com.skse.inventory.repository.FinishedStockRepository;
import com.skse.inventory.repository.PlanRepository;
import com.skse.inventory.repository.UpperStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UpperStockRepository upperStockRepository;

    @Autowired
    private FinishedStockRepository finishedStockRepository;

    @Autowired
    private PlanRepository planRepository;

    // Create a new article
    public Article createArticle(Article article) {
        return articleRepository.save(article);
    }

    // Get all articles
    public List<Article> getAllArticles() {
        return articleRepository.findAllByOrderByIdAsc();
    }

    // Search articles by name or description (case-insensitive)
    public List<Article> searchArticles(String q) {
        if (q == null || q.trim().isEmpty()) {
            return articleRepository.findAllByOrderByIdAsc();
        }
        String term = q.trim();
        return articleRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByIdAsc(term, term);
    }

    // Get article by ID
    public Article getArticleById(Long id) {
        Optional<Article> article = articleRepository.findById(id);
        return article.orElse(null);
    }

    // Get article by Name
    public Article getArticleByName(String name) {
        Optional<Article> article = articleRepository.findByName(name);
        return article.orElse(null);
    }

    // Update an article
    public Article updateArticle(Long id, Article updatedArticle) {
        Optional<Article> existingArticleOpt = articleRepository.findById(id);

        if (existingArticleOpt.isPresent()) {
            Article existingArticle = existingArticleOpt.get();
            existingArticle.setName(updatedArticle.getName());
            existingArticle.setDescription(updatedArticle.getDescription());

            // Rate heads: only replace when the update carries a non-null reference (web UI always sets
            // these from IDs; REST clients that omit nested objects must not wipe existing links).
            if (updatedArticle.getCuttingRateHead() != null) {
                existingArticle.setCuttingRateHead(updatedArticle.getCuttingRateHead());
                existingArticle.setCuttingCost(null);
            } else if (existingArticle.getCuttingRateHead() == null) {
                existingArticle.setCuttingCost(updatedArticle.getCuttingCost());
            }
            if (updatedArticle.getPrintingRateHead() != null) {
                existingArticle.setPrintingRateHead(updatedArticle.getPrintingRateHead());
                existingArticle.setPrintingCost(null);
            } else if (existingArticle.getPrintingRateHead() == null) {
                existingArticle.setPrintingCost(updatedArticle.getPrintingCost());
            }
            if (updatedArticle.getStitchingRateHead() != null) {
                existingArticle.setStitchingRateHead(updatedArticle.getStitchingRateHead());
                existingArticle.setStitchingCost(null);
            } else if (existingArticle.getStitchingRateHead() == null) {
                existingArticle.setStitchingCost(updatedArticle.getStitchingCost());
            }

            return articleRepository.save(existingArticle);
        } else {
            return null;
        }
    }

    /**
     * Deletes an article when nothing in the system still depends on it.
     * Upper/finished stock rows reference the article with a non-null FK; plans reference the article by name.
     */
    public void deleteArticle(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found."));
        if (upperStockRepository.existsByArticle_Id(id)) {
            throw new IllegalStateException(
                    "Cannot delete this article: upper stock still exists. Adjust or remove stock first.");
        }
        if (finishedStockRepository.existsByArticle_Id(id)) {
            throw new IllegalStateException(
                    "Cannot delete this article: finished stock still exists. Adjust or remove stock first.");
        }
        if (planRepository.existsByArticleName(article.getName())) {
            throw new IllegalStateException(
                    "Cannot delete this article: one or more plans still use this article. Remove or change those plans first.");
        }
        articleRepository.deleteById(id);
    }

    public void updateUpperStockAfterCompletion(Plan plan) {
        String articleName = plan.getArticleName();
        String size = plan.getPlanSize();
        String color = plan.getColor();
        int quantity = plan.getTotal(); // You can adjust this depending on the size-quantity pairs

        Optional<UpperStock> upperStock = upperStockRepository.findFirstByArticleNameAndSizeAndColorOrderByIdAsc(articleName, size, color);
        if (upperStock.isPresent()) {
            UpperStock stock = upperStock.get();
            stock.setQuantity(stock.getQuantity() + quantity);
            upperStockRepository.save(stock);
        }
    }
}
