package com.skse.inventory.controller;

import com.skse.inventory.model.Article;
import com.skse.inventory.model.VendorRole;
import com.skse.inventory.service.ArticleService;
import com.skse.inventory.service.RateHeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/articles")
public class ArticleViewController {

    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private RateHeadService rateHeadService;

    @GetMapping
    public String listArticles(Model model) {
        model.addAttribute("title", "Articles");
        model.addAttribute("articles", articleService.getAllArticles());
        return "articles/list";
    }

    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("title", "Add Article");
        model.addAttribute("article", new Article());
        model.addAttribute("cuttingRateHeads", rateHeadService.getActiveRateHeadsByOperationType(VendorRole.Cutting));
        model.addAttribute("printingRateHeads", rateHeadService.getActiveRateHeadsByOperationType(VendorRole.Printing));
        model.addAttribute("stitchingRateHeads", rateHeadService.getActiveRateHeadsByOperationType(VendorRole.Stitching));
        return "articles/add";
    }

    @PostMapping
    public String saveArticle(@ModelAttribute Article article, 
                             @RequestParam(required = false) Long cuttingRateHeadId,
                             @RequestParam(required = false) Long printingRateHeadId,
                             @RequestParam(required = false) Long stitchingRateHeadId) {
        // Set rate heads if provided
        if (cuttingRateHeadId != null) {
            article.setCuttingRateHead(rateHeadService.getRateHeadById(cuttingRateHeadId));
        }
        if (printingRateHeadId != null) {
            article.setPrintingRateHead(rateHeadService.getRateHeadById(printingRateHeadId));
        }
        if (stitchingRateHeadId != null) {
            article.setStitchingRateHead(rateHeadService.getRateHeadById(stitchingRateHeadId));
        }
        articleService.createArticle(article);
        return "redirect:/articles";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Article article = articleService.getArticleById(id);
        if (article == null) {
            return "redirect:/articles";
        }
        model.addAttribute("title", "Edit Article");
        model.addAttribute("article", article);
        model.addAttribute("cuttingRateHeads", rateHeadService.getActiveRateHeadsByOperationType(VendorRole.Cutting));
        model.addAttribute("printingRateHeads", rateHeadService.getActiveRateHeadsByOperationType(VendorRole.Printing));
        model.addAttribute("stitchingRateHeads", rateHeadService.getActiveRateHeadsByOperationType(VendorRole.Stitching));
        return "articles/edit";
    }

    @PostMapping("/update/{id}")
    public String updateArticle(@PathVariable Long id, @ModelAttribute Article article,
                               @RequestParam(required = false) Long cuttingRateHeadId,
                               @RequestParam(required = false) Long printingRateHeadId,
                               @RequestParam(required = false) Long stitchingRateHeadId) {
        // Set rate heads if provided
        if (cuttingRateHeadId != null) {
            article.setCuttingRateHead(rateHeadService.getRateHeadById(cuttingRateHeadId));
        }
        if (printingRateHeadId != null) {
            article.setPrintingRateHead(rateHeadService.getRateHeadById(printingRateHeadId));
        }
        if (stitchingRateHeadId != null) {
            article.setStitchingRateHead(rateHeadService.getRateHeadById(stitchingRateHeadId));
        }
        articleService.updateArticle(id, article);
        return "redirect:/articles";
    }

    @PostMapping("/delete/{id}")
    public String deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return "redirect:/articles";
    }
}
