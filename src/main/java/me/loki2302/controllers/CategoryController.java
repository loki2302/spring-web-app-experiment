package me.loki2302.controllers;

import me.loki2302.service.CategoryService;
import me.loki2302.service.dto.category.CompleteCategory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;
    
    @RequestMapping
    public String listOfCategories(Model model) {
        model.addAttribute("categories", categoryService.getCategories(3));            
        return "category/index";
    }
    
    @RequestMapping("{categoryId}")
    public String category(
            @PathVariable("categoryId") int categoryId, 
            Integer articlesPerPage,
            Integer page,
            Model model) {
        
        CompleteCategory category = categoryService.getCategory(
                categoryId, 
                articlesPerPage == null ? 5 : articlesPerPage, 
                page == null ? 0 : page);
        
        model.addAttribute("category", category);
        return "category/category";
    }
}
