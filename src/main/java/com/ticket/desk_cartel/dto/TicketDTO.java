package com.ticket.desk_cartel.dto;

import com.ticket.desk_cartel.entities.Priority;
import com.ticket.desk_cartel.entities.Status;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class TicketDTO {

    private String title;
    private String description;
    private Priority priority;
    private Status status;
    private String category;


}
