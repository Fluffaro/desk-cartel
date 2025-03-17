package com.ticket.desk_cartel.dto;

import lombok.*;

import java.util.Date;

@Getter
@Setter
public class RegisterRequestDTO {
    private String username;
    private String email;
    private String fullName;
    private String password;
    private Date dateOfBirth;
    private String phoneNumber;
    private String address;
}