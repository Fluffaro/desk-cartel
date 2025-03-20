package com.ticket.desk_cartel.entities;

/**
 * Enum representing the priority levels of tickets.
 * Each priority has an associated weight (workload impact) and time limit in hours.
 */
public enum Priority {
    NOT_ASSIGNED(0, 0),       // Default state before classification
    LOW(10, 4),               // Weight: 10, Time limit: 4 hours
    MEDIUM(20, 8),            // Weight: 20, Time limit: 8 hours
    HIGH(30, 24),             // Weight: 30, Time limit: 24 hours
    CRITICAL(40, 48);         // Weight: 40, Time limit: 48 hours

    private final int weight;
    private final int timeLimit;

    Priority(int weight, int timeLimit) {
        this.weight = weight;
        this.timeLimit = timeLimit;
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
     * Gets the time limit in hours for tickets of this priority.
     * 
     * @return the time limit in hours
     */
    public int getTimeLimit() {
        return timeLimit;
    }
}
