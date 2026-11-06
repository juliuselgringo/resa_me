package com.julius.resame.dto;

import com.julius.resame.model.AppUser;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter 
@Setter 
@NoArgsConstructor 
public class AppUserRequest {
    @NotNull @Email private String email;

    @NotNull @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    private AppUser.Role role = AppUser.Role.USER;
}
