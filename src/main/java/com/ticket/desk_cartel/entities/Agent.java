package com.ticket.desk_cartel.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing a support agent in the system.
 * Each agent has a level (JUNIOR, MID, SENIOR) which determines their capacity.
 * Capacity is calculated as: baseCapacity + (completedTickets / 5)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "agents")
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentLevel level;
    
    // Base capacity determined by level
    private int baseCapacity;
    
    // Track completed tickets for level progression and bonus capacity
    private int completedTickets = 0;
    
    // Track total performance points earned by the agent
    private int totalPerformancePoints = 0;

    // Total capacity calculated from base + bonus
    private int totalCapacity;

    // Current workload based on assigned tickets
    private int currentWorkload = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean isActive = true;

    /**
     * Creates a new agent with the specified level.
     * The capacity is automatically set based on the level.
     * 
     * @param user The user associated with this agent
     * @param level The agent's experience level
     */
    public Agent(User user, AgentLevel level) {
        this.user = user;
        this.level = level;
        this.baseCapacity = level.getBaseCapacity();
        this.totalCapacity = this.baseCapacity; // Initially no bonus
        this.currentWorkload = 0;
        this.completedTickets = 0;
        this.totalPerformancePoints = 0;
    }

    /**
     * Updates the agent's level, recalculates capacity and checks for level upgrade.
     */
    public void recalculateLevel() {
        // Get the appropriate level based on completed tickets
        AgentLevel newLevel = AgentLevel.calculateLevel(this.completedTickets);
        
        // Update level if changed
        if (this.level != newLevel) {
            this.level = newLevel;
            this.baseCapacity = newLevel.getBaseCapacity();
        }
        
        // Recalculate total capacity
        this.totalCapacity = this.level.calculateTotalCapacity(this.completedTickets);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Increases the completed tickets count and recalculates level and capacity.
     */
    public void incrementCompletedTickets() {
        this.completedTickets++;
        recalculateLevel();
    }
    
    /**
     * Adds performance points earned from a completed ticket and increments
     * the completed tickets count.
     * 
     * @param points the performance points earned
     */
    public void addCompletedTicketWithPoints(int points) {
        this.completedTickets++;
        this.totalPerformancePoints += points;
        recalculateLevel();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Increases the current workload by the given amount.
     * 
     * @param weight the amount to increase the workload by
     */
    public void addWorkload(int weight) {
        this.currentWorkload += weight;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Decreases the current workload by the given amount.
     * 
     * @param weight the amount to decrease the workload by
     */
    public void reduceWorkload(int weight) {
        this.currentWorkload = Math.max(0, this.currentWorkload - weight);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Checks if the agent has enough capacity for the given workload.
     * 
     * @param weight the workload to check
     * @return true if the agent has enough capacity
     */
    public boolean hasCapacityFor(int weight) {
        return (this.currentWorkload + weight) <= this.totalCapacity;
    }

    /**
     * Sets the active status of this agent.
     * 
     * @param active the new active status
     */
    public void setActive(boolean active) {
        this.isActive = active;
        this.updatedAt = LocalDateTime.now();
    }
} 