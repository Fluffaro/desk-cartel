package com.ticket.desk_cartel.repositories;

import com.ticket.desk_cartel.entities.Agent;
import com.ticket.desk_cartel.entities.AgentLevel;
import com.ticket.desk_cartel.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByUser(User user);
    
    Optional<Agent> findByUserId(Long userId);
    
    List<Agent> findByLevel(AgentLevel level);

    List<Agent> findByCurrentWorkloadLessThan(int capacity);  // ✅ Correct method

} 