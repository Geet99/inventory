package com.skse.inventory.repository;

import com.skse.inventory.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    Plan findByPlanNumber(String planNumber);

    @Query("SELECT p.cuttingVendor, COUNT(p), SUM(a.cuttingCost) " +
            "FROM Plan p JOIN Article a ON p.articleName = a.articleName " +
            "WHERE p.status IN ('Cutting', 'Pending_Printing') " +
            "GROUP BY p.cuttingVendor")
    List<Object[]> getCuttingVendorSummary();

    @Query("SELECT p.printingVendor, COUNT(p), SUM(a.printingCost) " +
            "FROM Plan p JOIN Article a ON p.articleName = a.articleName " +
            "WHERE p.status IN ('Printing', 'Pending_Stitching') " +
            "GROUP BY p.printingVendor")
    List<Object[]> getPrintingVendorSummary();

    @Query("SELECT p.stitchingVendor, COUNT(p), SUM(a.stitchingCost) " +
            "FROM Plan p JOIN Article a ON p.articleName = a.articleName " +
            "WHERE p.status IN ('Stitching', 'Pending_Machine') " +
            "GROUP BY p.stitchingVendor")
    List<Object[]> getStitchingVendorSummary();

    @Query("SELECT p.status, COUNT(p) " +
            "FROM Plan p " +
            "WHERE p.status NOT IN ('Completed') " +
            "GROUP BY p.status")
    List<Object[]> getActiveOrdersByState();
}
