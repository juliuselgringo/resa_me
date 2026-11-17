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

    public AppUserResponse(AppUser appUser){
        this.userId = appUser.getUserId();
        this.email = appUser.getEmail();
        this.role = appUser.getRole();
        this.createdAt = appUser.getCreatedAt();
    }

}
