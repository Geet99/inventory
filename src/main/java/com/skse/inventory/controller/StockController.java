package com.skse.inventory.controller;

import com.skse.inventory.model.StockMovementRequest;
import com.skse.inventory.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
@Tag(name = "Stock", description = "APIs to manage Stock")
public class StockController {

    @Autowired
    private StockService stockService;

    @Operation(summary = "Deduct UpperStock and add to FinishedStock when items are put on machine",
            description = "Deducts the specified quantity from UpperStock and adds it to FinishedStock.")
    @PostMapping("/move-to-machine")
    public ResponseEntity<String> moveToMachine(@RequestBody StockMovementRequest request) {
        stockService.moveToMachine(request);
        return ResponseEntity.ok("Stock updated: UpperStock subtracted and FinishedStock added.");
    }

    @Operation(summary = "Deduct stock from FinishedStock when items are sold",
            description = "Allows deducting specified quantity from FinishedStock when items are sold.")
    @PostMapping("/deduct-sold-stock")
    public ResponseEntity<String> deductSoldStock(@RequestBody StockMovementRequest request) {
        stockService.deductSoldStock(request);
        return ResponseEntity.ok("Stock deducted from FinishedStock.");
    }

    @Operation(summary = "Get summary of Upper Stock")
    @GetMapping("/upper-stock")
    public ResponseEntity<List<Map<String, Object>>> getUpperStockSummary() {
        return ResponseEntity.ok(stockService.getUpperStockSummary());
    }

    @Operation(summary = "Get summary of Finished Stock")
    @GetMapping("/finished-stock")
    public ResponseEntity<List<Map<String, Object>>> getFinishedStockSummary() {
        return ResponseEntity.ok(stockService.getFinishedStockSummary());
    }
}
