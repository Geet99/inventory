package com.skse.inventory.service;

import com.skse.inventory.model.RateHead;
import com.skse.inventory.model.RateHeadPrice;
import com.skse.inventory.model.VendorRole;
import com.skse.inventory.repository.RateHeadPriceRepository;
import com.skse.inventory.repository.RateHeadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RateHeadService {
    private static final Logger log = LoggerFactory.getLogger(RateHeadService.class);

    @Autowired
    private RateHeadRepository rateHeadRepository;

    @Autowired
    private RateHeadPriceRepository rateHeadPriceRepository;

    public RateHead createRateHead(RateHead rateHead) {
        log.info("Creating rate head: name={} operationType={} cost={}", rateHead.getName(), rateHead.getOperationType(), rateHead.getCost());
        return rateHeadRepository.save(rateHead);
    }

    public List<RateHead> getAllRateHeads() {
        return rateHeadRepository.findAll();
    }

    public RateHead getRateHeadById(Long id) {
        return rateHeadRepository.findById(id).orElse(null);
    }

    public List<RateHead> getRateHeadsByOperationType(VendorRole operationType) {
        return rateHeadRepository.findByOperationType(operationType);
    }

    public List<RateHead> getActiveRateHeadsByOperationType(VendorRole operationType) {
        return rateHeadRepository.findByOperationTypeAndActiveTrue(operationType);
    }

    public RateHead updateRateHead(Long id, RateHead updatedRateHead) {
        Optional<RateHead> existingRateHeadOpt = rateHeadRepository.findById(id);
        if (existingRateHeadOpt.isPresent()) {
            RateHead existing = existingRateHeadOpt.get();
            existing.setName(updatedRateHead.getName());
            existing.setOperationType(updatedRateHead.getOperationType());
            existing.setActive(updatedRateHead.isActive());
            return rateHeadRepository.save(existing);
        }
        return null;
    }

    @Transactional
    public void deleteRateHead(Long id) {
        log.info("deleteRateHead: id={}", id);
        RateHead rateHead = getRateHeadById(id);
        if (rateHead != null) {
            rateHeadPriceRepository.deleteByRateHead(rateHead);
            rateHeadRepository.delete(rateHead);
        }
    }

    public RateHead deactivateRateHead(Long id) {
        Optional<RateHead> rateHeadOpt = rateHeadRepository.findById(id);
        if (rateHeadOpt.isPresent()) {
            RateHead rateHead = rateHeadOpt.get();
            rateHead.setActive(false);
            return rateHeadRepository.save(rateHead);
        }
        return null;
    }

    // --- Price entries ---

    public List<RateHeadPrice> getPriceHistory(RateHead rateHead) {
        return rateHeadPriceRepository.findByRateHeadOrderByEffectiveFromDesc(rateHead);
    }

    /**
     * Adds a new price entry for a rate head and syncs the rate head's cost
     * to the latest (most recent effectiveFrom) entry.
     */
    @Transactional
    public RateHeadPrice addPriceEntry(Long rateHeadId, Double cost, LocalDate effectiveFrom) {
        log.info("addPriceEntry: rateHeadId={} cost={} effectiveFrom={}", rateHeadId, cost, effectiveFrom);
        RateHead rateHead = getRateHeadById(rateHeadId);
        if (rateHead == null) {
            throw new IllegalArgumentException("Rate head not found: " + rateHeadId);
        }
        RateHeadPrice price = new RateHeadPrice();
        price.setRateHead(rateHead);
        price.setCost(cost);
        price.setEffectiveFrom(effectiveFrom);
        rateHeadPriceRepository.save(price);
        syncCurrentCost(rateHead);
        return price;
    }

    @Transactional
    public RateHeadPrice updatePriceEntry(Long priceId, Double cost, LocalDate effectiveFrom) {
        log.info("updatePriceEntry: priceId={} cost={} effectiveFrom={}", priceId, cost, effectiveFrom);
        RateHeadPrice price = rateHeadPriceRepository.findById(priceId).orElse(null);
        if (price == null) {
            throw new IllegalArgumentException("Price entry not found: " + priceId);
        }
        price.setCost(cost);
        price.setEffectiveFrom(effectiveFrom);
        rateHeadPriceRepository.save(price);
        syncCurrentCost(price.getRateHead());
        return price;
    }

    @Transactional
    public void deletePriceEntry(Long priceId) {
        log.info("deletePriceEntry: priceId={}", priceId);
        RateHeadPrice price = rateHeadPriceRepository.findById(priceId).orElse(null);
        if (price == null) return;
        RateHead rateHead = price.getRateHead();
        rateHeadPriceRepository.delete(price);
        syncCurrentCost(rateHead);
    }

    /** Syncs the RateHead.cost field to the latest price entry (most recent effectiveFrom). */
    private void syncCurrentCost(RateHead rateHead) {
        List<RateHeadPrice> prices = rateHeadPriceRepository.findByRateHeadOrderByEffectiveFromDesc(rateHead);
        if (!prices.isEmpty()) {
            rateHead.setCost(prices.get(0).getCost());
        }
        rateHeadRepository.save(rateHead);
    }

    /**
     * Returns the cost for a rate head that was effective at the given date.
     * Looks up from the price history table. Falls back to the rate head's own cost (legacy data).
     */
    public Double getCostForDate(RateHead rateHead, LocalDate date) {
        if (rateHead == null || date == null) {
            return rateHead != null ? rateHead.getCost() : null;
        }
        List<RateHeadPrice> prices = rateHeadPriceRepository.findEffectivePrices(rateHead, date);
        if (!prices.isEmpty()) {
            Double cost = prices.get(0).getCost();
            log.debug("getCostForDate: rateHead={} date={} -> cost={} (from price entry effectiveFrom={})",
                    rateHead.getName(), date, cost, prices.get(0).getEffectiveFrom());
            return cost;
        }
        // No price entries (legacy rate head) — use the rate head's own cost field
        log.debug("getCostForDate: rateHead={} date={} -> cost={} (legacy fallback)", rateHead.getName(), date, rateHead.getCost());
        return rateHead.getCost();
    }
}
