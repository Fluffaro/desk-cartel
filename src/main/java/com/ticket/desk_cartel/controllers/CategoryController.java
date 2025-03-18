package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.entities.Category;
import com.ticket.desk_cartel.services.CategoryService;
import jakarta.security.auth.message.AuthException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public ResponseEntity<?> getAllCategory(){
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveCategories(){
        return ResponseEntity.ok(categoryService.getActiveCategories());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCategory(@RequestBody Category category) throws AuthException {
        return ResponseEntity.ok(categoryService.categoryCreation(category.getName(), category.getDescription(), category.getPoints(), category.getIsActive()));
    }

    @PutMapping("/deactivate")
    public ResponseEntity<?> deactivateCategory(@RequestParam Long id) throws AuthException {
        return ResponseEntity.ok(categoryService.deactivateCategory(id));

    }

    @GetMapping("/getCategory")
    public ResponseEntity<?> getACategory(@RequestParam Long id) throws AuthException {
        return ResponseEntity.ok(categoryService.getACategory(id));

    }

    @PutMapping("/updateCategory")
    public ResponseEntity<?> updateCategory(@RequestParam Long id, @RequestBody Category category) throws AuthException {
        return ResponseEntity.ok(categoryService.updateCategory(id, category.getName(), category.getDescription(),category.getIsActive(), category.getPoints()));
    }

}

// Create a separate controller for public category access
@RestController
@RequestMapping("/api/categories")
class PublicCategoryController {
    private final CategoryService categoryService;

    public PublicCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // Public endpoint for users to get active categories when creating tickets
    @GetMapping("/active")
    public ResponseEntity<?> getActiveCategories(){
        return ResponseEntity.ok(categoryService.getActiveCategories());
    }
}
