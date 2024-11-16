package com.skse.inventory.service;

import com.skse.inventory.model.Article;
import com.skse.inventory.model.FinishedStock;
import com.skse.inventory.model.Plan;
import com.skse.inventory.model.UpperStock;
import com.skse.inventory.repository.ArticleRepository;
import com.skse.inventory.repository.UpperStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UpperStockRepository upperStockRepository;

    @Autowired
    private FinishedStockRepository finishedStockRepository;

    public Article addSlipperType(Article article) {
        return articleRepository.save(article);
    }

    public List<Article> getAllSlipperTypes() {
        return articleRepository.findAll();
    }

    public void addUpperStockFromPlan(Plan plan) {
        Article article = articleRepository.findByArticleName(plan.getArticleName())
                .orElseThrow(() -> new IllegalArgumentException("Article not found: " + plan.getSlipperType()));

        String[] sizeQuantityPairs = plan.getSizeQuantityPairs().split(",");
        String color = plan.getColor();

        for (String pair : sizeQuantityPairs) {
            String[] sizeAndQuantity = pair.split(":");
            String size = sizeAndQuantity[0];
            int quantity = Integer.parseInt(sizeAndQuantity[1]);

            // Find existing stock for the size and color
            UpperStock stock = upperStockRepository.findByArticleAndSizeAndColor(article, size, color)
                    .orElseGet(() -> {
                        UpperStock newStock = new UpperStock();
                        newStock.setArticle(article);
                        newStock.setSize(size);
                        newStock.setColor(color);
                        newStock.setQuantity(0);
                        return newStock;
                    });

            // Update the quantity
            stock.setQuantity(stock.getQuantity() + quantity);
            upperStockRepository.save(stock);
        }
    }

    public void processBatchToFinishedStock(Plan plan, String processedPairs) {
        Article article = articleRepository.findByArticleName(plan.getArticleName())
                .orElseThrow(() -> new IllegalArgumentException("SlipperType not found: " + plan.getSlipperType()));

        String[] sizeQuantityPairs = processedPairs.split(",");
        String color = plan.getColor();

        for (String pair : sizeQuantityPairs) {
            String[] sizeAndQuantity = pair.split(":");
            String size = sizeAndQuantity[0];
            int quantity = Integer.parseInt(sizeAndQuantity[1]);

            // Deduct from UpperStock
            UpperStock upperStock = upperStockRepository.findBySlipperTypeAndSizeAndColor(slipperType, size, color)
                    .orElseThrow(() -> new IllegalArgumentException("UpperStock not found for size " + size + " and color " + color));
            if (upperStock.getQuantity() < quantity) {
                throw new IllegalStateException("Not enough stock available in UpperStock for size " + size);
            }
            upperStock.setQuantity(upperStock.getQuantity() - quantity);
            upperStockRepository.save(upperStock);

            // Add to FinishedStock
            FinishedStock finishedStock = finishedStockRepository.findBySlipperTypeAndSizeAndColor(slipperType, size, color)
                    .orElseGet(() -> {
                        FinishedStock newStock = new FinishedStock();
                        newStock.setArticle(article);
                        newStock.setSize(size);
                        newStock.setColor(color);
                        newStock.setQuantity(0);
                        return newStock;
                    });

            finishedStock.setQuantity(finishedStock.getQuantity() + quantity);
            finishedStockRepository.save(finishedStock);
        }
    }
}
