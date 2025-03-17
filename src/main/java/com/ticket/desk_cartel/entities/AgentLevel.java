package com.ticket.desk_cartel.entities;

/**
 * Enum representing the different levels of agents and their capacities.
 * Junior agents have a capacity of 10.
 * Mid-level agents have a capacity of 20.
 * Senior agents have a capacity of 50.
 */
public enum AgentLevel {
    JUNIOR(10),
    MID(20),
    SENIOR(50);

    private final int capacity;

    AgentLevel(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }
} 