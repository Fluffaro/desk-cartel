package com.ticket.desk_cartel.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for ticket sorting operations.
 * Allows sorting by different fields with a specified direction.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ticket sorting parameters")
public class TicketSortDTO {

    @Schema(description = "Field to sort by", example = "priority", required = true, 
            allowableValues = {"ticketId", "title", "status", "priority", "category", "date_started", "completion_date"})
    private String sortBy = "ticketId";

    @Schema(description = "Sort direction", example = "ASC", required = true, 
            allowableValues = {"ASC", "DESC"})
    private String direction = "ASC";
} 