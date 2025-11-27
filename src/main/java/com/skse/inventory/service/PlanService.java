package com.skse.inventory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skse.inventory.model.*;
import com.skse.inventory.repository.ArticleRepository;
import com.skse.inventory.repository.PlanRepository;
import com.skse.inventory.repository.UpperStockRepository;
import com.skse.inventory.repository.FinishedStockRepository;
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
    private FinishedStockRepository finishedStockRepository;
    
    @Autowired
    private StockMovementRepository stockMovementRepository;
    
    @Autowired
    private VendorService vendorService;

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
                // Record vendor order for cutting - using COMPLETION DATE
                if (plan.getCuttingVendor() != null) {
                    double cuttingPayment = calculatePayment(plan, VendorRole.Cutting);
                    plan.setCuttingVendorPaymentDue(cuttingPayment);
                    // Use the cutting END date for monthly accounting
                    vendorService.recordVendorOrderToMonth(
                        plan.getCuttingVendor().getId(), 
                        planNumber, 
                        cuttingPayment, 
                        VendorRole.Cutting,
                        plan.getCuttingEndDate() // Completion date
                    );
                }
                updateUpperStockFromPlan(plan);
                break;
            case Printing:
                plan.setPrintingStartDate(LocalDate.now());
                break;
            case Pending_Stitching:
                plan.setPrintingEndDate(LocalDate.now());
                // Record vendor order for printing - using COMPLETION DATE
                if (plan.getPrintingVendor() != null) {
                    double printingPayment = calculatePayment(plan, VendorRole.Printing);
                    plan.setPrintingVendorPaymentDue(printingPayment);
                    // Use the printing END date for monthly accounting
                    vendorService.recordVendorOrderToMonth(
                        plan.getPrintingVendor().getId(), 
                        planNumber, 
                        printingPayment, 
                        VendorRole.Printing,
                        plan.getPrintingEndDate() // Completion date
                    );
                }
                break;
            case Stitching:
                plan.setStitchingStartDate(LocalDate.now());
                break;
            case Completed:
                plan.setStitchingEndDate(LocalDate.now());
                // Record vendor order for stitching - using COMPLETION DATE
                if (plan.getStitchingVendor() != null) {
                    double stitchingPayment = calculatePayment(plan, VendorRole.Stitching);
                    plan.setStitchingVendorPaymentDue(stitchingPayment);
                    // Use the stitching END date for monthly accounting
                    vendorService.recordVendorOrderToMonth(
                        plan.getStitchingVendor().getId(), 
                        planNumber, 
                        stitchingPayment, 
                        VendorRole.Stitching,
                        plan.getStitchingEndDate() // Completion date
                    );
                }
                break;
        }

        plan.setStatus(nextStatus);
        return planRepository.save(plan);
    }

    public Plan sendToMachine(String planNumber) {
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found with number: " + planNumber);
        }

        if (plan.getStatus() != PlanStatus.Completed) {
            throw new IllegalStateException("Plan must be completed before sending to machine");
        }

        plan.setMachineProcessingDate(LocalDate.now());
        
        // Move stock from upper to finished using the total quantity
        moveStockFromUpperToFinished(plan, plan.getTotal());
        
        return planRepository.save(plan);
    }

    private void moveStockFromUpperToFinished(Plan plan, int finalQuantity) {
        String[] sizeQuantityPairs = plan.getSizeQuantityPairs().split(",");
        
        // First, validate that we have sufficient upper stock for all sizes
        for (String pair : sizeQuantityPairs) {
            String[] sizeQuantity = pair.trim().split(":");
            String size = sizeQuantity[0].trim();
            int plannedQuantity = Integer.parseInt(sizeQuantity[1].trim());
            
            // Calculate proportional final quantity for this size
            double proportion = (double) plannedQuantity / plan.getTotal();
            int finalSizeQuantity = (int) Math.round(finalQuantity * proportion);
            
            // Check upper stock availability
            Optional<UpperStock> upperStockOpt = upperStockRepository.findByArticleNameAndSizeAndColor(
                plan.getArticleName(), size, plan.getColor());
            
            if (!upperStockOpt.isPresent()) {
                throw new IllegalStateException(
                    String.format("No upper stock found for Article: %s, Size: %s, Color: %s", 
                        plan.getArticleName(), size, plan.getColor()));
            }
            
            UpperStock upperStock = upperStockOpt.get();
            if (upperStock.getQuantity() < finalSizeQuantity) {
                throw new IllegalStateException(
                    String.format("Insufficient upper stock for Article: %s, Size: %s, Color: %s. Required: %d, Available: %d", 
                        plan.getArticleName(), size, plan.getColor(), finalSizeQuantity, upperStock.getQuantity()));
            }
        }
        
        // Now move the stock
        Article article = articleRepository.findByName(plan.getArticleName())
            .orElseThrow(() -> new IllegalArgumentException("Article not found: " + plan.getArticleName()));
        
        for (String pair : sizeQuantityPairs) {
            String[] sizeQuantity = pair.trim().split(":");
            String size = sizeQuantity[0].trim();
            int plannedQuantity = Integer.parseInt(sizeQuantity[1].trim());
            
            // Calculate proportional final quantity for this size
            double proportion = (double) plannedQuantity / plan.getTotal();
            int finalSizeQuantity = (int) Math.round(finalQuantity * proportion);
            
            // Reduce upper stock
            UpperStock upperStock = upperStockRepository.findByArticleNameAndSizeAndColor(
                plan.getArticleName(), size, plan.getColor()).get();
            upperStock.setQuantity(upperStock.getQuantity() - finalSizeQuantity);
            upperStockRepository.save(upperStock);
            
            // Add to finished stock
            Optional<FinishedStock> finishedStockOpt = finishedStockRepository.findByArticleNameAndSizeAndColor(
                plan.getArticleName(), size, plan.getColor());
            
            if (finishedStockOpt.isPresent()) {
                FinishedStock finishedStock = finishedStockOpt.get();
                finishedStock.setQuantity(finishedStock.getQuantity() + finalSizeQuantity);
                finishedStockRepository.save(finishedStock);
            } else {
                FinishedStock newFinishedStock = new FinishedStock();
                newFinishedStock.setArticle(article);
                newFinishedStock.setSize(size);
                newFinishedStock.setColor(plan.getColor());
                newFinishedStock.setQuantity(finalSizeQuantity);
                finishedStockRepository.save(newFinishedStock);
            }
            
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
            case Printing -> (plan.getPrintingRateHead() != null) 
                ? plan.getPrintingRateHead().getCost() 
                : article.getPrintingCost();
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
    
    public void assignVendorsToPlan(String planNumber, Long cuttingVendorId, Long printingVendorId, Long stitchingVendorId) {
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found with number: " + planNumber);
        }

        // Only update vendors that were selected (not null or empty)
        if (cuttingVendorId != null && cuttingVendorId > 0) {
            Vendor cuttingVendor = vendorService.getVendorById(cuttingVendorId);
            if (cuttingVendor == null) {
                throw new IllegalArgumentException("Cutting vendor not found");
            }
            plan.setCuttingVendor(cuttingVendor);
        }
        
        if (printingVendorId != null && printingVendorId > 0) {
            Vendor printingVendor = vendorService.getVendorById(printingVendorId);
            if (printingVendor == null) {
                throw new IllegalArgumentException("Printing vendor not found");
            }
            plan.setPrintingVendor(printingVendor);
        }
        
        if (stitchingVendorId != null && stitchingVendorId > 0) {
            Vendor stitchingVendor = vendorService.getVendorById(stitchingVendorId);
            if (stitchingVendor == null) {
                throw new IllegalArgumentException("Stitching vendor not found");
            }
            plan.setStitchingVendor(stitchingVendor);
        }

        planRepository.save(plan);
    }

    public Plan getPlanByNumber(String planNumber) {
        return planRepository.findByPlanNumber(planNumber);
    }

    public List<Plan> getAllPlans() {
        return planRepository.findAll();
    }

    public int deleteCompletedPlansFromPreviousMonth() {
        LocalDate now = LocalDate.now();
        LocalDate startOfPreviousMonth = now.minusMonths(1).withDayOfMonth(1);
        LocalDate endOfPreviousMonth = now.withDayOfMonth(1).minusDays(1);
        
        List<Plan> completedPlans = planRepository.findAll().stream()
            .filter(plan -> plan.getStatus() == PlanStatus.Completed)
            .filter(plan -> {
                LocalDate createDate = plan.getCreateDate();
                return !createDate.isBefore(startOfPreviousMonth) && 
                       !createDate.isAfter(endOfPreviousMonth);
            })
            .toList();
        
        int count = completedPlans.size();
        planRepository.deleteAll(completedPlans);
        return count;
    }
}
