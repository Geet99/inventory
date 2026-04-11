package com.skse.inventory.service;

import com.skse.inventory.model.*;
import com.skse.inventory.repository.ArticleRepository;
import com.skse.inventory.repository.PlanRepository;
import com.skse.inventory.repository.UpperStockRepository;
import com.skse.inventory.repository.FinishedStockRepository;
import com.skse.inventory.repository.StockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class PlanService {
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

    /**
     * Creates plan. createDate is honored for reporting and cleanup (e.g. deleteCompletedPlansFromPreviousMonth).
     * Defaults to today only when null.
     */
    public Plan createPlan(Plan plan) {
        validateTotalAndSizePairs(plan);
        if (plan.getCreateDate() == null) {
            plan.setCreateDate(LocalDate.now());
        }
        plan.setStatus(PlanStatus.Pending_Cutting);
        return planRepository.save(plan);
    }

    public Plan updatePlan(String planNumber, Plan updatedPlan) {
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan != null) {
            // Only allow changing total & size:quantity pairs until cutting is finalized.
            // After we move to Pending_Printing (and upper stock is updated), changing
            // these fields would desync plan vs stock, so we keep the original values.
            PlanStatus status = plan.getStatus();
            boolean allowQuantityEdit = (status == null
                    || status == PlanStatus.Pending_Cutting
                    || status == PlanStatus.Cutting);

            if (allowQuantityEdit) {
                validateTotalAndSizePairs(updatedPlan);
                plan.setTotal(updatedPlan.getTotal());
                plan.setSizeQuantityPairs(updatedPlan.getSizeQuantityPairs());
            } else {
                // If user tried to change quantities after cutting is finalized, surface an error
                if (updatedPlan.getTotal() != plan.getTotal()
                        || (updatedPlan.getSizeQuantityPairs() != null
                            && !updatedPlan.getSizeQuantityPairs().equals(plan.getSizeQuantityPairs()))) {
                    throw new IllegalArgumentException(
                        "Total quantity and Size:Quantity pairs cannot be changed after cutting is completed. " +
                        "If the actual quantity is different, please adjust it during the Cutting stage."
                    );
                }
            }

            plan.setArticleName(updatedPlan.getArticleName());
            plan.setColor(updatedPlan.getColor());
            plan.setDescription(updatedPlan.getDescription());
            plan.setPrintingRateHead(updatedPlan.getPrintingRateHead());
            if (updatedPlan.getCreateDate() != null) {
                plan.setCreateDate(updatedPlan.getCreateDate());
            }
            return planRepository.save(plan);
        } else {
            throw new IllegalArgumentException("Plan not found: " + planNumber);
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
     * Deletes a plan only while it is still in Pending_Cutting (not yet started in the workshop).
     */
    public void deletePlan(String planNumber) {
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found: " + planNumber);
        }
        if (plan.getStatus() != PlanStatus.Pending_Cutting) {
            throw new IllegalArgumentException(
                    "Only plans in Pending Cutting state can be deleted. This plan has already progressed.");
        }
        unlinkStockMovementsFromPlan(planNumber);
        planRepository.delete(plan);
    }

    /**
     * Removes a plan that has already left {@link PlanStatus#Pending_Cutting}: reverses vendor monthly
     * charges, then stock (machine move if applicable, then cutting output on upper stock).
     * Use {@link #deletePlan(String)} for plans still pending cutting.
     */
    @Transactional
    public void forceCleanupPlan(String planNumber) {
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found: " + planNumber);
        }
        if (plan.getStatus() == PlanStatus.Pending_Cutting) {
            deletePlan(planNumber);
            return;
        }
        vendorService.removeVendorOrdersForPlan(planNumber);
        if (plan.getMachineProcessingDate() != null) {
            reverseMoveStockFromUpperToFinished(plan);
        }
        if (upperStockWasIncreasedForCuttingOutput(plan)) {
            reverseUpdateUpperStockFromPlan(plan);
        }
        unlinkStockMovementsFromPlan(planNumber);
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

            Optional<FinishedStock> finishedStockOpt = finishedStockRepository.findByArticleNameAndSizeAndColor(
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

            Optional<UpperStock> upperOpt = upperStockRepository.findByArticleNameAndSizeAndColor(
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

            Optional<UpperStock> upperStockOpt = upperStockRepository.findByArticleNameAndSizeAndColor(
                    plan.getArticleName(), size, plan.getColor());
            if (upperStockOpt.isEmpty()) {
                throw new IllegalStateException(
                        "Cannot reverse cutting stock: no upper stock for article "
                                + plan.getArticleName() + ", size " + size + ", color " + plan.getColor());
            }
            UpperStock stock = upperStockOpt.get();
            if (stock.getQuantity() < quantity) {
                throw new IllegalStateException(
                        "Cannot reverse cutting stock: upper stock would go negative for size "
                                + size + " (have " + stock.getQuantity() + ", need to remove " + quantity + ").");
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
        Plan plan = planRepository.findByPlanNumber(planNumber);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found with number: " + planNumber);
        }

        PlanStatus nextStatus = getNextStatus(plan.getStatus());
        if (nextStatus == null) {
            throw new IllegalStateException("Invalid status transition");
        }

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
                if (plan.getCuttingVendor() != null) {
                    double cuttingPayment = calculatePayment(plan, VendorRole.Cutting);
                    plan.setCuttingVendorPaymentDue(cuttingPayment);
                    vendorService.recordVendorOrderToMonth(
                        plan.getCuttingVendor().getId(), planNumber, cuttingPayment,
                        VendorRole.Cutting, date);
                }
                updateUpperStockFromPlan(plan);
                break;
            case Printing:
                plan.setPrintingStartDate(date);
                break;
            case Pending_Stitching:
                plan.setPrintingEndDate(date);
                if (plan.getPrintingVendor() != null) {
                    double printingPayment = calculatePayment(plan, VendorRole.Printing);
                    plan.setPrintingVendorPaymentDue(printingPayment);
                    vendorService.recordVendorOrderToMonth(
                        plan.getPrintingVendor().getId(), planNumber, printingPayment,
                        VendorRole.Printing, date);
                }
                break;
            case Stitching:
                plan.setStitchingStartDate(date);
                break;
            case Completed:
                plan.setStitchingEndDate(date);
                if (plan.getStitchingVendor() != null) {
                    double stitchingPayment = calculatePayment(plan, VendorRole.Stitching);
                    plan.setStitchingVendorPaymentDue(stitchingPayment);
                    vendorService.recordVendorOrderToMonth(
                        plan.getStitchingVendor().getId(), planNumber, stitchingPayment,
                        VendorRole.Stitching, date);
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

    public PlanStatus getNextStatus(PlanStatus currentStatus) {
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
        Article article = articleRepository.findByName(plan.getArticleName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No article named \"" + plan.getArticleName()
                                + "\". Fix the plan's article so it matches an existing article name exactly."));
        int totalQuantity = plan.getTotal();
        Double costPerUnit = switch (roleType) {
            case Cutting -> article.getCuttingCost();
            case Printing -> (plan.getPrintingRateHead() != null)
                    ? plan.getPrintingRateHead().getCost()
                    : article.getPrintingCost();
            case Stitching -> article.getStitchingCost();
            default -> 0.0;
        };
        if (costPerUnit == null) {
            throw new IllegalStateException(
                    "Missing " + roleType + " cost for article \"" + article.getName()
                            + "\". Set cutting/printing/stitching rate heads (or legacy costs) on the article before completing this stage.");
        }
        return costPerUnit * totalQuantity;
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
                Article article = articleRepository.findByName(plan.getArticleName())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No article named \"" + plan.getArticleName()
                                        + "\". Fix the plan's article so it matches an existing article name exactly."));
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
        return planRepository.findAllByOrderByPlanNumberAsc();
    }

    /**
     * Get plans filtered by plan number (contains, case-insensitive) and/or create date range.
     * Null or blank planNumber and null dates mean no filter on that criterion.
     */
    public List<Plan> getPlansFiltered(String planNumber, LocalDate createDateFrom, LocalDate createDateTo) {
        String q = (planNumber != null && !planNumber.isBlank()) ? planNumber.trim() : null;
        if (q == null && createDateFrom == null && createDateTo == null) {
            return planRepository.findAllByOrderByPlanNumberAsc();
        }
        return planRepository.findFiltered(q, createDateFrom, createDateTo);
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
        planRepository.deleteAll(completedPlans);
        return count;
    }
}
