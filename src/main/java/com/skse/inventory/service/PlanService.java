package com.skse.inventory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skse.inventory.model.*;
import com.skse.inventory.repository.ArticleRepository;
import com.skse.inventory.repository.PlanRepository;
import com.skse.inventory.repository.UpperStockRepository;
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
                plan.setStitchingVendorPaymentDue(calculatePayment(plan, VendorRole.Stitching));
                articleService.updateUpperStockAfterCompletion(plan);
                break;
        }

        plan.setStatus(nextStatus);
        return planRepository.save(plan);
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
        Article article = articleRepository.findByArticleName(plan.getArticleName()).get();
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

    private void updateStockFromPlan(Plan plan) {
        String[] sizeQuantityPairs = plan.getSizeQuantityPairs().split(",");
        for (String pair : sizeQuantityPairs) {
            String[] sizeQuantity = pair.split(":");
            String size = sizeQuantity[0];
            int quantity = Integer.parseInt(sizeQuantity[1]);

            // Update upper stock and finished stock as per size and color
            Optional<UpperStock> upperStock = upperStockRepository.findByArticleNameAndSizeAndColor(plan.getArticleName(), size, plan.getColor());
            UpperStock stock = null;
            if (upperStock.isPresent()) {
                stock = upperStock.get();
                stock.setQuantity(stock.getQuantity() + quantity);
            } else {
                stock = new UpperStock();
                Article article = articleRepository.findByArticleName(plan.getArticleName()).get();
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
                plan.setCuttingVendor(vendorAssignmentRequest.getVendorId());
                break;
            case Printing:
                plan.setPrintingVendor(vendorAssignmentRequest.getVendorId());
                break;
            case Stitching:
                plan.setStitchingVendor(vendorAssignmentRequest.getVendorId());
                break;
            default:
                throw new IllegalArgumentException("Invalid vendor role: " + vendorAssignmentRequest.getRole());
        }

        planRepository.save(plan);
    }

//    public Plan recordMachineProcessing(String planNumber, String processedPairs, LocalDate startDate, LocalDate endDate) {
//        Plan plan = planRepository.findByPlanNumber(planNumber);
//        if (plan == null)
//            throw new IllegalArgumentException("Plan not found with number: " + planNumber);
//
//        if (plan.getStatus() != PlanStatus.Machine) {
//            throw new IllegalStateException("Plan is not in Machine state");
//        }
//
//        articleService.processBatchToFinishedStock(plan, processedPairs);
//
//        // Parse and update processed pairs
//        String[] processedPairsArray = processedPairs.split(",");
//        List<BatchLog> batchLogs = new ArrayList<>();
//
//        try {
//            if (plan.getMachineBatchLogs() != null) {
//                batchLogs = objectMapper.readValue(plan.getMachineBatchLogs(), new TypeReference<>() {});
//            }
//        } catch (Exception e) {
//            throw new IllegalStateException("Error parsing existing batch logs", e);
//        }
//
//        for (String pair : processedPairsArray) {
//            String[] sizeAndCount = pair.split(":");
//            BatchLog batchLog = new BatchLog(
//                    sizeAndCount[0],
//                    Integer.parseInt(sizeAndCount[1]),
//                    startDate,
//                    endDate
//            );
//            batchLogs.add(batchLog);
//        }
//
//        // Update logs and processed pairs
//        try {
//            plan.setMachineBatchLogs(objectMapper.writeValueAsString(batchLogs));
//        } catch (Exception e) {
//            throw new IllegalStateException("Error updating batch logs", e);
//        }
//
//        // Calculate total processed count
//        int totalProcessedCount = calculateTotalProcessed(plan, processedPairs);
//        int totalPlanCount = Arrays.stream(plan.getSizeQuantityPairs().split(","))
//                .mapToInt(pair -> Integer.parseInt(pair.split(":")[1]))
//                .sum();
//
//        // Update status if processing is complete
//        if (totalProcessedCount >= totalPlanCount) {
//            plan.setStatus(PlanStatus.Completed);
//            plan.setMachineEndDate(endDate);
//        }
////        String[] existingPairsArray = plan.getMachineProcessedPairs() != null
//                ? plan.getMachineProcessedPairs().split(",")
//                : new String[0];
//
//        Map<String, Integer> totalPairs = new HashMap<>();
//
//        // Add existing processed pairs
//        for (String pair : existingPairsArray) {
//            String[] sizeAndCount = pair.split(":");
//            totalPairs.put(sizeAndCount[0], totalPairs.getOrDefault(sizeAndCount[0], 0) + Integer.parseInt(sizeAndCount[1]));
//        }
//
//        // Add new processed pairs
//        for (String pair : processedPairsArray) {
//            String[] sizeAndCount = pair.split(":");
//            totalPairs.put(sizeAndCount[0], totalPairs.getOrDefault(sizeAndCount[0], 0) + Integer.parseInt(sizeAndCount[1]));
//        }
//
//        // Rebuild processed pairs as a string
//        StringBuilder updatedPairs = new StringBuilder();
//        int totalProcessedCount = 0;
//        for (Map.Entry<String, Integer> entry : totalPairs.entrySet()) {
//            if (updatedPairs.length() > 0) updatedPairs.append(",");
//            updatedPairs.append(entry.getKey()).append(":").append(entry.getValue());
//            totalProcessedCount += entry.getValue();
//        }
//
//        plan.setMachineProcessedPairs(updatedPairs.toString());
//
//        // Check if the plan is complete
//        int totalPlanCount = Arrays.stream(plan.getSizeQuantityPairs().split(","))
//                .mapToInt(pair -> Integer.parseInt(pair.split(":")[1]))
//                .sum();
//
//        if (totalProcessedCount >= totalPlanCount) {
//            plan.setStatus(PlanStatus.Completed);
//            plan.setMachineEndDate(LocalDateTime.now());
//        }

//        return planRepository.save(plan);
//    }
//
//    private int calculateTotalProcessed(Plan plan, String processedPairs) {
//        return Arrays.stream(processedPairs.split(","))
//                .mapToInt(pair -> Integer.parseInt(pair.split(":")[1]))
//                .sum();
//    }
//
//    public Map<String, Integer> getActiveOrdersByState() {
//        List<Object[]> results = planRepository.getActiveOrdersByState();
//        Map<String, Integer> stateSummary = new HashMap<>();
//        for (Object[] row : results) {
//            stateSummary.put(row[0].toString(), ((Long) row[1]).intValue());
//        }
//        return stateSummary;
//    }
}
