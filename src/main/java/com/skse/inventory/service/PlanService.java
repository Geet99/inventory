package com.skse.inventory.service;

import com.skse.inventory.model.*;
import com.skse.inventory.repository.ArticleRepository;
import com.skse.inventory.repository.PlanRepository;
import com.skse.inventory.repository.UpperStockRepository;
import com.skse.inventory.repository.FinishedStockRepository;
import com.skse.inventory.repository.StockMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class PlanService {
    private static final Logger log = LoggerFactory.getLogger(PlanService.class);
    @Autowired
    private PlanRepository planRepository;

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

    @Autowired
    private RateHeadService rateHeadService;

    /**
     * Resolves a plan by primary key ({@link Plan#getPlanNumber()}), trimming the argument.
     * Tries exact match first, then case-insensitive match so URLs and filters stay consistent
     * across databases (e.g. PostgreSQL) and proxies that may alter path casing.
     */
    private Plan findPlanByNumberOrNull(String planNumber) {
        if (planNumber == null || planNumber.isBlank()) {
            return null;
        }
        String trimmed = planNumber.trim();
        Plan exact = planRepository.findByPlanNumber(trimmed);
        if (exact != null) {
            return exact;
        }
        return planRepository.findByPlanNumberIgnoreCase(trimmed).orElse(null);
    }

    /**
     * Creates plan. createDate is honored for reporting and cleanup (e.g. deleteCompletedPlansFromPreviousMonth).
     * Defaults to today only when null.
     */
    public Plan createPlan(Plan plan) {
        log.info("createPlan: planNumber={} article={} color={} total={}", plan.getPlanNumber(), plan.getArticleName(), plan.getColor(), plan.getTotal());
        validateTotalAndSizePairs(plan);
        if (plan.getCreateDate() == null) {
            plan.setCreateDate(LocalDate.now());
        }
        plan.setStatus(PlanStatus.Pending_Cutting);
        return planRepository.save(plan);
    }

    public Plan updatePlan(String planNumber, Plan updatedPlan) {
        Plan plan = findPlanByNumberOrNull(planNumber);
        if (plan != null) {
            Plan beforeEdit = snapshotForRecalculation(plan);
            boolean quantityOrDistributionChanged = stockAffectingFieldsChanged(beforeEdit, updatedPlan);
            boolean cuttingAlreadyCompleted = isCuttingCompletedForAccounting(beforeEdit);
            boolean machineAlreadyProcessed = beforeEdit.getMachineProcessingDate() != null;

            // Allow quantity edits in every state, then sync previously completed work below.
            validateTotalAndSizePairs(updatedPlan);
            plan.setTotal(updatedPlan.getTotal());
            plan.setSizeQuantityPairs(updatedPlan.getSizeQuantityPairs());

            plan.setArticleName(updatedPlan.getArticleName());
            plan.setColor(updatedPlan.getColor());
            plan.setDescription(updatedPlan.getDescription());
            plan.setPrintingRateHead(updatedPlan.getPrintingRateHead());
            if (updatedPlan.getCreateDate() != null) {
                plan.setCreateDate(updatedPlan.getCreateDate());
            }
            plan.setCuttingStartDate(updatedPlan.getCuttingStartDate());
            plan.setCuttingEndDate(updatedPlan.getCuttingEndDate());
            plan.setPrintingStartDate(updatedPlan.getPrintingStartDate());
            plan.setPrintingEndDate(updatedPlan.getPrintingEndDate());
            plan.setStitchingStartDate(updatedPlan.getStitchingStartDate());
            plan.setStitchingEndDate(updatedPlan.getStitchingEndDate());
            plan.setMachineProcessingDate(updatedPlan.getMachineProcessingDate());
            validateTransitionDateOrder(plan);

            if (cuttingAlreadyCompleted && quantityOrDistributionChanged) {
                applyStockSyncForEditedCompletedPlan(beforeEdit, plan, machineAlreadyProcessed);
            }
            syncCompletedVendorChargesAfterEdit(beforeEdit, plan);
            return planRepository.save(plan);
        } else {
            throw new IllegalArgumentException("Plan not found: " + planNumber);
        }
    }

    private static Plan snapshotForRecalculation(Plan src) {
        Plan p = new Plan();
        p.setPlanNumber(src.getPlanNumber());
        p.setArticleName(src.getArticleName());
        p.setColor(src.getColor());
        p.setTotal(src.getTotal());
        p.setSizeQuantityPairs(src.getSizeQuantityPairs());
        p.setStatus(src.getStatus());
        p.setMachineProcessingDate(src.getMachineProcessingDate());
        p.setCuttingEndDate(src.getCuttingEndDate());
        p.setPrintingEndDate(src.getPrintingEndDate());
        p.setStitchingEndDate(src.getStitchingEndDate());
        p.setCuttingVendor(src.getCuttingVendor());
        p.setPrintingVendor(src.getPrintingVendor());
        p.setStitchingVendor(src.getStitchingVendor());
        p.setPrintingRateHead(src.getPrintingRateHead());
        p.setCreateDate(src.getCreateDate());
        return p;
    }

    private static boolean stockAffectingFieldsChanged(Plan beforeEdit, Plan updatedPlan) {
        return beforeEdit.getTotal() != updatedPlan.getTotal()
                || !Objects.equals(beforeEdit.getSizeQuantityPairs(), updatedPlan.getSizeQuantityPairs())
                || !Objects.equals(beforeEdit.getArticleName(), updatedPlan.getArticleName())
                || !Objects.equals(beforeEdit.getColor(), updatedPlan.getColor());
    }

    private boolean isCuttingCompletedForAccounting(Plan plan) {
        return plan.getCuttingEndDate() != null
                || (plan.getStatus() != null && plan.getStatus().compareTo(PlanStatus.Pending_Printing) >= 0)
                || vendorService.hasVendorOrderForPlanWithRole(plan.getPlanNumber(), VendorRole.Cutting);
    }

    private boolean isPrintingCompletedForAccounting(Plan plan) {
        return plan.getPrintingEndDate() != null
                || (plan.getStatus() != null && plan.getStatus().compareTo(PlanStatus.Pending_Stitching) >= 0)
                || vendorService.hasVendorOrderForPlanWithRole(plan.getPlanNumber(), VendorRole.Printing);
    }

    private boolean isStitchingCompletedForAccounting(Plan plan) {
        return plan.getStitchingEndDate() != null
                || plan.getStatus() == PlanStatus.Completed
                || vendorService.hasVendorOrderForPlanWithRole(plan.getPlanNumber(), VendorRole.Stitching);
    }

    private void applyStockSyncForEditedCompletedPlan(Plan beforeEdit, Plan afterEdit, boolean machineAlreadyProcessed) {
        if (machineAlreadyProcessed) {
            reverseMoveStockFromUpperToFinished(beforeEdit);
        }
        reverseUpdateUpperStockFromPlan(beforeEdit);
        updateUpperStockFromPlan(afterEdit);
        if (machineAlreadyProcessed) {
            moveStockFromUpperToFinished(afterEdit, afterEdit.getTotal());
        }
    }

    private void syncCompletedVendorChargesAfterEdit(Plan beforeEdit, Plan plan) {
        String planNumber = plan.getPlanNumber();

        if (isCuttingCompletedForAccounting(beforeEdit)) {
            double amount = 0.0;
            if (plan.getCuttingVendor() != null) {
                amount = calculatePayment(plan, VendorRole.Cutting, plan.getCuttingEndDate());
            }
            plan.setCuttingVendorPaymentDue(amount);
            vendorService.syncVendorOrderForPlanRole(
                    planNumber, VendorRole.Cutting, plan.getCuttingVendor(), amount, plan.getCuttingEndDate());
        }

        if (isPrintingCompletedForAccounting(beforeEdit)) {
            double amount = 0.0;
            if (plan.getPrintingVendor() != null) {
                amount = calculatePayment(plan, VendorRole.Printing, plan.getPrintingEndDate());
            }
            plan.setPrintingVendorPaymentDue(amount);
            vendorService.syncVendorOrderForPlanRole(
                    planNumber, VendorRole.Printing, plan.getPrintingVendor(), amount, plan.getPrintingEndDate());
        }

        if (isStitchingCompletedForAccounting(beforeEdit)) {
            double amount = 0.0;
            if (plan.getStitchingVendor() != null) {
                amount = calculatePayment(plan, VendorRole.Stitching, plan.getStitchingEndDate());
            }
            plan.setStitchingVendorPaymentDue(amount);
            vendorService.syncVendorOrderForPlanRole(
                    planNumber, VendorRole.Stitching, plan.getStitchingVendor(), amount, plan.getStitchingEndDate());
        }
    }

    private static void validateTransitionDateOrder(Plan p) {
        if (p.getCuttingStartDate() != null && p.getCuttingEndDate() != null
                && p.getCuttingStartDate().isAfter(p.getCuttingEndDate())) {
            throw new IllegalArgumentException("Cutting start date cannot be after cutting end date.");
        }
        if (p.getCuttingEndDate() != null && p.getPrintingStartDate() != null
                && p.getCuttingEndDate().isAfter(p.getPrintingStartDate())) {
            throw new IllegalArgumentException("Cutting end date cannot be after printing start date.");
        }
        if (p.getPrintingStartDate() != null && p.getPrintingEndDate() != null
                && p.getPrintingStartDate().isAfter(p.getPrintingEndDate())) {
            throw new IllegalArgumentException("Printing start date cannot be after printing end date.");
        }
        if (p.getPrintingEndDate() != null && p.getStitchingStartDate() != null
                && p.getPrintingEndDate().isAfter(p.getStitchingStartDate())) {
            throw new IllegalArgumentException("Printing end date cannot be after stitching start date.");
        }
        if (p.getStitchingStartDate() != null && p.getStitchingEndDate() != null
                && p.getStitchingStartDate().isAfter(p.getStitchingEndDate())) {
            throw new IllegalArgumentException("Stitching start date cannot be after stitching end date.");
        }
    }

    private void validateTotalAndSizePairs(Plan plan) {
        if (plan.getTotal() <= 0) {
            throw new IllegalArgumentException("Total quantity must be greater than zero.");
        }

        String pairs = plan.getSizeQuantityPairs();
        if (pairs == null || pairs.trim().isEmpty()) {
            throw new IllegalArgumentException("Size:Quantity pairs are required.");
        }

        int sum = 0;
        try {
            String[] sizeQuantityPairs = pairs.split(",");
            for (String pair : sizeQuantityPairs) {
                if (pair.trim().isEmpty()) {
                    continue;
                }
                String[] sizeQuantity = pair.trim().split(":");
                if (sizeQuantity.length != 2) {
                    throw new IllegalArgumentException("Invalid size:quantity format. Use format like 6:50, 7:30, 8:20.");
                }
                int quantity = Integer.parseInt(sizeQuantity[1].trim());
                if (quantity <= 0) {
                    throw new IllegalArgumentException("All size quantities must be greater than zero.");
                }
                sum += quantity;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Size:quantity pairs must use whole numbers for quantities.");
        }

        if (sum != plan.getTotal()) {
            throw new IllegalArgumentException(String.format(
                "Total quantity (%d) does not match the sum (%d) of size:quantity pairs.",
                plan.getTotal(), sum
            ));
        }
    }

    /**
     * Deletes an in-progress plan (any status before Completed).
     * Reverses vendor payments for any stages that have already been completed.
     * Does NOT reverse stock.
     */
    @Transactional
    public void deletePlan(String planNumber) {
        log.info("deletePlan: planNumber={}", planNumber);
        Plan plan = findPlanByNumberOrNull(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found: " + planNumber);
        }
        if (plan.getStatus() == PlanStatus.Completed) {
            throw new IllegalArgumentException(
                    "Completed plans cannot be deleted. Use Cleanup instead.");
        }
        vendorService.removeVendorOrdersForPlan(plan.getPlanNumber());
        unlinkStockMovementsFromPlan(plan.getPlanNumber());
        planRepository.delete(plan);
    }

    /**
     * Cleanup for finished (Completed) plans: removes the plan from the screen
     * and its vendor payment info. Does NOT reverse stock.
     */
    @Transactional
    public void forceCleanupPlan(String planNumber) {
        Plan plan = findPlanByNumberOrNull(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found: " + planNumber);
        }
        if (plan.getStatus() != PlanStatus.Completed) {
            throw new IllegalArgumentException(
                    "Only completed plans can be cleaned up. Use Delete for in-progress plans.");
        }
        vendorService.removeVendorOrdersForPlan(plan.getPlanNumber());
        unlinkStockMovementsFromPlan(plan.getPlanNumber());
        planRepository.delete(plan);
    }

    private static boolean upperStockWasIncreasedForCuttingOutput(Plan plan) {
        PlanStatus s = plan.getStatus();
        return s != null && s.compareTo(PlanStatus.Pending_Printing) >= 0;
    }

    /** Inverse of {@link #moveStockFromUpperToFinished(Plan, int)} for {@link Plan#sendToMachine}. */
    private void reverseMoveStockFromUpperToFinished(Plan plan) {
        int finalQuantity = plan.getTotal();
        String[] sizeQuantityPairs = plan.getSizeQuantityPairs().split(",");

        for (String pair : sizeQuantityPairs) {
            if (pair.trim().isEmpty()) {
                continue;
            }
            String[] sizeQuantity = pair.trim().split(":");
            String size = sizeQuantity[0].trim();
            int plannedQuantity = Integer.parseInt(sizeQuantity[1].trim());
            double proportion = (double) plannedQuantity / plan.getTotal();
            int finalSizeQuantity = (int) Math.round(finalQuantity * proportion);

            Optional<FinishedStock> finishedStockOpt = finishedStockRepository.findFirstByArticleNameAndSizeAndColorOrderByIdAsc(
                    plan.getArticleName(), size, plan.getColor());
            if (finishedStockOpt.isEmpty()) {
                throw new IllegalStateException(
                        "Cannot reverse machine step: no finished stock for " + plan.getArticleName()
                                + " size " + size + " color " + plan.getColor());
            }
            FinishedStock finishedStock = finishedStockOpt.get();
            if (finishedStock.getQuantity() < finalSizeQuantity) {
                throw new IllegalStateException(
                        "Cannot reverse machine step: insufficient finished stock for size " + size
                                + ". Need " + finalSizeQuantity + ", have " + finishedStock.getQuantity());
            }
            finishedStock.setQuantity(finishedStock.getQuantity() - finalSizeQuantity);
            finishedStockRepository.save(finishedStock);

            Optional<UpperStock> upperOpt = upperStockRepository.findFirstByArticleNameAndSizeAndColorOrderByIdAsc(
                    plan.getArticleName(), size, plan.getColor());
            if (upperOpt.isEmpty()) {
                throw new IllegalStateException(
                        "Cannot reverse machine step: no upper stock row for size " + size);
            }
            UpperStock upperStock = upperOpt.get();
            upperStock.setQuantity(upperStock.getQuantity() + finalSizeQuantity);
            upperStockRepository.save(upperStock);
        }
    }

    /** Inverse of {@link #updateUpperStockFromPlan(Plan)}. */
    private void reverseUpdateUpperStockFromPlan(Plan plan) {
        String[] sizeQuantityPairs = plan.getSizeQuantityPairs().split(",");
        for (String pair : sizeQuantityPairs) {
            if (pair.trim().isEmpty()) {
                continue;
            }
            String[] sizeQuantity = pair.trim().split(":");
            String size = sizeQuantity[0].trim();
            int quantity = Integer.parseInt(sizeQuantity[1].trim());

            Optional<UpperStock> upperStockOpt = upperStockRepository.findFirstByArticleNameAndSizeAndColorOrderByIdAsc(
                    plan.getArticleName(), size, plan.getColor());
            if (upperStockOpt.isEmpty()) {
                // No row to reverse (never recorded, consumed elsewhere, or name mismatch before fix).
                continue;
            }
            UpperStock stock = upperStockOpt.get();
            if (stock.getQuantity() < quantity) {
                throw new IllegalStateException(
                        "Cannot reverse cutting stock: upper stock would go negative for article "
                                + plan.getArticleName() + ", size " + size + ", color " + plan.getColor()
                                + " (have " + stock.getQuantity() + ", need to remove " + quantity + ").");
            }
            stock.setQuantity(stock.getQuantity() - quantity);
            upperStockRepository.save(stock);
        }
    }

    private void unlinkStockMovementsFromPlan(String planNumber) {
        for (StockMovementRequest sm : stockMovementRepository.findByPlanNumber(planNumber)) {
            sm.setPlan(null);
            stockMovementRepository.save(sm);
        }
    }

    public Plan moveToNextState(String planNumber) {
        return moveToNextState(planNumber, LocalDate.now());
    }

    @Transactional
    public Plan moveToNextState(String planNumber, LocalDate transitionDate) {
        Plan plan = findPlanByNumberOrNull(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found with number: " + planNumber);
        }
        String canonicalPlanNumber = plan.getPlanNumber();

        PlanStatus nextStatus = getNextStatus(plan);
        if (nextStatus == null) {
            throw new IllegalStateException("Invalid status transition");
        }

        log.info("moveToNextState: plan={} currentStatus={} effectiveStatus={} -> nextStatus={}",
                canonicalPlanNumber, plan.getStatus(), getEffectiveStatus(plan), nextStatus);

        // Use transition date everywhere: plan timestamps and vendor payment month (financial)
        LocalDate date = transitionDate != null ? transitionDate : LocalDate.now();

        switch (nextStatus) {
            case Pending_Cutting:
                break;
            case Cutting:
                plan.setCuttingStartDate(date);
                break;
            case Pending_Printing:
                plan.setCuttingEndDate(date);
                boolean hasExistingOrder = vendorService.hasVendorOrderForPlanWithRole(canonicalPlanNumber, VendorRole.Cutting);
                log.info("Pending_Printing: plan={} hasExistingCuttingOrder={} cuttingVendor={}",
                        canonicalPlanNumber, hasExistingOrder, plan.getCuttingVendor() != null ? plan.getCuttingVendor().getName() : "NONE");
                if (!hasExistingOrder) {
                    if (plan.getCuttingVendor() != null) {
                        double cuttingPayment = calculatePayment(plan, VendorRole.Cutting, date);
                        plan.setCuttingVendorPaymentDue(cuttingPayment);
                        vendorService.recordVendorOrderToMonth(
                                plan.getCuttingVendor().getId(), canonicalPlanNumber, cuttingPayment,
                                VendorRole.Cutting, date);
                    }
                    log.info("Calling updateUpperStockFromPlan for plan={}", canonicalPlanNumber);
                    updateUpperStockFromPlan(plan);
                    log.info("updateUpperStockFromPlan completed for plan={}", canonicalPlanNumber);
                } else {
                    log.warn("SKIPPING upper stock update for plan={} because vendor order already exists!", canonicalPlanNumber);
                }
                break;
            case Printing:
                plan.setPrintingStartDate(date);
                break;
            case Pending_Stitching:
                plan.setPrintingEndDate(date);
                if (!vendorService.hasVendorOrderForPlanWithRole(canonicalPlanNumber, VendorRole.Printing)) {
                    if (plan.getPrintingVendor() != null) {
                        double printingPayment = calculatePayment(plan, VendorRole.Printing, date);
                        plan.setPrintingVendorPaymentDue(printingPayment);
                        vendorService.recordVendorOrderToMonth(
                                plan.getPrintingVendor().getId(), canonicalPlanNumber, printingPayment,
                                VendorRole.Printing, date);
                    }
                }
                break;
            case Stitching:
                plan.setStitchingStartDate(date);
                break;
            case Completed:
                plan.setStitchingEndDate(date);
                if (!vendorService.hasVendorOrderForPlanWithRole(canonicalPlanNumber, VendorRole.Stitching)) {
                    if (plan.getStitchingVendor() != null) {
                        double stitchingPayment = calculatePayment(plan, VendorRole.Stitching, date);
                        plan.setStitchingVendorPaymentDue(stitchingPayment);
                        vendorService.recordVendorOrderToMonth(
                                plan.getStitchingVendor().getId(), canonicalPlanNumber, stitchingPayment,
                                VendorRole.Stitching, date);
                    }
                }
                break;
        }

        plan.setStatus(nextStatus);
        return planRepository.save(plan);
    }

    public Plan sendToMachine(String planNumber) {
        log.info("sendToMachine: planNumber={}", planNumber);
        Plan plan = findPlanByNumberOrNull(planNumber);
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
            Optional<UpperStock> upperStockOpt = upperStockRepository.findFirstByArticleNameAndSizeAndColorOrderByIdAsc(
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
        String articleKey = Article.normalizeNameKey(plan.getArticleName());
        if (articleKey == null) {
            throw new IllegalArgumentException("Plan has no article name.");
        }
        Article article = articleRepository.findFirstByNameNormalizedOrderByIdAsc(articleKey)
            .orElseThrow(() -> new IllegalArgumentException("Article not found: " + plan.getArticleName()));
        
        for (String pair : sizeQuantityPairs) {
            String[] sizeQuantity = pair.trim().split(":");
            String size = sizeQuantity[0].trim();
            int plannedQuantity = Integer.parseInt(sizeQuantity[1].trim());
            
            // Calculate proportional final quantity for this size
            double proportion = (double) plannedQuantity / plan.getTotal();
            int finalSizeQuantity = (int) Math.round(finalQuantity * proportion);
            
            // Reduce upper stock
            UpperStock upperStock = upperStockRepository.findFirstByArticleNameAndSizeAndColorOrderByIdAsc(
                plan.getArticleName(), size, plan.getColor()).get();
            upperStock.setQuantity(upperStock.getQuantity() - finalSizeQuantity);
            upperStockRepository.save(upperStock);
            
            // Add to finished stock
            Optional<FinishedStock> finishedStockOpt = finishedStockRepository.findFirstByArticleNameAndSizeAndColorOrderByIdAsc(
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

    /**
     * Workflow state for transitions and UI. Uses stored {@link Plan#getStatus()} when set, but if
     * transition dates (e.g. from Edit plan) are ahead of the stored status, uses the later of the two
     * so "Next state" does not re-apply cutting completion (vendor + upper stock) twice.
     */
    public PlanStatus getEffectiveStatus(Plan plan) {
        if (plan.getStatus() == PlanStatus.Completed) {
            return PlanStatus.Completed;
        }
        PlanStatus inferred = inferStatusFromDatesOnly(plan);
        if (plan.getStatus() == null) {
            return inferred;
        }
        int merged = Math.max(statusOrder(plan.getStatus()), statusOrder(inferred));
        return statusFromOrder(merged);
    }

    private static PlanStatus inferStatusFromDatesOnly(Plan plan) {
        if (plan.getStitchingEndDate() != null) {
            return PlanStatus.Completed;
        }
        if (plan.getStitchingStartDate() != null) {
            return PlanStatus.Stitching;
        }
        if (plan.getPrintingEndDate() != null) {
            return PlanStatus.Pending_Stitching;
        }
        if (plan.getPrintingStartDate() != null) {
            return PlanStatus.Printing;
        }
        if (plan.getCuttingEndDate() != null) {
            return PlanStatus.Pending_Printing;
        }
        if (plan.getCuttingStartDate() != null) {
            return PlanStatus.Cutting;
        }
        return PlanStatus.Pending_Cutting;
    }

    private static int statusOrder(PlanStatus s) {
        return switch (s) {
            case Pending_Cutting -> 0;
            case Cutting -> 1;
            case Pending_Printing -> 2;
            case Printing -> 3;
            case Pending_Stitching -> 4;
            case Stitching -> 5;
            case Completed -> 6;
        };
    }

    private static PlanStatus statusFromOrder(int order) {
        return switch (order) {
            case 0 -> PlanStatus.Pending_Cutting;
            case 1 -> PlanStatus.Cutting;
            case 2 -> PlanStatus.Pending_Printing;
            case 3 -> PlanStatus.Printing;
            case 4 -> PlanStatus.Pending_Stitching;
            case 5 -> PlanStatus.Stitching;
            case 6 -> PlanStatus.Completed;
            default -> PlanStatus.Pending_Cutting;
        };
    }

    /** Next workflow state after {@link #getEffectiveStatus(Plan)}. */
    public PlanStatus getNextStatus(Plan plan) {
        return nextStatusAfter(getEffectiveStatus(plan));
    }

    private static PlanStatus nextStatusAfter(PlanStatus current) {
        return switch (current) {
            case Pending_Cutting -> PlanStatus.Cutting;
            case Cutting -> PlanStatus.Pending_Printing;
            case Pending_Printing -> PlanStatus.Printing;
            case Printing -> PlanStatus.Pending_Stitching;
            case Pending_Stitching -> PlanStatus.Stitching;
            case Stitching -> PlanStatus.Completed;
            case Completed -> null;
        };
    }

    /**
     * Calculates the payment amount for a plan's operation type.
     * Uses the plan's createDate to find the rate from the rate head's price history,
     * so new price entries with later effective dates don't affect older plans.
     */
    /**
     * Calculates the payment amount for a plan's operation type.
     * Uses the provided effectiveDate to find the rate from the rate head's price history.
     * For transitions, this is the transition date (when the stage completes).
     * For recalculations, this is the stage's end date.
     */
    private double calculatePayment(Plan plan, VendorRole roleType, LocalDate effectiveDate) {
        String articleKey = Article.normalizeNameKey(plan.getArticleName());
        if (articleKey == null) {
            throw new IllegalArgumentException("Plan has no article name.");
        }
        Article article = articleRepository.findFirstByNameNormalizedOrderByIdAsc(articleKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No article named \"" + plan.getArticleName()
                                + "\". Fix the plan's article so it matches an existing article (matching is case-insensitive)."));

        // Identify the rate head for this operation (Plan-level for printing, Article-level otherwise)
        RateHead rateHead = switch (roleType) {
            case Cutting -> article.getCuttingRateHead();
            case Printing -> plan.getPrintingRateHead() != null ? plan.getPrintingRateHead() : article.getPrintingRateHead();
            case Stitching -> article.getStitchingRateHead();
        };

        Double costPerUnit = null;

        if (rateHead != null) {
            LocalDate lookupDate = effectiveDate != null ? effectiveDate : LocalDate.now();
            costPerUnit = rateHeadService.getCostForDate(rateHead, lookupDate);
        }

        // Fall back to legacy cost fields on article (no rate head assigned or no price entries)
        if (costPerUnit == null) {
            costPerUnit = switch (roleType) {
                case Cutting -> article.getCuttingCost();
                case Printing -> article.getPrintingCost();
                case Stitching -> article.getStitchingCost();
            };
        }

        if (costPerUnit == null) {
            throw new IllegalStateException(
                    "Missing " + roleType + " cost for article \"" + article.getName()
                            + "\". Set cutting/printing/stitching rate heads (or legacy costs) on the article before completing this stage.");
        }
        return costPerUnit * plan.getTotal();
    }

    /**
     * Recalculates vendor payments for all plans that use the given rate head.
     * Called after a price entry is added, updated, or deleted so that
     * already-completed stages reflect the corrected rate.
     */
    @Transactional
    public void recalculatePaymentsForRateHead(RateHead rateHead) {
        if (rateHead == null) return;
        log.info("recalculatePaymentsForRateHead: rateHead={} (id={})", rateHead.getName(), rateHead.getId());

        // Find articles that reference this rate head
        List<Article> articles = articleRepository.findAllByOrderByIdAsc();
        Set<String> affectedArticleNames = new HashSet<>();
        Map<String, Set<VendorRole>> articleRoles = new HashMap<>();

        for (Article article : articles) {
            Set<VendorRole> roles = new HashSet<>();
            if (rateHead.equals(article.getCuttingRateHead())) roles.add(VendorRole.Cutting);
            if (rateHead.equals(article.getPrintingRateHead())) roles.add(VendorRole.Printing);
            if (rateHead.equals(article.getStitchingRateHead())) roles.add(VendorRole.Stitching);
            if (!roles.isEmpty()) {
                String normalized = Article.normalizeNameKey(article.getName());
                affectedArticleNames.add(normalized);
                articleRoles.put(normalized, roles);
            }
        }

        List<Plan> allPlans = planRepository.findAll();
        for (Plan plan : allPlans) {
            boolean changed = false;
            String normalized = Article.normalizeNameKey(plan.getArticleName());

            // Check cutting (rate head comes from article)
            if (affectedArticleNames.contains(normalized)
                    && articleRoles.get(normalized).contains(VendorRole.Cutting)
                    && plan.getCuttingEndDate() != null
                    && plan.getCuttingVendor() != null) {
                double amount = calculatePayment(plan, VendorRole.Cutting, plan.getCuttingEndDate());
                if (Math.abs(amount - plan.getCuttingVendorPaymentDue()) > 0.001) {
                    plan.setCuttingVendorPaymentDue(amount);
                    vendorService.syncVendorOrderForPlanRole(
                            plan.getPlanNumber(), VendorRole.Cutting, plan.getCuttingVendor(), amount, plan.getCuttingEndDate());
                    changed = true;
                }
            }

            // Check printing (rate head can come from plan or article)
            boolean printingAffected = false;
            if (plan.getPrintingRateHead() != null && rateHead.getId().equals(plan.getPrintingRateHead().getId())) {
                printingAffected = true;
            } else if (plan.getPrintingRateHead() == null
                    && affectedArticleNames.contains(normalized)
                    && articleRoles.getOrDefault(normalized, Set.of()).contains(VendorRole.Printing)) {
                printingAffected = true;
            }
            if (printingAffected && plan.getPrintingEndDate() != null && plan.getPrintingVendor() != null) {
                double amount = calculatePayment(plan, VendorRole.Printing, plan.getPrintingEndDate());
                if (Math.abs(amount - plan.getPrintingVendorPaymentDue()) > 0.001) {
                    plan.setPrintingVendorPaymentDue(amount);
                    vendorService.syncVendorOrderForPlanRole(
                            plan.getPlanNumber(), VendorRole.Printing, plan.getPrintingVendor(), amount, plan.getPrintingEndDate());
                    changed = true;
                }
            }

            // Check stitching (rate head comes from article)
            if (affectedArticleNames.contains(normalized)
                    && articleRoles.get(normalized).contains(VendorRole.Stitching)
                    && plan.getStitchingEndDate() != null
                    && plan.getStitchingVendor() != null) {
                double amount = calculatePayment(plan, VendorRole.Stitching, plan.getStitchingEndDate());
                if (Math.abs(amount - plan.getStitchingVendorPaymentDue()) > 0.001) {
                    plan.setStitchingVendorPaymentDue(amount);
                    vendorService.syncVendorOrderForPlanRole(
                            plan.getPlanNumber(), VendorRole.Stitching, plan.getStitchingVendor(), amount, plan.getStitchingEndDate());
                    changed = true;
                }
            }

            if (changed) {
                planRepository.save(plan);
            }
        }
    }

    public Map<String, Integer> getActiveOrdersByState() {
        Map<String, Integer> activeOrders = new HashMap<>();

        // Get the raw results from the repository
        List<Object[]> results = planRepository.getActiveOrdersByState();

        // Process each result and add to the map
        for (Object[] result : results) {
            String statusKey = planStatusGroupKey(result[0]);
            int count = ((Number) result[1]).intValue();
            activeOrders.put(statusKey, count);
        }

        return activeOrders;
    }

    /** JPA returns {@link PlanStatus} for {@code SELECT p.status}, not a String. */
    private static String planStatusGroupKey(Object statusColumn) {
        if (statusColumn instanceof PlanStatus ps) {
            return ps.name();
        }
        if (statusColumn instanceof String s) {
            return s;
        }
        return String.valueOf(statusColumn);
    }

    private void updateUpperStockFromPlan(Plan plan) {
        String pairs = plan.getSizeQuantityPairs();
        if (pairs == null || pairs.trim().isEmpty()) {
            throw new IllegalStateException("Cannot update upper stock: plan has no size:quantity pairs.");
        }
        String[] sizeQuantityPairs = pairs.split(",");
        for (String pair : sizeQuantityPairs) {
            if (pair == null || pair.trim().isEmpty()) {
                continue;
            }
            String[] sizeQuantity = pair.trim().split(":");
            if (sizeQuantity.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid size:quantity pair \"" + pair.trim() + "\". Use format like 6:50, 7:30.");
            }
            String size = sizeQuantity[0].trim();
            int quantity;
            try {
                quantity = Integer.parseInt(sizeQuantity[1].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid quantity in pair \"" + pair.trim() + "\": must be a whole number.");
            }

            // Update upper stock and finished stock as per size and color
            log.info("updateUpperStock: article={} size={} color={} qty={}", plan.getArticleName(), size, plan.getColor(), quantity);
            Optional<UpperStock> upperStock = upperStockRepository.findFirstByArticleNameAndSizeAndColorOrderByIdAsc(plan.getArticleName(), size, plan.getColor());
            UpperStock stock = null;
            if (upperStock.isPresent()) {
                stock = upperStock.get();
                log.info("updateUpperStock: found existing stock id={} oldQty={} newQty={}", stock.getId(), stock.getQuantity(), stock.getQuantity() + quantity);
                stock.setQuantity(stock.getQuantity() + quantity);
            } else {
                stock = new UpperStock();
                String articleKey = Article.normalizeNameKey(plan.getArticleName());
                if (articleKey == null) {
                    throw new IllegalArgumentException("Plan has no article name.");
                }
                Article article = articleRepository.findFirstByNameNormalizedOrderByIdAsc(articleKey)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No article named \"" + plan.getArticleName()
                                        + "\". Fix the plan's article so it matches an existing article (matching is case-insensitive)."));
                log.info("updateUpperStock: creating new stock for article={} (id={}) size={} color={} qty={}", article.getName(), article.getId(), size, plan.getColor(), quantity);
                stock.setArticle(article);
                stock.setSize(size);
                stock.setColor(plan.getColor());
                stock.setQuantity(quantity); // Initialize with quantity from the plan
            }
            upperStockRepository.save(stock);
        }
    }

    public void assignVendorToPlan(String planNumber, VendorAssignmentRequest vendorAssignmentRequest) {
        Plan plan = findPlanByNumberOrNull(planNumber);
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
        log.info("assignVendorsToPlan: plan={} cutting={} printing={} stitching={}", planNumber, cuttingVendorId, printingVendorId, stitchingVendorId);
        Plan plan = findPlanByNumberOrNull(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found with number: " + planNumber);
        }

        // null = no change, 0 = unassign, >0 = assign that vendor
        if (cuttingVendorId != null) {
            if (cuttingVendorId == 0) {
                plan.setCuttingVendor(null);
            } else {
                Vendor cuttingVendor = vendorService.getVendorById(cuttingVendorId);
                if (cuttingVendor == null) {
                    throw new IllegalArgumentException("Cutting vendor not found");
                }
                plan.setCuttingVendor(cuttingVendor);
            }
        }

        if (printingVendorId != null) {
            if (printingVendorId == 0) {
                plan.setPrintingVendor(null);
            } else {
                Vendor printingVendor = vendorService.getVendorById(printingVendorId);
                if (printingVendor == null) {
                    throw new IllegalArgumentException("Printing vendor not found");
                }
                plan.setPrintingVendor(printingVendor);
            }
        }

        if (stitchingVendorId != null) {
            if (stitchingVendorId == 0) {
                plan.setStitchingVendor(null);
            } else {
                Vendor stitchingVendor = vendorService.getVendorById(stitchingVendorId);
                if (stitchingVendor == null) {
                    throw new IllegalArgumentException("Stitching vendor not found");
                }
                plan.setStitchingVendor(stitchingVendor);
            }
        }

        planRepository.save(plan);
    }

    public Plan getPlanByNumber(String planNumber) {
        return findPlanByNumberOrNull(planNumber);
    }

    public List<Plan> getAllPlans() {
        return planRepository.findAllByOrderByPlanNumberIgnoreCaseDesc();
    }

    /**
     * Get plans filtered by plan number/article name (contains, case-insensitive), status,
     * and/or create date range.
     * Null or blank inputs and null dates mean no filter on that criterion.
     */
    public List<Plan> getPlansFiltered(String planNumber, String articleName, PlanStatus status, LocalDate createDateFrom, LocalDate createDateTo) {
        String q = (planNumber != null && !planNumber.isBlank()) ? planNumber.trim() : null;
        String articleQ = (articleName != null && !articleName.isBlank()) ? articleName.trim() : null;
        if (q == null && articleQ == null && status == null && createDateFrom == null && createDateTo == null) {
            return planRepository.findAllByOrderByPlanNumberIgnoreCaseDesc();
        }
        String planPattern = toContainsLikePattern(q);
        String articlePattern = toContainsLikePattern(articleQ);
        if (status == null) {
            return planRepository.findFiltered(planPattern, articlePattern, createDateFrom, createDateTo);
        }
        return planRepository.findFilteredByStatus(planPattern, articlePattern, status, createDateFrom, createDateTo);
    }

    /** Lowercase %term% pattern for JPQL LIKE. Returns {@code "%"} (match-all) when blank so callers never pass null to a query parameter (PostgreSQL cannot infer the type of a null String parameter). */
    private static String toContainsLikePattern(String term) {
        if (term == null || term.isBlank()) {
            return "%";
        }
        return "%" + term.toLowerCase() + "%";
    }

    /**
     * Deletes completed plans whose createDate falls in the previous calendar month.
     * Uses plan createDate (not transition dates) for consistency with "plan added" period.
     */
    public int deleteCompletedPlansFromPreviousMonth() {
        LocalDate now = LocalDate.now();
        LocalDate startOfPreviousMonth = now.minusMonths(1).withDayOfMonth(1);
        LocalDate endOfPreviousMonth = now.withDayOfMonth(1).minusDays(1);

        List<Plan> completedPlans = planRepository.findAll().stream()
            .filter(plan -> plan.getStatus() == PlanStatus.Completed)
            .filter(plan -> {
                LocalDate createDate = plan.getCreateDate();
                return createDate != null
                    && !createDate.isBefore(startOfPreviousMonth)
                    && !createDate.isAfter(endOfPreviousMonth);
            })
            .toList();

        int count = completedPlans.size();
        for (Plan plan : completedPlans) {
            vendorService.removeVendorOrdersForPlan(plan.getPlanNumber());
            unlinkStockMovementsFromPlan(plan.getPlanNumber());
        }
        planRepository.deleteAll(completedPlans);
        return count;
    }
}
