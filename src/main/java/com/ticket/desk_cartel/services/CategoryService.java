package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.Category;
import com.ticket.desk_cartel.repositories.CategoryRepository;
import jakarta.security.auth.message.AuthException;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;


    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category categoryCreation(String name, String description) throws AuthException {

        if(categoryRepository.findByName(name).isPresent()){
            throw new AuthException("Category name already taken");
        }

        Category category = new Category(name, description);

        return categoryRepository.save(category);




    }
}
