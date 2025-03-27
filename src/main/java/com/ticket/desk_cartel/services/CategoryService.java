package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.Category;
import com.ticket.desk_cartel.repositories.CategoryRepository;
import jakarta.security.auth.message.AuthException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;


    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category categoryCreation(String name, String description, int points, boolean isActive) throws AuthException {

        if(categoryRepository.findByName(name).isPresent()){
            throw new AuthException("Category name already taken");
        }

        Category category = new Category(name, description, points, isActive);

        return categoryRepository.save(category);






    }

    public List<Category> getAllCategories(){
        return categoryRepository.findAll();
    }

    /**
     * Get only active categories for users when creating tickets
     * @return List of active categories
     */
    public List<Category> getActiveCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    public Category deactivateCategory(Long id) throws AuthException {
        Optional<Category> existingCategory = categoryRepository.findById(id);
        if(existingCategory.isEmpty()){
            throw new AuthException("Category do not exist");
        }
        if(existingCategory.get().getIsActive() == false){
            throw new AuthException("Category is already deactivated");
        }

        existingCategory.get().setActive(false);

        Category category = existingCategory.get();
        return categoryRepository.save(category);


    }

    public Category getACategory(Long id) throws AuthException {
        if(categoryRepository.findById(id).isEmpty()){
            throw new AuthException("Category not found");
        }

        Category category = categoryRepository.findById(id).get();
        return category;
    }

    public Category updateCategory(Long id, String name, String description, boolean isActive, int points) throws AuthException {
        Optional<Category> existingCategory = categoryRepository.findById(id);
        if(existingCategory.isEmpty()){
            throw new AuthException("Category not found");
        }

        Category category = existingCategory.get();

        category.setName(name);
        category.setDescription(description);
        category.setPoints(points);
        category.setActive(isActive);
        return categoryRepository.save(category);
    }


}
