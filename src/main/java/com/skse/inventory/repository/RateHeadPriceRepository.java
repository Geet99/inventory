package com.skse.inventory.repository;

import com.skse.inventory.model.RateHead;
import com.skse.inventory.model.RateHeadPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RateHeadPriceRepository extends JpaRepository<RateHeadPrice, Long> {

    List<RateHeadPrice> findByRateHeadOrderByEffectiveFromDesc(RateHead rateHead);

    /**
     * Finds price entries for a rate head effective at or before a given date,
     * most recent first. Take the first result for the applicable cost.
     */
    @Query("SELECT p FROM RateHeadPrice p WHERE p.rateHead = :rateHead " +
           "AND p.effectiveFrom <= :date ORDER BY p.effectiveFrom DESC")
    List<RateHeadPrice> findEffectivePrices(@Param("rateHead") RateHead rateHead,
                                            @Param("date") LocalDate date);

    void deleteByRateHead(RateHead rateHead);
}
