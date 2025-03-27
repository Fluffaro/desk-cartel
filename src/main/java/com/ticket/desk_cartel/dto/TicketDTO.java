package com.ticket.desk_cartel.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ticket.desk_cartel.entities.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for ticket creation and updates.
 * Uses simple identifiers instead of full entity objects for better API usability.
 * Only title, description, and categoryId are required for creating tickets.
 * Priority will be determined by AI if not specified.
 * Status will default to ASSIGNED if not specified.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ticket creation data - only title, description and categoryId are required")
public class TicketDTO {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    @Schema(description = "Ticket title", example = "Cannot connect to the internet", required = true)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, message = "Description must be at least 10 characters")
    @Schema(description = "Detailed description of the issue", example = "My internet connection stopped working after restarting my computer", required = true)
    private String description;
    
    @Schema(description = "Priority ID (optional - AI will determine if omitted)", required = false, example = "2")
    private Long priorityId;
    
    @Schema(description = "Ticket status (optional - defaults to ASSIGNED)", required = false, example = "ASSIGNED")
    private Status status;
    
    @NotNull(message = "Category ID is required")
    @Schema(description = "Category ID of the ticket", example = "1", required = true)
    private Long categoryId;

}
