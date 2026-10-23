package com.julius.resame.service;

import com.julius.resame.repository.AppUserRepository;
import com.julius.resame.model.AppUser;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Service 
@RequiredArgsConstructor 
public class AppUserService {
    private final AppUserRepository appUserRepository;

    @Transactional 
    public AppUser createUser(AppUser appUser) {
        // vérifier si l'utilisateur existe déjà
        if (appUserRepository.findByEmail(appUser.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email " + appUser.getEmail() + " already exists");
        }
        // hasher le mot de passe avant de le sauvegarder (à implémenter)
        return appUserRepository.save(appUser);
    }

    public Optional<AppUser> getUserByEmail(String email) {
        return appUserRepository.findByEmail(email);
    }

}
