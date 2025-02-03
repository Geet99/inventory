package com.skse.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

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

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public String getArticleName() {
        return articleName;
    }

    public void setArticleName(String articleName) {
        this.articleName = articleName;
    }

    public Double getCuttingCost() {
        return cuttingCost;
    }

    public void setCuttingCost(Double cuttingCost) {
        this.cuttingCost = cuttingCost;
    }

    public Double getPrintingCost() {
        return printingCost;
    }

    public void setPrintingCost(Double printingCost) {
        this.printingCost = printingCost;
    }

    public Double getStitchingCost() {
        return stitchingCost;
    }

    public void setStitchingCost(Double stitchingCost) {
        this.stitchingCost = stitchingCost;
    }

    public Double getSlipperCost() {
        return slipperCost;
    }

    public void setSlipperCost(Double slipperCost) {
        this.slipperCost = slipperCost;
    }
}
