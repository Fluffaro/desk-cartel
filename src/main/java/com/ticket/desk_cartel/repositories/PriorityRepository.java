package com.ticket.desk_cartel.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ticket.desk_cartel.entities.Priority;

import java.util.Optional;

/**
 * Repository for Priority entity providing CRUD operations
 */
@Repository
public interface PriorityRepository extends JpaRepository<Priority, Long> {
    
    /**
     * Find a priority level by its name
     * 
     * @param name the priority name to search for
     * @return an Optional containing the found Priority or empty if not found
     */
    Optional<Priority> findByName(String name);
    
    /**
     * Check if a priority level with the given name exists
     * 
     * @param name the priority name to check
     * @return true if the priority exists, false otherwise
     */
    boolean existsByName(String name);
} 