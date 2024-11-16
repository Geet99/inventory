package com.skse.inventory.controller;

import com.skse.inventory.model.Article;
import com.skse.inventory.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/slipper-types")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @PostMapping
    public Article createSlipperType(@RequestBody Article article) {
        return articleService.addSlipperType(article);
    }

    @GetMapping
    public List<Article> getAllSlipperTypes() {
        return articleService.getAllSlipperTypes();
    }
}
