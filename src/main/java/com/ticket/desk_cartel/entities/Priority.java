package com.ticket.desk_cartel.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing the priority levels of tickets.
 * Each priority has an associated weight (workload impact) and time limit in hours.
 * Priority levels can be configured by administrators.
 */
@Entity
@Table(name = "priority_levels")
public class Priority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Priority name is required")
    @Column(unique = true, nullable = false)
    private String name;
    
    @Min(value = 0, message = "Weight must be a positive number")
    private int weight;
    
    @Min(value = 0, message = "Time limit must be a positive number")
    @Column(name = "time_limit_hours")
    private int timeLimit;
    
    // Default constructor required by JPA
    public Priority() {
    }
    
    public Priority(String name, int weight, int timeLimit) {
        this.name = name;
        this.weight = weight;
        this.timeLimit = timeLimit;
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

    /**
     * Gets the weight of this priority level.
     * Used for calculating agent workload.
     * 
     * @return the priority weight
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Sets the weight for this priority level.
     * 
     * @param weight the priority weight to set
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * Gets the time limit in hours for tickets of this priority.
     * 
     * @return the time limit in hours
     */
    public int getTimeLimit() {
        return timeLimit;
    }

    /**
     * Sets the time limit in hours for tickets of this priority.
     * 
     * @param timeLimit the time limit in hours
     */
    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }
}
