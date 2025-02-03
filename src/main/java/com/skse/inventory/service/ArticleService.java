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

    // Add a new article
    public Article addArticle(Article article) {
        return articleRepository.save(article);
    }

    // Get all articles
    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }

    // Get article by Name
    public Optional<Article> getArticleByName(String name) {
        return articleRepository.findByArticleName(name);
    }

    // Update an article
    public Article updateArticle(Long id, Article updatedArticle) {
        Optional<Article> existingArticleOpt = articleRepository.findById(id);

        if (existingArticleOpt.isPresent()) {
            Article existingArticle = existingArticleOpt.get();
            existingArticle.setArticleName(updatedArticle.getArticleName());
            existingArticle.setCuttingCost(updatedArticle.getCuttingCost());
            existingArticle.setPrintingCost(updatedArticle.getPrintingCost());
            existingArticle.setStitchingCost(updatedArticle.getStitchingCost());
            existingArticle.setSlipperCost(updatedArticle.getSlipperCost());

            return articleRepository.save(existingArticle);
        } else {
            throw new RuntimeException("Article not found with ID: " + id);
        }
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
