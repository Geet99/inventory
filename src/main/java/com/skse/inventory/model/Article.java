package com.skse.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long articleId;

    private String articleName;
    private Double cuttingCost;
    private Double printingCost;
    private Double stitchingCost;
    private Double slipperCost;
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UpperStock> upperStocks = new ArrayList<>();
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinishedStock> finishedStocks = new ArrayList<>();
}
