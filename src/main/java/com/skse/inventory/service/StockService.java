package com.skse.inventory.service;

import com.skse.inventory.model.FinishedStock;
import com.skse.inventory.model.StockMovementRequest;
import com.skse.inventory.model.UpperStock;
import com.skse.inventory.repository.FinishedStockRepository;
import com.skse.inventory.repository.UpperStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StockService {

    @Autowired
    private UpperStockRepository upperStockRepository;

    @Autowired
    private FinishedStockRepository finishedStockRepository;

    public void moveToMachine(StockMovementRequest request) {
        // Find UpperStock entry for the given article, size, and color
        Optional<UpperStock> upperStock = upperStockRepository.findByArticleNameAndSizeAndColor(
                request.getArticleName(), request.getSize(), request.getColor());

        if (upperStock.isPresent() && upperStock.get().getQuantity() >= request.getQuantity()) {
            UpperStock stock = upperStock.get();
            // Deduct the quantity from UpperStock
            stock.setQuantity(stock.getQuantity() - request.getQuantity());
            upperStockRepository.save(stock);

            // Add the quantity to FinishedStock
            FinishedStock finishedStock = new FinishedStock();
            finishedStock.setArticle(stock.getArticle());
            finishedStock.setSize(request.getSize());
            finishedStock.setColor(request.getColor());
            finishedStock.setQuantity(request.getQuantity());
            finishedStockRepository.save(finishedStock);
        } else {
            throw new IllegalStateException("Insufficient UpperStock for the requested quantity.");
        }
    }

    // Deduct stock from FinishedStock when sold
    public void deductSoldStock(StockMovementRequest request) {
        // Find FinishedStock entry for the given article, size, and color
        Optional<FinishedStock> finishedStock = finishedStockRepository.findByArticleNameAndSizeAndColor(
                request.getArticleName(), request.getSize(), request.getColor());

        if (finishedStock.isPresent() && finishedStock.get().getQuantity() >= request.getQuantity()) {
            FinishedStock stock = finishedStock.get();
            // Deduct the quantity from FinishedStock
            stock.setQuantity(stock.getQuantity() - request.getQuantity());
            finishedStockRepository.save(stock);
        } else {
            throw new IllegalStateException("Insufficient FinishedStock for the requested quantity.");
        }
    }

    public List<Map<String, Object>> getUpperStockSummary() {
        return formatStockSummary(upperStockRepository.getUpperStockSummary());
    }

    public List<Map<String, Object>> getFinishedStockSummary() {
        return formatStockSummary(finishedStockRepository.getFinishedStockSummary());
    }

    private List<Map<String, Object>> formatStockSummary(List<Object[]> results) {
        List<Map<String, Object>> summaryList = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> summary = new HashMap<>();
            summary.put("size", row[0]);
            summary.put("color", row[1]);
            summary.put("quantity", row[2]);
            summaryList.add(summary);
        }
        return summaryList;
    }
}
