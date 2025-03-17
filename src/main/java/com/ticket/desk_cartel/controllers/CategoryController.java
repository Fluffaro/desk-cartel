package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.services.CategoryService;
import jakarta.security.auth.message.AuthException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCategory(@RequestParam String name, @RequestParam String description) throws AuthException {
           return ResponseEntity.ok(categoryService.categoryCreation(name, description));
    }


}
