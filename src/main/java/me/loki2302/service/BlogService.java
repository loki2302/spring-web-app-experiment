package me.loki2302.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.loki2302.dao.ArticleDao;
import me.loki2302.dao.CategoryDao;
import me.loki2302.dao.UserDao;
import me.loki2302.dao.rows.ArticleRow;
import me.loki2302.dao.rows.CategoryRow;
import me.loki2302.dao.rows.UserRow;
import me.loki2302.service.dto.Home;
import me.loki2302.service.dto.article.BriefArticle;
import me.loki2302.service.dto.article.CompleteArticle;
import me.loki2302.service.dto.article.ShortArticle;
import me.loki2302.service.dto.category.BriefCategory;
import me.loki2302.service.dto.category.CompleteCategory;
import me.loki2302.service.dto.category.ShortCategory;
import me.loki2302.service.dto.user.BriefUser;
import me.loki2302.service.dto.user.CompleteUser;
import me.loki2302.service.dto.user.ShortUser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlogService {
    @Autowired
    private UserDao userDao;
    
    @Autowired
    private CategoryDao categoryDao;
    
    @Autowired
    private ArticleDao articleDao;
    
    public UserRow createUser(String userName) {
        UserRow existingUser = userDao.findUserByUserName(userName);
        if(existingUser != null) {
            throw new RuntimeException("user already exists");
        }
        
        return userDao.createUser(userName);
    }
    
    public CompleteUser getUser(int userId) {
        UserRow userRow = userDao.getUser(userId);
        if(userRow == null) {
            throw new RuntimeException("no such user");
        }
        
        CompleteUser completeUser = makeCompleteUser(userRow);
        
        return completeUser;
    }
    
    public CategoryRow createCategory(String categoryName) {
        return categoryDao.createCategory(categoryName);
    }
    
    public List<ShortCategory> getCategories() {
        List<CategoryRow> allCategoryRows = categoryDao.getCategories();
        
        Set<Integer> categoryIds = extractIds(allCategoryRows);
        List<ArticleRow> articleRows = articleDao.getRecentArticlesForCategories(categoryIds, 3);
        
        Set<Integer> userIds = extractUserIds(articleRows);
        List<UserRow> userRows = userDao.getUsers(userIds);
        Map<Integer, BriefUser> briefUsersMap = makeBriefUsersMap(userRows);
        
        Map<Integer, BriefCategory> briefCategoriesMap = makeBriefCategoriesMap(allCategoryRows);
        
        List<ShortCategory> shortCategories = new ArrayList<ShortCategory>();
        for(CategoryRow categoryRow : allCategoryRows) {
            ShortCategory shortCategory = new ShortCategory();
            shortCategory.CategoryId = categoryRow.Id;
            shortCategory.Name = categoryRow.Name;
            
            //
            List<ArticleRow> articleRowsForThisCategory = new ArrayList<ArticleRow>();
            for(ArticleRow articleRow : articleRows) {
                if(articleRow.CategoryId != categoryRow.Id) {
                    continue;
                }
                
                articleRowsForThisCategory.add(articleRow);
            }
            //
            
            shortCategory.RecentArticles = makeBriefArticles(
                    articleRowsForThisCategory, 
                    briefUsersMap, 
                    briefCategoriesMap);
            
            shortCategories.add(shortCategory);
        }
        
        return shortCategories;
    }
    
    public ArticleRow createArticle(int userId, int categoryId, String title, String text) {
        return articleDao.createArticle(userId, categoryId, title, text, new Date());
    }
        
    public CompleteArticle getArticle(int articleId) {
        ArticleRow articleRow = articleDao.getArticle(articleId);
        if(articleRow == null) {
            throw new ArticleNotFoundException();
        }

        List<UserRow> userRows = Arrays.asList(userDao.getUser(articleRow.UserId));
        Map<Integer, BriefUser> briefUsersMap = makeBriefUsersMap(userRows);
        
        List<CategoryRow> categoryRows = Arrays.asList(categoryDao.getCategory(articleRow.CategoryId));
        Map<Integer, BriefCategory> briefCategoriesMaps = makeBriefCategoriesMap(categoryRows);
        
        CompleteArticle completeArticle = makeCompleteArticle(
                articleRow, 
                briefUsersMap, 
                briefCategoriesMaps);
                
        return completeArticle;
    } 
    
    public CompleteCategory getCategory(int categoryId) {
        CategoryRow categoryRow = categoryDao.getCategory(categoryId);
        if(categoryRow == null) {
            throw new CategoryNotFoundException();
        }
        
        List<ArticleRow> articleRows = articleDao.getArticlesByCategory(categoryId);        
        
        Set<Integer> userIds = extractUserIds(articleRows);        
        List<UserRow> userRows = userDao.getUsers(userIds);
        Map<Integer, BriefUser> briefUsersMap = makeBriefUsersMap(userRows);
        
        List<CategoryRow> categoryRows = Arrays.asList(categoryRow);
        Map<Integer, BriefCategory> briefCategoriesMaps = makeBriefCategoriesMap(categoryRows);
        
        List<ShortArticle> shortArticles = makeShortArticles(
                articleRows,
                briefUsersMap,
                briefCategoriesMaps);
        
        CompleteCategory completeCategory = makeCompleteCategory(
                categoryRow, 
                shortArticles);
        
        return completeCategory;
    }
    
    public Home getHome(int numberOfRecentArticles) {        
        Home home = new Home();
        
        List<CategoryRow> allCategoryRows = categoryDao.getCategories();
        home.Categories = makeBriefCategories(allCategoryRows);
        
        List<ArticleRow> articleRows = articleDao.getRecentArticles(numberOfRecentArticles);
            
        Set<Integer> userIds = extractUserIds(articleRows);
        List<UserRow> userRows = userDao.getUsers(userIds);
        Map<Integer, BriefUser> briefUsersMap = makeBriefUsersMap(userRows);
            
        Set<Integer> categoryIds = extractCategoryIds(articleRows);
        List<CategoryRow> categoryRows = categoryDao.getCategories(categoryIds);
        Map<Integer, BriefCategory> briefCategoriesMap = makeBriefCategoriesMap(categoryRows);
            
        home.MostRecentArticles = makeShortArticles(
                articleRows, 
                briefUsersMap, 
                briefCategoriesMap);
        
        return home;
    }
    
    private static Set<Integer> extractUserIds(Iterable<ArticleRow> articleRows) {
        Set<Integer> userIds = new HashSet<Integer>();
        for(ArticleRow articleRow : articleRows) {
            userIds.add(articleRow.UserId);
        }
        return userIds;
    }
    
    private static Set<Integer> extractCategoryIds(Iterable<ArticleRow> articleRows) {
        Set<Integer> userIds = new HashSet<Integer>();
        for(ArticleRow articleRow : articleRows) {
            userIds.add(articleRow.CategoryId);
        }
        return userIds;
    }
    
    private static Set<Integer> extractIds(Iterable<CategoryRow> categoryRows) {
        Set<Integer> userIds = new HashSet<Integer>();
        for(CategoryRow categoryRow : categoryRows) {
            userIds.add(categoryRow.Id);
        }
        return userIds;
    }
        
    private static CompleteArticle makeCompleteArticle(
            ArticleRow articleRow, 
            Map<Integer, BriefUser> briefUsersMap, 
            Map<Integer, BriefCategory> briefCategoriesMap) {
        CompleteArticle completeArticle = new CompleteArticle();
        completeArticle.ArticleId = articleRow.Id;
        completeArticle.Title = articleRow.Title;
        completeArticle.Text = articleRow.Text;
        completeArticle.CreatedAt = articleRow.CreatedAt;
        completeArticle.UpdatedAt = articleRow.UpdatedAt;        
        completeArticle.User = briefUsersMap.get(articleRow.UserId);                        
        completeArticle.Category = briefCategoriesMap.get(articleRow.CategoryId);        
        return completeArticle;
    }
    
    private static List<ShortArticle> makeShortArticles(
            List<ArticleRow> articleRows,
            Map<Integer, BriefUser> briefUsersMap, 
            Map<Integer, BriefCategory> briefCategoriesMap) {
        List<ShortArticle> shortArticles = new ArrayList<ShortArticle>();
        for(ArticleRow articleRow : articleRows) {
            ShortArticle shortArticle = makeShortArticle(
                    articleRow,
                    briefUsersMap,
                    briefCategoriesMap);
            shortArticles.add(shortArticle);
        }
        return shortArticles;
    }
    
    private static ShortArticle makeShortArticle(
            ArticleRow articleRow,
            Map<Integer, BriefUser> briefUsersMap, 
            Map<Integer, BriefCategory> briefCategoriesMap) {
        ShortArticle shortArticle = new ShortArticle();
        shortArticle.ArticleId = articleRow.Id;
        shortArticle.Title = articleRow.Title;
        shortArticle.FirstParagraph = articleRow.Text.split("\n\n")[0];
        shortArticle.CreatedAt = articleRow.CreatedAt;
        shortArticle.UpdatedAt = articleRow.UpdatedAt;        
        shortArticle.User = briefUsersMap.get(articleRow.UserId);                        
        shortArticle.Category = briefCategoriesMap.get(articleRow.CategoryId);        
        return shortArticle;
    }
    
    private static List<BriefArticle> makeBriefArticles(
            List<ArticleRow> articleRows,
            Map<Integer, BriefUser> briefUsersMap, 
            Map<Integer, BriefCategory> briefCategoriesMap) {
        List<BriefArticle> briefArticles = new ArrayList<BriefArticle>();
        for(ArticleRow articleRow : articleRows) {
            BriefArticle briefArticle = makeBriefArticle(
                    articleRow,
                    briefUsersMap,
                    briefCategoriesMap);
            briefArticles.add(briefArticle);
        }
        return briefArticles;
    }
    
    private static BriefArticle makeBriefArticle(
            ArticleRow articleRow,
            Map<Integer, BriefUser> briefUsersMap, 
            Map<Integer, BriefCategory> briefCategoriesMap) {
        BriefArticle briefArticle = new BriefArticle();        
        briefArticle.ArticleId = articleRow.Id;
        briefArticle.Title = articleRow.Title;
        briefArticle.CreatedAt = articleRow.CreatedAt;
        briefArticle.UpdatedAt = articleRow.UpdatedAt;
        briefArticle.User = briefUsersMap.get(articleRow.UserId);
        briefArticle.Category = briefCategoriesMap.get(articleRow.CategoryId);        
        return briefArticle;
    }
    
    private static Map<Integer, BriefUser> makeBriefUsersMap(List<UserRow> userRows) {
        Map<Integer, BriefUser> usersMap = new HashMap<Integer, BriefUser>();
        for(UserRow userRow : userRows) {
            BriefUser briefUser = makeBriefUser(userRow);
            usersMap.put(briefUser.UserId, briefUser);
        }
        return usersMap;
    }
    
    private static Map<Integer, BriefCategory> makeBriefCategoriesMap(List<CategoryRow> categoryRows) {
        Map<Integer, BriefCategory> categoriesMap = new HashMap<Integer, BriefCategory>();
        for(CategoryRow categoryRow : categoryRows) {
            BriefCategory briefCategory = makeBriefCategory(categoryRow);
            categoriesMap.put(briefCategory.CategoryId, briefCategory);
        }
        return categoriesMap;
    }
    
    private static BriefUser makeBriefUser(UserRow userRow) {
        BriefUser user = new BriefUser();
        user.UserId = userRow.Id;
        user.Name = userRow.Name;
        return user;
    }
    
    private static ShortUser makeShortUser(UserRow userRow) {
        throw new RuntimeException();
    }
    
    private static CompleteUser makeCompleteUser(UserRow userRow) {
        CompleteUser user = new CompleteUser();
        user.UserId = userRow.Id;
        user.Name = userRow.Name;
        return user;
    }
    
    private static List<BriefCategory> makeBriefCategories(List<CategoryRow> categoryRows) {
        List<BriefCategory> briefCategories = new ArrayList<BriefCategory>();
        for(CategoryRow categoryRow : categoryRows) {
            BriefCategory briefCategory = makeBriefCategory(categoryRow);
            briefCategories.add(briefCategory);
        }        
        return briefCategories;
    }
    
    private static BriefCategory makeBriefCategory(CategoryRow categoryRow) {
        BriefCategory category = new BriefCategory();
        category.CategoryId = categoryRow.Id;
        category.Name = categoryRow.Name;            
        return category;
    }
    
    private static ShortCategory makeShortCategory(CategoryRow categoryRow) {
        throw new RuntimeException();
    }
    
    private static CompleteCategory makeCompleteCategory(
            CategoryRow categoryRow, 
            List<ShortArticle> shortArticles) {
        CompleteCategory category = new CompleteCategory();
        category.CategoryId = categoryRow.Id;
        category.Name = categoryRow.Name;
        category.Articles = shortArticles;
        return category;
    }
}