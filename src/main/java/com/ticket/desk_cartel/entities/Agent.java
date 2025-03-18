package com.ticket.desk_cartel.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing a support agent in the system.
 * Each agent has a level (JUNIOR, MID, SENIOR) which determines their capacity.
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

    // Capacity is determined by level but stored for quick reference
    private int capacity;

    // Derived field that can be calculated based on assigned tickets
    private int currentWorkload;

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
        this.capacity = level.getCapacity();
        this.currentWorkload = 0;
    }

    /**
     * Updates the agent's level and adjusts capacity accordingly.
     * 
     * @param level The new agent level
     */
    public void updateLevel(AgentLevel level) {
        this.level = level;
        this.capacity = level.getCapacity();
        this.updatedAt = LocalDateTime.now();
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }
} 