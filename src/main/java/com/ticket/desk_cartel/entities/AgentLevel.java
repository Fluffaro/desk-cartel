package com.ticket.desk_cartel.entities;

/**
 * Enum representing the different levels of agents and their capacities.
 * Each level has a base capacity and a completion threshold to advance to the next level.
 * Actual capacity is calculated as: baseCapacity + (completedTickets / 10)
 */
public enum AgentLevel {
    JUNIOR(10, 0, 49),      // Base capacity: 10, Level range: 0-49 completed tickets
    MID(20, 50, 99),        // Base capacity: 20, Level range: 50-99 completed tickets
    SENIOR(50, 100, Integer.MAX_VALUE); // Base capacity: 50, Level range: 100+ completed tickets

    private final int baseCapacity;
    private final int minTickets;
    private final int maxTickets;

    AgentLevel(int baseCapacity, int minTickets, int maxTickets) {
        this.baseCapacity = baseCapacity;
        this.minTickets = minTickets;
        this.maxTickets = maxTickets;
    }

    /**
     * Gets the base capacity for this agent level.
     * The actual capacity will be calculated as baseCapacity + (completedTickets / 10)
     * 
     * @return the base capacity value
     */
    public int getBaseCapacity() {
        return baseCapacity;
    }
    
    /**
     * Gets the minimum number of completed tickets required for this level.
     * 
     * @return the minimum tickets threshold
     */
    public int getMinTickets() {
        return minTickets;
    }
    
    /**
     * Gets the maximum number of completed tickets for this level.
     * 
     * @return the maximum tickets threshold
     */
    public int getMaxTickets() {
        return maxTickets;
    }
    
    /**
     * Calculate the appropriate level for the given number of completed tickets.
     * 
     * @param completedTickets the number of completed tickets
     * @return the appropriate agent level
     */
    public static AgentLevel calculateLevel(int completedTickets) {
        for (AgentLevel level : values()) {
            if (completedTickets >= level.getMinTickets() && 
                completedTickets <= level.getMaxTickets()) {
                return level;
            }
        }
        return SENIOR; // Default to highest level if for some reason no match found
    }
    
    /**
     * Calculate the total capacity for an agent based on their level and completed tickets.
     * Formula: baseCapacity + (completedTickets / 10)
     * 
     * @param completedTickets the number of completed tickets
     * @return the total capacity
     */
    public int calculateTotalCapacity(int completedTickets) {
        return baseCapacity + (completedTickets / 5);
    }
} 