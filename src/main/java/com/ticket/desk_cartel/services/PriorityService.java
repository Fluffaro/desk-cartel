package com.ticket.desk_cartel.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ticket.desk_cartel.entities.Priority;
import com.ticket.desk_cartel.exceptions.ResourceAlreadyExistsException;
import com.ticket.desk_cartel.exceptions.ResourceNotFoundException;
import com.ticket.desk_cartel.repositories.PriorityRepository;

/**
 * Service class for managing Priority entities.
 * Provides CRUD operations and business logic for priority levels.
 */
@Service
public class PriorityService {

    private final PriorityRepository priorityRepository;

    @Autowired
    public PriorityService(PriorityRepository priorityRepository) {
        this.priorityRepository = priorityRepository;
    }

    /**
     * Initialize default priority levels if none exist.
     * Called during application startup.
     */
    @Transactional
    public void initializeDefaultPriorities() {
        if (priorityRepository.count() == 0) {
            // Create default priority levels
            priorityRepository.save(new Priority("NOT_ASSIGNED", 0, 0));
            priorityRepository.save(new Priority("LOW", 10, 4));
            priorityRepository.save(new Priority("MEDIUM", 20, 8));
            priorityRepository.save(new Priority("HIGH", 30, 24));
            priorityRepository.save(new Priority("CRITICAL", 40, 48));
        }
    }

    /**
     * Get all priority levels.
     * 
     * @return list of all priority levels
     */
    public List<Priority> getAllPriorities() {
        return priorityRepository.findAll();
    }

    /**
     * Get a priority level by ID.
     * 
     * @param id the ID of the priority to find
     * @return the found priority level
     * @throws ResourceNotFoundException if the priority with the given ID does not exist
     */
    public Priority getPriorityById(Long id) {
        return priorityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Priority not found with id: " + id));
    }

    /**
     * Get a priority level by name.
     * 
     * @param name the name of the priority to find
     * @return the found priority level
     * @throws ResourceNotFoundException if the priority with the given name does not exist
     */
    public Priority getPriorityByName(String name) {
        return priorityRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Priority not found with name: " + name));
    }

    /**
     * Create a new priority level.
     * 
     * @param priority the priority to create
     * @return the created priority
     * @throws ResourceAlreadyExistsException if a priority with the same name already exists
     */
    @Transactional
    public Priority createPriority(Priority priority) {
        if (priorityRepository.existsByName(priority.getName())) {
            throw new ResourceAlreadyExistsException("Priority already exists with name: " + priority.getName());
        }
        return priorityRepository.save(priority);
    }

    /**
     * Update an existing priority level.
     * 
     * @param id the ID of the priority to update
     * @param priorityDetails the updated priority details
     * @return the updated priority
     * @throws ResourceNotFoundException if the priority with the given ID does not exist
     * @throws ResourceAlreadyExistsException if changing the name and a priority with the new name already exists
     */
    @Transactional
    public Priority updatePriority(Long id, Priority priorityDetails) {
        Priority priority = getPriorityById(id);
        
        // Check if name is being changed and if the new name already exists
        if (!priority.getName().equals(priorityDetails.getName()) && 
            priorityRepository.existsByName(priorityDetails.getName())) {
            throw new ResourceAlreadyExistsException("Priority already exists with name: " + priorityDetails.getName());
        }
        
        priority.setName(priorityDetails.getName());
        priority.setWeight(priorityDetails.getWeight());
        priority.setTimeLimit(priorityDetails.getTimeLimit());
        
        return priorityRepository.save(priority);
    }

    /**
     * Delete a priority level.
     * 
     * @param id the ID of the priority to delete
     * @throws ResourceNotFoundException if the priority with the given ID does not exist
     */
    @Transactional
    public void deletePriority(Long id) {
        if (!priorityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Priority not found with id: " + id);
        }
        priorityRepository.deleteById(id);
    }
} 