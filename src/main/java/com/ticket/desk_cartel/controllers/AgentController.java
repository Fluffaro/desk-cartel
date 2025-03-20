package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.entities.Agent;
import com.ticket.desk_cartel.entities.AgentLevel;
import com.ticket.desk_cartel.entities.Ticket;
import com.ticket.desk_cartel.repositories.AgentRepository;
import com.ticket.desk_cartel.services.AgentService;
import com.ticket.desk_cartel.services.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing agent operations and assignments.
 */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;
    private final TicketService ticketService;
    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    public AgentController(AgentService agentService, TicketService ticketService) {
        this.agentService = agentService;
        this.ticketService = ticketService;
    }

    /**
     * Get all agents.
     * 
     * @return List of all agents with their stats
     */
    @GetMapping
    public ResponseEntity<List<Agent>> getAllAgents() {
        return ResponseEntity.ok(agentService.getAllAgents());
    }

    /**
     * Get an agent by ID.
     * 
     * @param id The agent ID
     * @return The agent or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Agent> getAgentById(@PathVariable Long id) {
        Optional<Agent> agent = agentService.getAgentById(id);
        return agent.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all tickets assigned to an agent.
     * 
     * @param id The agent ID
     * @return List of tickets assigned to the agent
     */
    @GetMapping("/{id}/tickets")
    public ResponseEntity<List<Ticket>> getAgentTickets(@PathVariable Long id) {
        Optional<Agent> agent = agentService.getAgentById(id);
        if (agent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ticketService.getTicketsByAgent(id));
    }

    /**
     * Create a new agent from a user.
     * 
     * @param userId The user ID to convert to an agent
     * @return The newly created agent
     */
    @PostMapping
    public ResponseEntity<?> createAgent(@RequestParam Long userId) {
        try {
            Agent agent = agentService.createAgent(userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(agent);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Set an agent's active status.
     * 
     * @param id The agent ID
     * @param active The new active status
     * @return The updated agent or 404 if not found
     */
    @PutMapping("/{id}/active")
    public ResponseEntity<?> setAgentActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        Optional<Agent> agent = agentService.setAgentActiveStatus(id, active);
        return agent.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get statistics for an agent.
     * 
     * @param id The agent ID
     * @return Agent statistics
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getAgentStats(@PathVariable Long id) {
        Optional<Agent> agentOpt = agentService.getAgentById(id);
        if (agentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Agent agent = agentOpt.get();
        List<Ticket> agentTickets = ticketService.getTicketsByAgent(id);
        
        long completedTickets = agent.getCompletedTickets();
        long assignedTickets = agentTickets.size();
        long ongoingTickets = agentTickets.stream()
                .filter(t -> t.getStatus() == com.ticket.desk_cartel.entities.Status.ONGOING)
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("agentId", agent.getId());
        stats.put("name", agent.getUser().getUsername());
        stats.put("level", agent.getLevel());
        stats.put("baseCapacity", agent.getBaseCapacity());
        stats.put("totalCapacity", agent.getTotalCapacity());
        stats.put("currentWorkload", agent.getCurrentWorkload());
        stats.put("completedTickets", completedTickets);
        stats.put("assignedTickets", assignedTickets);
        stats.put("ongoingTickets", ongoingTickets);
        stats.put("availableCapacity", agent.getTotalCapacity() - agent.getCurrentWorkload());
        stats.put("nextLevelAt", agent.getLevel() != AgentLevel.SENIOR 
                ? AgentLevel.calculateLevel(agent.getCompletedTickets() + 1).getMinTickets()
                : "Max level reached");
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get agent leaderboard ranked by performance points.
     * 
     * @return List of agents sorted by total performance points
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getAgentLeaderboard() {
        List<Agent> allAgents = agentService.getAllAgents();
        
        // Sort agents by performance points (highest first)
        List<Agent> sortedAgents = allAgents.stream()
                .sorted(Comparator.comparing(Agent::getTotalPerformancePoints).reversed())
                .toList();
        
        // Convert to a simplified format for the leaderboard
        List<Map<String, Object>> leaderboard = sortedAgents.stream()
                .map(agent -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("agentId", agent.getId());
                    entry.put("name", agent.getUser().getUsername());
                    entry.put("level", agent.getLevel());
                    entry.put("totalPoints", agent.getTotalPerformancePoints());
                    entry.put("completedTickets", agent.getCompletedTickets());
                    entry.put("rank", sortedAgents.indexOf(agent) + 1);
                    return entry;
                })
                .toList();
        
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Get all tickets assigned to an agent identified by user ID.
     * 
     * @param userId The user ID
     * @return List of tickets assigned to the agent or 404 if user is not an agent
     */
    @GetMapping("/by-user/{userId}/tickets")
    public ResponseEntity<List<Ticket>> getAgentTicketsByUser(@PathVariable Long userId) {
        Optional<Agent> agent = agentService.findAgentByUserId(userId);
        if (agent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ticketService.getTicketsByAgent(agent.get().getId()));
    }

    /**
     * Start work on a ticket by user ID.
     * 
     * @param userId The user ID of the agent
     * @param ticketId The ticket ID
     * @param token The JWT authorization token
     * @return The updated ticket or error response
     */
    @PostMapping("/by-user/{userId}/tickets/{ticketId}/start")
    public ResponseEntity<?> startTicket(
            @PathVariable Long userId,
            @PathVariable Long ticketId,
            @RequestHeader("Authorization") String token) {
        token = token.replace("Bearer ", "");
        
        // Look up the agent associated with this user
        Optional<Agent> agent = agentService.findAgentByUserId(userId);
        if (agent.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User is not an agent.");
            return ResponseEntity.badRequest().body(error);
        }
        
        Ticket updatedTicket = ticketService.startTicket(ticketId, agent.get().getId(), token);
        
        if (updatedTicket == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Could not start ticket. Ensure the ticket is assigned to you and in ASSIGNED status.");
            return ResponseEntity.badRequest().body(error);
        }
        
        return ResponseEntity.ok(updatedTicket);
    }

    @PostMapping("/notifCount/{id}")
    public ResponseEntity<?> getNotifCount(@PathVariable Long id){
        Optional<Agent> agentOpt = agentRepository.findById(id);

        Agent agentNotif = agentOpt.get();

        return ResponseEntity.ok(agentNotif.getNotifCount());
    }
} 