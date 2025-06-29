package com.skse.inventory.service;

import com.skse.inventory.model.Article;
import com.skse.inventory.model.FinishedStock;
import com.skse.inventory.model.Plan;
import com.skse.inventory.model.UpperStock;
import com.skse.inventory.repository.ArticleRepository;
import com.skse.inventory.repository.FinishedStockRepository;
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

    // Create a new article
    public Article createArticle(Article article) {
        return articleRepository.save(article);
    }

    // Get all articles
    public List<Article> getAllArticles() {
        return articleRepository.findAll();
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
            existingArticle.setCuttingCost(updatedArticle.getCuttingCost());
            existingArticle.setPrintingCost(updatedArticle.getPrintingCost());
            existingArticle.setStitchingCost(updatedArticle.getStitchingCost());
            existingArticle.setSlipperCost(updatedArticle.getSlipperCost());

            return articleRepository.save(existingArticle);
        } else {
            return null;
        }
    }

    // Delete an article
    public void deleteArticle(Long id) {
        articleRepository.deleteById(id);
    }

    public void updateUpperStockAfterCompletion(Plan plan) {
        String articleName = plan.getArticleName();
        String size = plan.getPlanSize();
        String color = plan.getColor();
        int quantity = plan.getTotal(); // You can adjust this depending on the size-quantity pairs

        Optional<UpperStock> upperStock = upperStockRepository.findByArticleNameAndSizeAndColor(articleName, size, color);
        if (upperStock.isPresent()) {
            UpperStock stock = upperStock.get();
            stock.setQuantity(stock.getQuantity() + quantity);
            upperStockRepository.save(stock);
        }
    }
}
