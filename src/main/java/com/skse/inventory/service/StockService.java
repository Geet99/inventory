package com.skse.inventory.service;

import com.skse.inventory.model.*;
import com.skse.inventory.repository.FinishedStockRepository;
import com.skse.inventory.repository.UpperStockRepository;
import com.skse.inventory.repository.StockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class StockService {
    
    @Autowired
    private UpperStockRepository upperStockRepository;
    
    @Autowired
    private FinishedStockRepository finishedStockRepository;
    
    @Autowired
    private StockMovementRepository stockMovementRepository;

    public List<UpperStock> getAllUpperStock() {
        return upperStockRepository.findAll();
    }

    public List<FinishedStock> getAllFinishedStock() {
        return finishedStockRepository.findAll();
    }

    public UpperStock getUpperStockByArticleAndSizeAndColor(String articleName, String size, String color) {
        Optional<UpperStock> stock = upperStockRepository.findByArticleNameAndSizeAndColor(articleName, size, color);
        return stock.orElse(null);
    }

    public FinishedStock getFinishedStockByArticleAndSizeAndColor(String articleName, String size, String color) {
        Optional<FinishedStock> stock = finishedStockRepository.findByArticleNameAndSizeAndColor(articleName, size, color);
        return stock.orElse(null);
    }

    public void addToUpperStock(Article article, String size, String color, int quantity) {
        Optional<UpperStock> existingStock = upperStockRepository.findByArticleNameAndSizeAndColor(
            article.getName(), size, color);
        
        if (existingStock.isPresent()) {
            UpperStock stock = existingStock.get();
            stock.setQuantity(stock.getQuantity() + quantity);
            upperStockRepository.save(stock);
        } else {
            UpperStock newStock = new UpperStock();
            newStock.setArticle(article);
            newStock.setSize(size);
            newStock.setColor(color);
            newStock.setQuantity(quantity);
            upperStockRepository.save(newStock);
        }
    }

    public void addToFinishedStock(Article article, String size, String color, int quantity) {
        Optional<FinishedStock> existingStock = finishedStockRepository.findByArticleNameAndSizeAndColor(
            article.getName(), size, color);
        
        if (existingStock.isPresent()) {
            FinishedStock stock = existingStock.get();
            stock.setQuantity(stock.getQuantity() + quantity);
            finishedStockRepository.save(stock);
        } else {
            FinishedStock newStock = new FinishedStock();
            newStock.setArticle(article);
            newStock.setSize(size);
            newStock.setColor(color);
            newStock.setQuantity(quantity);
            finishedStockRepository.save(newStock);
        }
    }

    public void moveFromUpperToFinished(Article article, String size, String color, int quantity) {
        // Reduce upper stock
        Optional<UpperStock> upperStock = upperStockRepository.findByArticleNameAndSizeAndColor(
            article.getName(), size, color);
        
        if (upperStock.isPresent() && upperStock.get().getQuantity() >= quantity) {
            UpperStock stock = upperStock.get();
            stock.setQuantity(stock.getQuantity() - quantity);
            upperStockRepository.save(stock);
            
            // Add to finished stock
            addToFinishedStock(article, size, color, quantity);
            
            // Record movement
            StockMovementRequest movement = new StockMovementRequest();
            movement.setArticleName(article.getName());
            movement.setColor(color);
            movement.setSize(size);
            movement.setQuantity(quantity);
            movement.setMovementDate(LocalDate.now());
            movement.setMovementType("UPPER_TO_FINISHED");
            stockMovementRepository.save(movement);
        } else {
            throw new IllegalStateException("Insufficient upper stock for article: " + article.getName() + 
                ", size: " + size + ", color: " + color);
        }
    }

    public List<StockMovementRequest> getStockMovements(LocalDate startDate, LocalDate endDate) {
        return stockMovementRepository.findByMovementDateBetween(startDate, endDate);
    }

    public List<StockMovementRequest> getStockMovementsByType(String movementType) {
        return stockMovementRepository.findByMovementType(movementType);
    }

    public double getTotalUpperStockValue() {
        List<UpperStock> upperStocks = getAllUpperStock();
        return upperStocks.stream()
            .mapToDouble(stock -> stock.getQuantity() * stock.getArticle().getSlipperCost())
            .sum();
    }

    public double getTotalFinishedStockValue() {
        List<FinishedStock> finishedStocks = getAllFinishedStock();
        return finishedStocks.stream()
            .mapToDouble(stock -> stock.getQuantity() * stock.getArticle().getSlipperCost())
            .sum();
    }
}
