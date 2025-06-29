package com.skse.inventory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skse.inventory.model.*;
import com.skse.inventory.repository.ArticleRepository;
import com.skse.inventory.repository.PlanRepository;
import com.skse.inventory.repository.UpperStockRepository;
import com.skse.inventory.repository.StockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PlanService {
    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UpperStockRepository upperStockRepository;
    
    @Autowired
    private StockMovementRepository stockMovementRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Plan createPlan(Plan plan) {
        plan.setCreateDate(LocalDate.now());
        plan.setStatus(PlanStatus.Pending_Cutting); // Initial status
        return planRepository.save(plan);
    }

    public Plan updatePlan(String planNumber, Plan updatedPlan) {
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan != null) {
            plan.setTotal(updatedPlan.getTotal());
            plan.setSizeQuantityPairs(updatedPlan.getSizeQuantityPairs());
            return planRepository.save(plan);
        } else {
            throw new IllegalArgumentException("Plan not found: " + planNumber);
        }
    }

    public void deletePlan(String planNumber) {
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan != null) {
            planRepository.delete(plan);
        } else {
            throw new IllegalArgumentException("Plan not found: " + planNumber);
        }
    }

    public Plan moveToNextState(String planNumber) {
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found with number: " + planNumber);
        }

        PlanStatus nextStatus = getNextStatus(plan.getStatus());
        if (nextStatus == null) {
            throw new IllegalStateException("Invalid status transition");
        }

        // Transition logic (timestamps and stock update)
        switch (nextStatus) {
            case Cutting:
                plan.setCuttingStartDate(LocalDate.now());
                break;
            case Pending_Printing:
                plan.setCuttingEndDate(LocalDate.now());
                plan.setCuttingVendorPaymentDue(calculatePayment(plan, VendorRole.Cutting));
                updateUpperStockFromPlan(plan);
                break;
            case Printing:
                plan.setPrintingStartDate(LocalDate.now());
                break;
            case Pending_Stitching:
                plan.setPrintingEndDate(LocalDate.now());
                plan.setPrintingVendorPaymentDue(calculatePayment(plan, VendorRole.Printing));
                break;
            case Stitching:
                plan.setStitchingStartDate(LocalDate.now());
                break;
            case Completed:
                plan.setStitchingEndDate(LocalDate.now());
                plan.setStitchingVendorPaymentDue(calculatePayment(plan, VendorRole.Stitching));
                break;
        }

        plan.setStatus(nextStatus);
        return planRepository.save(plan);
    }

    public Plan sendToMachine(String planNumber, int finalQuantity) {
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found with number: " + planNumber);
        }

        if (plan.getStatus() != PlanStatus.Completed) {
            throw new IllegalStateException("Plan must be completed before sending to machine");
        }

        plan.setFinalQuantity(finalQuantity);
        plan.setMachineProcessingDate(LocalDate.now());
        
        // Move stock from upper to finished
        moveStockFromUpperToFinished(plan, finalQuantity);
        
        return planRepository.save(plan);
    }

    private void moveStockFromUpperToFinished(Plan plan, int finalQuantity) {
        String[] sizeQuantityPairs = plan.getSizeQuantityPairs().split(",");
        for (String pair : sizeQuantityPairs) {
            String[] sizeQuantity = pair.trim().split(":");
            String size = sizeQuantity[0].trim();
            int plannedQuantity = Integer.parseInt(sizeQuantity[1].trim());
            
            // Calculate proportional final quantity for this size
            double proportion = (double) plannedQuantity / plan.getTotal();
            int finalSizeQuantity = (int) Math.round(finalQuantity * proportion);
            
            // Reduce upper stock
            Optional<UpperStock> upperStockOpt = upperStockRepository.findByArticleNameAndSizeAndColor(
                plan.getArticleName(), size, plan.getColor());
            
            if (upperStockOpt.isPresent()) {
                UpperStock upperStock = upperStockOpt.get();
                upperStock.setQuantity(upperStock.getQuantity() - finalSizeQuantity);
                upperStockRepository.save(upperStock);
                
                // Record stock movement
                StockMovementRequest movement = new StockMovementRequest();
                movement.setPlan(plan);
                movement.setArticleName(plan.getArticleName());
                movement.setColor(plan.getColor());
                movement.setSize(size);
                movement.setQuantity(finalSizeQuantity);
                movement.setMovementDate(LocalDate.now());
                movement.setMovementType("UPPER_TO_FINISHED");
                stockMovementRepository.save(movement);
            }
        }
    }

    private PlanStatus getNextStatus(PlanStatus currentStatus) {
        return switch (currentStatus) {
            case Pending_Cutting -> PlanStatus.Cutting;
            case Cutting -> PlanStatus.Pending_Printing;
            case Pending_Printing -> PlanStatus.Printing;
            case Printing -> PlanStatus.Pending_Stitching;
            case Pending_Stitching -> PlanStatus.Stitching;
            case Stitching -> PlanStatus.Completed;
            default -> null;
        };
    }

    private double calculatePayment(Plan plan, VendorRole roleType) {
        Article article = articleRepository.findByName(plan.getArticleName()).get();
        int totalQuantity = plan.getTotal();
        double cost = switch (roleType) {
            case Cutting -> article.getCuttingCost();
            case Printing -> article.getPrintingCost();
            case Stitching -> article.getStitchingCost();
            default -> 0.0;
        };

        return cost * totalQuantity;
    }

    public Map<String, Integer> getActiveOrdersByState() {
        Map<String, Integer> activeOrders = new HashMap<>();

        // Get the raw results from the repository
        List<Object[]> results = planRepository.getActiveOrdersByState();

        // Process each result and add to the map
        for (Object[] result : results) {
            String status = (String) result[0]; // Get status from the first element of the array
            Long count = (Long) result[1];       // Get count from the second element of the array
            activeOrders.put(status, count.intValue()); // Add to map (convert Long to int)
        }

        return activeOrders;
    }

    private void updateUpperStockFromPlan(Plan plan) {
        String[] sizeQuantityPairs = plan.getSizeQuantityPairs().split(",");
        for (String pair : sizeQuantityPairs) {
            String[] sizeQuantity = pair.trim().split(":");
            String size = sizeQuantity[0].trim();
            int quantity = Integer.parseInt(sizeQuantity[1].trim());

            // Update upper stock and finished stock as per size and color
            Optional<UpperStock> upperStock = upperStockRepository.findByArticleNameAndSizeAndColor(plan.getArticleName(), size, plan.getColor());
            UpperStock stock = null;
            if (upperStock.isPresent()) {
                stock = upperStock.get();
                stock.setQuantity(stock.getQuantity() + quantity);
            } else {
                stock = new UpperStock();
                Article article = articleRepository.findByName(plan.getArticleName()).get();
                stock.setArticle(article);
                stock.setSize(size);
                stock.setColor(plan.getColor());
                stock.setQuantity(quantity); // Initialize with quantity from the plan
            }
            upperStockRepository.save(stock);
        }
    }

    public void assignVendorToPlan(String planNumber, VendorAssignmentRequest vendorAssignmentRequest) {
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found with number: " + planNumber);
        }

        // Assign the vendor based on the role
        switch (vendorAssignmentRequest.getRole()) {
            case Cutting:
                plan.setCuttingVendor(vendorAssignmentRequest.getVendor());
                break;
            case Printing:
                plan.setPrintingVendor(vendorAssignmentRequest.getVendor());
                break;
            case Stitching:
                plan.setStitchingVendor(vendorAssignmentRequest.getVendor());
                break;
            default:
                throw new IllegalArgumentException("Invalid vendor role: " + vendorAssignmentRequest.getRole());
        }

        planRepository.save(plan);
    }

    public Plan getPlanByNumber(String planNumber) {
        return planRepository.findByPlanNumber(planNumber);
    }

    public List<Plan> getAllPlans() {
        return planRepository.findAll();
    }

    public Plan updateFinalQuantity(String planNumber, int finalQuantity) {
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found with number: " + planNumber);
        }
        
        plan.setFinalQuantity(finalQuantity);
        return planRepository.save(plan);
    }
}
