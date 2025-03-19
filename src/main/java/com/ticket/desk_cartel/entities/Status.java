package com.ticket.desk_cartel.entities;

/**
 * Enum representing the different statuses a ticket can have.
 * NO_AGENT_AVAILABLE - No suitable agent is available for assignment
 * ASSIGNED - Ticket is assigned to an agent but work hasn't started yet
 * NOT_YET_STARTED - Legacy status, similar to ASSIGNED (to be deprecated)
 * ONGOING - Agent has started working on the ticket
 * COMPLETED - Ticket has been resolved
 */
public enum Status {
    NO_AGENT_AVAILABLE, // No agent with enough capacity is available
    ASSIGNED,           // Assigned to an agent but not started
    NOT_YET_STARTED,    // Legacy status (to be deprecated)
    ONGOING,            // Work has started
    COMPLETED           // Work is completed
}