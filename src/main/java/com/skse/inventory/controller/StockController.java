package com.skse.inventory.controller;

import com.skse.inventory.model.*;
import com.skse.inventory.service.StockService;
import com.skse.inventory.service.ArticleService;
import com.skse.inventory.service.ColorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/stock")
@Tag(name = "Stock Management", description = "APIs to manage stock operations")
public class StockController {

    @Autowired
    private StockService stockService;
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private ColorService colorService;

    @GetMapping
    public String stockDashboard(Model model) {
        List<UpperStock> upperStocks = stockService.getAllUpperStock();
        List<FinishedStock> finishedStocks = stockService.getAllFinishedStock();
        
        double totalUpperValue = stockService.getTotalUpperStockValue();
        double totalFinishedValue = stockService.getTotalFinishedStockValue();
        
        model.addAttribute("upperStocks", upperStocks);
        model.addAttribute("finishedStocks", finishedStocks);
        model.addAttribute("totalUpperValue", totalUpperValue);
        model.addAttribute("totalFinishedValue", totalFinishedValue);
        
        return "stock/dashboard";
    }

    @GetMapping("/upper")
    public String upperStockList(Model model) {
        List<UpperStock> upperStocks = stockService.getAllUpperStock();
        model.addAttribute("upperStocks", upperStocks);
        return "stock/upper-list";
    }

    @GetMapping("/finished")
    public String finishedStockList(Model model) {
        List<FinishedStock> finishedStocks = stockService.getAllFinishedStock();
        model.addAttribute("finishedStocks", finishedStocks);
        return "stock/finished-list";
    }

    @GetMapping("/movements")
    public String stockMovements(Model model, 
                                @RequestParam(required = false) String startDate,
                                @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        List<StockMovementRequest> movements = stockService.getStockMovements(start, end);
        model.addAttribute("movements", movements);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        
        return "stock/movements";
    }

    @PostMapping("/move-to-machine")
    @ResponseBody
    public ResponseEntity<Map<String, String>> moveToMachine(@RequestBody Map<String, Object> request) {
        try {
            String articleName = (String) request.get("articleName");
            String size = (String) request.get("size");
            String color = (String) request.get("color");
            Integer quantity = (Integer) request.get("quantity");
            
            Article article = articleService.getArticleByName(articleName);
            if (article == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Article not found"));
            }
            
            stockService.moveFromUpperToFinished(article, size, color, quantity);
            
            return ResponseEntity.ok(Map.of("message", "Stock moved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/add-upper")
    @ResponseBody
    public ResponseEntity<Map<String, String>> addUpperStock(@RequestBody Map<String, Object> request) {
        try {
            String articleName = (String) request.get("articleName");
            String size = (String) request.get("size");
            String color = (String) request.get("color");
            Integer quantity = (Integer) request.get("quantity");
            
            Article article = articleService.getArticleByName(articleName);
            if (article == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Article not found"));
            }
            
            stockService.addToUpperStock(article, size, color, quantity);
            
            return ResponseEntity.ok(Map.of("message", "Upper stock added successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/add-finished")
    @ResponseBody
    public ResponseEntity<Map<String, String>> addFinishedStock(@RequestBody Map<String, Object> request) {
        try {
            String articleName = (String) request.get("articleName");
            String size = (String) request.get("size");
            String color = (String) request.get("color");
            Integer quantity = (Integer) request.get("quantity");
            
            Article article = articleService.getArticleByName(articleName);
            if (article == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Article not found"));
            }
            
            stockService.addToFinishedStock(article, size, color, quantity);
            
            return ResponseEntity.ok(Map.of("message", "Finished stock added successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/upper")
    @ResponseBody
    public ResponseEntity<List<UpperStock>> getUpperStockApi() {
        return ResponseEntity.ok(stockService.getAllUpperStock());
    }

    @GetMapping("/api/finished")
    @ResponseBody
    public ResponseEntity<List<FinishedStock>> getFinishedStockApi() {
        return ResponseEntity.ok(stockService.getAllFinishedStock());
    }

    @GetMapping("/api/summary")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStockSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalUpperValue", stockService.getTotalUpperStockValue());
        summary.put("totalFinishedValue", stockService.getTotalFinishedStockValue());
        summary.put("upperStockCount", stockService.getAllUpperStock().size());
        summary.put("finishedStockCount", stockService.getAllFinishedStock().size());
        
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/move-to-finished")
    public String showMoveToFinishedForm(Model model) {
        model.addAttribute("upperStock", stockService.getAllUpperStock());
        model.addAttribute("articles", articleService.getAllArticles());
        model.addAttribute("colors", colorService.getAllColors());
        return "stock/move-to-finished";
    }
    
    @PostMapping("/move-to-finished")
    public String moveToFinished(@RequestParam String articleName,
                                 @RequestParam String sizeQuantityPairs,
                                 @RequestParam String color,
                                 Model model) {
        try {
            Article article = articleService.getArticleByName(articleName);
            if (article == null) {
                model.addAttribute("error", "Article not found");
                model.addAttribute("upperStock", stockService.getAllUpperStock());
                model.addAttribute("articles", articleService.getAllArticles());
                model.addAttribute("colors", colorService.getAllColors());
                return "stock/move-to-finished";
            }
            
            // Parse size:quantity pairs (e.g., "6:50, 7:30, 8:20")
            String[] pairs = sizeQuantityPairs.split(",");
            int totalMoved = 0;
            
            for (String pair : pairs) {
                String[] parts = pair.trim().split(":");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid format. Use size:quantity (e.g., 6:50, 7:30)");
                }
                
                String size = parts[0].trim();
                int quantity = Integer.parseInt(parts[1].trim());
                
                stockService.moveFromUpperToFinished(article, size, color, quantity);
                totalMoved += quantity;
            }
            
            model.addAttribute("success", 
                String.format("Successfully moved %d units of %s (%s) to finished stock", 
                    totalMoved, articleName, color));
        } catch (NumberFormatException e) {
            model.addAttribute("error", "Invalid quantity format. Please use numbers only.");
        } catch (Exception e) {
            model.addAttribute("error", "Failed to move stock: " + e.getMessage());
        }
        
        model.addAttribute("upperStock", stockService.getAllUpperStock());
        model.addAttribute("articles", articleService.getAllArticles());
        model.addAttribute("colors", colorService.getAllColors());
        return "stock/move-to-finished";
    }
}
