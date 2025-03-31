package com.mobicommServices3.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long userId;
    private String username;
    private String email;
    private String role; // Role enum will be serialized as a string
    private LocalDateTime createdAt;
    private Boolean isActive;
}