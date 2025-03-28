package com.ticket.desk_cartel.repositories;

import com.ticket.desk_cartel.entities.Agent;
import com.ticket.desk_cartel.entities.AgentLevel;
import com.ticket.desk_cartel.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByUser(User user);
    
    Optional<Agent> findByUser_Id(Long userId);
    
    List<Agent> findByLevel(AgentLevel level);

    // Custom query to find agents with available capacity
    @Query("SELECT a FROM Agent a WHERE a.currentWorkload < a.totalCapacity AND a.isActive = true")
    List<Agent> findAvailableAgents();
    
    // Find agents that have enough capacity for a specific ticket weight
    @Query("SELECT a FROM Agent a WHERE (a.totalCapacity - a.currentWorkload) >= :weight AND a.isActive = true")
    List<Agent> findAgentsWithEnoughCapacityFor(@Param("weight") int weight);
    
    // Alternative method using Spring Data naming convention
    List<Agent> findByCurrentWorkloadLessThanAndIsActiveTrue(int totalCapacity);

    // Find all inactive agents
    @Query("SELECT a FROM Agent a WHERE a.isActive = false")
    List<Agent> findByIsActiveFalse();

    /**
     * Custom query to find agents by user_id.
     *
     * @param userId the user ID to search by.
     * @return a list of agents associated with the given user ID.
     */
    @Query("SELECT a FROM Agent a WHERE a.user.id = :userId")
    Optional<Agent> findByUserId(@Param("userId") Long userId);

} 