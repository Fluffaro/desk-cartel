package com.ticket.desk_cartel.repositories;

import com.ticket.desk_cartel.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    
    // Method to find all active categories
    List<Category> findByIsActiveTrue();
}
