package com.urlshortener.dto;

import com.urlshortener.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    
    @NotBlank(message = "Name cannot be empty")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Must be a valid email address")
    private String email;
    private Role role;
    private String plan;
    private int active;
    private int deleted;
    private LocalDateTime createdAt;
    private LocalDateTime subscriptionExpiry;
}
