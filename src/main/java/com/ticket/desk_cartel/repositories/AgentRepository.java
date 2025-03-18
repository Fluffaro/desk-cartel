package com.ticket.desk_cartel.repositories;

import com.ticket.desk_cartel.entities.Agent;
import com.ticket.desk_cartel.entities.AgentLevel;
import com.ticket.desk_cartel.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByUser(User user);
    
    Optional<Agent> findByUserId(Long userId);
    
    List<Agent> findByLevel(AgentLevel level);

    // Custom query to find agents with available capacity
    @Query("SELECT a FROM Agent a WHERE a.currentWorkload < a.capacity")
    List<Agent> findAvailableAgents();
    
    // Alternative method using Spring Data naming convention
    List<Agent> findByCurrentWorkloadLessThan(int capacity);

} 