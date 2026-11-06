package com.julius.resame.dto;

import com.julius.resame.model.AppUser;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter 
public class AppUserResponse {
    private Long userId;
    private String email;
    private AppUser.Role role;
    private LocalDateTime createdAt;

}
