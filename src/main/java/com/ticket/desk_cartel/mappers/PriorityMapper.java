package com.ticket.desk_cartel.mappers;

import org.springframework.stereotype.Component;

import com.ticket.desk_cartel.dto.PriorityDTO;
import com.ticket.desk_cartel.entities.Priority;

/**
 * Mapper class to convert between Priority entity and PriorityDTO.
 */
@Component
public class PriorityMapper {

    /**
     * Converts a Priority entity to a PriorityDTO.
     * 
     * @param priority the entity to convert
     * @return the converted DTO
     */
    public PriorityDTO toDto(Priority priority) {
        if (priority == null) {
            return null;
        }
        
        return new PriorityDTO(
                priority.getId(),
                priority.getName(),
                priority.getWeight(),
                priority.getTimeLimit());
    }
    
    /**
     * Converts a PriorityDTO to a Priority entity.
     * 
     * @param dto the DTO to convert
     * @return the converted entity
     */
    public Priority toEntity(PriorityDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Priority priority = new Priority();
        priority.setId(dto.getId());
        priority.setName(dto.getName());
        priority.setWeight(dto.getWeight());
        priority.setTimeLimit(dto.getTimeLimitHours());
        
        return priority;
    }
    
    /**
     * Updates a Priority entity with values from a PriorityDTO.
     * 
     * @param entity the entity to update
     * @param dto the DTO with new values
     * @return the updated entity
     */
    public Priority updateEntityFromDto(Priority entity, PriorityDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }
        
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        
        entity.setWeight(dto.getWeight());
        entity.setTimeLimit(dto.getTimeLimitHours());
        
        return entity;
    }
} 