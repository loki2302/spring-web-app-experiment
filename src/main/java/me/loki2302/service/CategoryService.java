package me.loki2302.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.loki2302.dao.ArticleDao;
import me.loki2302.dao.CategoryDao;
import me.loki2302.dao.rows.ArticleRow;
import me.loki2302.dao.rows.CategoryRow;
import me.loki2302.dao.rows.Page;
import me.loki2302.service.dto.article.BriefArticle;
import me.loki2302.service.dto.article.ShortArticle;
import me.loki2302.service.dto.category.BriefCategory;
import me.loki2302.service.dto.category.CompleteCategory;
import me.loki2302.service.dto.category.ShortCategory;
import me.loki2302.service.exceptions.CategoryNotFoundException;
import me.loki2302.service.mappers.ArticleMapper;
import me.loki2302.service.mappers.BriefCategoryMapper;
import me.loki2302.service.mappers.CompleteCategoryMapper;
import me.loki2302.service.mappers.ShortCategoryMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {    
    @Autowired
    private CategoryDao categoryDao;
    
    @Autowired
    private ArticleDao articleDao;
                
    @Autowired
    private BriefCategoryMapper briefCategoryMapper;
    
    @Autowired
    private ArticleMapper articleMapper;
    
    @Autowired
    private ShortCategoryMapper shortCategoryMapper;
        
    @Autowired
    private CompleteCategoryMapper completeCategoryMapper;
            
    public int createCategory(String categoryName) {
        return categoryDao.createCategory(categoryName);
    }
    
    public List<BriefCategory> getBriefCategories() {
        List<CategoryRow> allCategoryRows = categoryDao.getCategories();
        return briefCategoryMapper.makeBriefCategories(allCategoryRows);
    }
    
    public List<ShortCategory> getCategories(int numberOfRecentArticles) {
        List<CategoryRow> categoryRows = categoryDao.getCategories();
        
        Set<Integer> categoryIds = extractCategoryIdsFromCategoryRows(categoryRows);
        
        List<ArticleRow> articleRows = articleDao.getRecentArticlesForCategories(
                categoryIds, 
                numberOfRecentArticles);
        
        List<BriefArticle> briefArticles = articleMapper.makeBriefArticles(articleRows);
        
        List<ShortCategory> shortCategories = shortCategoryMapper.makeShortCategories(
                categoryRows, 
                briefArticles);
        
        return shortCategories;
    }
    
    public CompleteCategory getCategory(int categoryId, int articlesPerPage, int page) {
        CategoryRow categoryRow = categoryDao.getCategory(categoryId);
        if(categoryRow == null) {
            throw new CategoryNotFoundException();
        }
        
        Page<ArticleRow> articleRowsPage = articleDao.getArticlesByCategory(
                categoryId, 
                articlesPerPage, 
                page);
                
        List<ShortArticle> shortArticles = articleMapper.makeShortArticles(articleRowsPage.Items);
        
        Page<ShortArticle> pageData = new Page<ShortArticle>();
        pageData.NumberOfItems = articleRowsPage.NumberOfItems;
        pageData.ItemsPerPage = articleRowsPage.ItemsPerPage;
        pageData.NumberOfPages = articleRowsPage.NumberOfPages;
        pageData.CurrentPage = articleRowsPage.CurrentPage;
        pageData.Items = shortArticles;
        
        CompleteCategory completeCategory = completeCategoryMapper.makeCompleteCategory(
                categoryRow, 
                pageData);
        
        return completeCategory;
    }
    
    private static Set<Integer> extractCategoryIdsFromCategoryRows(Iterable<CategoryRow> categoryRows) {
        Set<Integer> categoryIds = new HashSet<Integer>();
        for(CategoryRow categoryRow : categoryRows) {
            categoryIds.add(categoryRow.Id);
        }
        return categoryIds;
    }
}