package com.ticket.desk_cartel.controllers.admin;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticket.desk_cartel.dto.PriorityDTO;
import com.ticket.desk_cartel.entities.Priority;
import com.ticket.desk_cartel.mappers.PriorityMapper;
import com.ticket.desk_cartel.services.PriorityService;

import jakarta.validation.Valid;

/**
 * REST controller for admin operations on Priority entities.
 * Provides endpoints for CRUD operations on priority levels.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/priorities")
@PreAuthorize("hasRole('ADMIN')")
public class PriorityAdminController {

    private final PriorityService priorityService;
    private final PriorityMapper priorityMapper;

    @Autowired
    public PriorityAdminController(PriorityService priorityService, PriorityMapper priorityMapper) {
        this.priorityService = priorityService;
        this.priorityMapper = priorityMapper;
    }

    /**
     * Get all priority levels.
     * 
     * @return list of all priority levels as DTOs
     */
    @GetMapping
    public ResponseEntity<List<PriorityDTO>> getAllPriorities() {
        List<Priority> priorities = priorityService.getAllPriorities();
        List<PriorityDTO> priorityDTOs = priorities.stream()
                .map(priorityMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(priorityDTOs);
    }

    /**
     * Get a priority level by ID.
     * 
     * @param id the ID of the priority to find
     * @return the found priority level as DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<PriorityDTO> getPriorityById(@PathVariable Long id) {
        Priority priority = priorityService.getPriorityById(id);
        PriorityDTO priorityDTO = priorityMapper.toDto(priority);
        return ResponseEntity.ok(priorityDTO);
    }

    /**
     * Create a new priority level.
     * 
     * @param priorityDTO the priority to create as DTO
     * @return the created priority as DTO
     */
    @PostMapping
    public ResponseEntity<PriorityDTO> createPriority(@Valid @RequestBody PriorityDTO priorityDTO) {
        Priority priority = priorityMapper.toEntity(priorityDTO);
        Priority createdPriority = priorityService.createPriority(priority);
        PriorityDTO createdPriorityDTO = priorityMapper.toDto(createdPriority);
        return new ResponseEntity<>(createdPriorityDTO, HttpStatus.CREATED);
    }

    /**
     * Update an existing priority level.
     * 
     * @param id the ID of the priority to update
     * @param priorityDTO the updated priority details as DTO
     * @return the updated priority as DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<PriorityDTO> updatePriority(
            @PathVariable Long id, 
            @Valid @RequestBody PriorityDTO priorityDTO) {
        Priority priority = priorityMapper.toEntity(priorityDTO);
        Priority updatedPriority = priorityService.updatePriority(id, priority);
        PriorityDTO updatedPriorityDTO = priorityMapper.toDto(updatedPriority);
        return ResponseEntity.ok(updatedPriorityDTO);
    }

    /**
     * Delete a priority level.
     * 
     * @param id the ID of the priority to delete
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePriority(@PathVariable Long id) {
        priorityService.deletePriority(id);
        return ResponseEntity.noContent().build();
    }
} 