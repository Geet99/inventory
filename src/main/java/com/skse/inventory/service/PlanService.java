package com.skse.inventory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skse.inventory.model.BatchLog;
import com.skse.inventory.model.Plan;
import com.skse.inventory.model.PlanStatus;
import com.skse.inventory.repository.PlanRepository;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Plan createPlan(Plan plan) {
        plan.setCreateDate(LocalDate.now());
        plan.setStatus(PlanStatus.Pending_Cutting); // Initial status
        return planRepository.save(plan);
    }

    public Plan updatePlanStatus(String planNumber, PlanStatus newStatus) {
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found with number: " + planNumber);
        }

        // Validate the transition
        if (!isValidStatusTransition(plan.getStatus(), newStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition: " + plan.getStatus() + " to " + newStatus
            );
        }

        // Set timestamps based on the new status
        switch (newStatus) {
            case Cutting -> plan.setCuttingStartDate(LocalDate.now());
            case Pending_Printing -> plan.setCuttingEndDate(LocalDate.now());
            case Printing -> plan.setPrintingStartDate(LocalDate.now());
            case Pending_Stitching -> plan.setPrintingEndDate(LocalDate.now());
            case Stitching -> plan.setStitchingStartDate(LocalDate.now());
            case Pending_Machine -> plan.setStitchingEndDate(LocalDate.now());
            case Machine -> plan.setMachineStartDate(LocalDate.now());
            case Completed -> plan.setMachineEndDate(LocalDate.now());
        }

        if (newStatus == PlanStatus.Pending_Machine) {
            articleService.addUpperStockFromPlan(plan);
        }

        // Update the status and save the plan
        plan.setStatus(newStatus);
        return planRepository.save(plan);
    }

    // Validate the transition logic
    private boolean isValidStatusTransition(PlanStatus current, PlanStatus next) {
        return switch (current) {
            case Pending_Cutting -> next == PlanStatus.Cutting;
            case Cutting -> next == PlanStatus.Pending_Printing;
            case Pending_Printing -> next == PlanStatus.Printing;
            case Printing -> next == PlanStatus.Pending_Stitching;
            case Pending_Stitching -> next == PlanStatus.Stitching;
            case Stitching -> next == PlanStatus.Pending_Machine;
            case Pending_Machine -> next == PlanStatus.Machine;
            case Machine -> next == PlanStatus.Completed;
            case Completed -> false; // No transitions from Completed
        };
    }

    public Plan recordMachineProcessing(String planNumber, String processedPairs, LocalDate startDate, LocalDate endDate) {
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan == null)
            throw new IllegalArgumentException("Plan not found with number: " + planNumber);

        if (plan.getStatus() != PlanStatus.Machine) {
            throw new IllegalStateException("Plan is not in Machine state");
        }

        articleService.processBatchToFinishedStock(plan, processedPairs);

        // Parse and update processed pairs
        String[] processedPairsArray = processedPairs.split(",");
        List<BatchLog> batchLogs = new ArrayList<>();

        try {
            if (plan.getMachineBatchLogs() != null) {
                batchLogs = objectMapper.readValue(plan.getMachineBatchLogs(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error parsing existing batch logs", e);
        }

        for (String pair : processedPairsArray) {
            String[] sizeAndCount = pair.split(":");
            BatchLog batchLog = new BatchLog(
                    sizeAndCount[0],
                    Integer.parseInt(sizeAndCount[1]),
                    startDate,
                    endDate
            );
            batchLogs.add(batchLog);
        }

        // Update logs and processed pairs
        try {
            plan.setMachineBatchLogs(objectMapper.writeValueAsString(batchLogs));
        } catch (Exception e) {
            throw new IllegalStateException("Error updating batch logs", e);
        }

        // Calculate total processed count
        int totalProcessedCount = calculateTotalProcessed(plan, processedPairs);
        int totalPlanCount = Arrays.stream(plan.getSizeQuantityPairs().split(","))
                .mapToInt(pair -> Integer.parseInt(pair.split(":")[1]))
                .sum();

        // Update status if processing is complete
        if (totalProcessedCount >= totalPlanCount) {
            plan.setStatus(PlanStatus.Completed);
            plan.setMachineEndDate(endDate);
        }
//        String[] existingPairsArray = plan.getMachineProcessedPairs() != null
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

        return planRepository.save(plan);
    }

    private int calculateTotalProcessed(Plan plan, String processedPairs) {
        return Arrays.stream(processedPairs.split(","))
                .mapToInt(pair -> Integer.parseInt(pair.split(":")[1]))
                .sum();
    }
}
