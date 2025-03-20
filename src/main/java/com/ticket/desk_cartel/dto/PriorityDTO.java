package com.ticket.desk_cartel.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for Priority entity.
 * Used for API requests and responses.
 */
public class PriorityDTO {
    
    private Long id;
    
    @NotBlank(message = "Priority name is required")
    private String name;
    
    @Min(value = 0, message = "Weight must be a positive number")
    private int weight;
    
    @Min(value = 0, message = "Time limit must be a positive number")
    private int timeLimitHours;
    
    public PriorityDTO() {
    }
    
    public PriorityDTO(Long id, String name, int weight, int timeLimitHours) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.timeLimitHours = timeLimitHours;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public int getTimeLimitHours() {
        return timeLimitHours;
    }
    
    public void setTimeLimitHours(int timeLimitHours) {
        this.timeLimitHours = timeLimitHours;
    }
} 