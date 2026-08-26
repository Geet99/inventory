package com.skse.inventory.service;

import com.skse.inventory.model.Color;
import com.skse.inventory.repository.ColorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ColorService {
    private static final Logger log = LoggerFactory.getLogger(ColorService.class);

    @Autowired
    private ColorRepository colorRepository;

    public Color addColor(Color color) {
        return colorRepository.save(color);
    }

    public List<Color> getAllColors() {
        return colorRepository.findAll();
    }

    public Color getColorById(Long id) {
        Optional<Color> color = colorRepository.findById(id);
        return color.orElse(null);
    }

    public Color updateColor(Long id, Color updatedColor) {
        Optional<Color> existingColorOpt = colorRepository.findById(id);
        if (existingColorOpt.isPresent()) {
            Color existingColor = existingColorOpt.get();
            existingColor.setName(updatedColor.getName());
            return colorRepository.save(existingColor);
        }
        return null;
    }

    public void deleteColor(Long id) {
        colorRepository.deleteById(id);
    }
}
