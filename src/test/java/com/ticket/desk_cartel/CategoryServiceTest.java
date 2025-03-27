package com.ticket.desk_cartel;

import com.ticket.desk_cartel.entities.Category;
import com.ticket.desk_cartel.repositories.CategoryRepository;
import com.ticket.desk_cartel.services.CategoryService;
import jakarta.security.auth.message.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient; // Add this import

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        lenient().when(categoryRepository.findByName("Existing Category"))
                .thenReturn(Optional.of(new Category("Existing Category", "Description", 10, true)));

        lenient().when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(new Category("Sample Category", "Desc", 5, true)));

        lenient().when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());
    }

    @Test
    void shouldCreateCategorySuccessfully() throws AuthException {
        when(categoryRepository.findByName("New Category")).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Category category = categoryService.categoryCreation("New Category", "Description", 10, true);

        assertNotNull(category);
        assertEquals("New Category", category.getName());
        verify(categoryRepository, times(1)).save(any());
    }

    @Test
    void shouldThrowErrorWhenCreatingDuplicateCategory() {
        assertThrows(AuthException.class, () -> {
            categoryService.categoryCreation("Existing Category", "Description", 10, true);
        });
    }

    @Test
    void shouldDeactivateCategorySuccessfully() throws AuthException {
        Category category = new Category("Sample Category", "Desc", 5, true); // Create a category
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category)); // Mock its existence
        when(categoryRepository.save(any())).thenReturn(category); // Ensure save() works properly

        Category updatedCategory = categoryService.deactivateCategory(1L);

        assertNotNull(updatedCategory); // Ensure it is not null
        assertFalse(updatedCategory.getIsActive()); // Check if it was deactivated
        verify(categoryRepository, times(1)).save(any());
    }


    @Test
    void shouldThrowErrorWhenDeactivatingNonExistentCategory() {
        assertThrows(AuthException.class, () -> categoryService.deactivateCategory(99L));
    }
}
