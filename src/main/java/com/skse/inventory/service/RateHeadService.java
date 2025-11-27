package com.skse.inventory.service;

import com.skse.inventory.model.RateHead;
import com.skse.inventory.model.VendorRole;
import com.skse.inventory.repository.RateHeadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RateHeadService {
    
    @Autowired
    private RateHeadRepository rateHeadRepository;
    
    public RateHead createRateHead(RateHead rateHead) {
        return rateHeadRepository.save(rateHead);
    }
    
    public List<RateHead> getAllRateHeads() {
        return rateHeadRepository.findAll();
    }
    
    public RateHead getRateHeadById(Long id) {
        Optional<RateHead> rateHead = rateHeadRepository.findById(id);
        return rateHead.orElse(null);
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
            RateHead existingRateHead = existingRateHeadOpt.get();
            existingRateHead.setName(updatedRateHead.getName());
            existingRateHead.setOperationType(updatedRateHead.getOperationType());
            existingRateHead.setCost(updatedRateHead.getCost());
            existingRateHead.setActive(updatedRateHead.isActive());
            return rateHeadRepository.save(existingRateHead);
        } else {
            return null;
        }
    }
    
    public void deleteRateHead(Long id) {
        rateHeadRepository.deleteById(id);
    }
    
    // Soft delete - mark as inactive instead of deleting
    public RateHead deactivateRateHead(Long id) {
        Optional<RateHead> rateHeadOpt = rateHeadRepository.findById(id);
        if (rateHeadOpt.isPresent()) {
            RateHead rateHead = rateHeadOpt.get();
            rateHead.setActive(false);
            return rateHeadRepository.save(rateHead);
        }
        return null;
    }
}

