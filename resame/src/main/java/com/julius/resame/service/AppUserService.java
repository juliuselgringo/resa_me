package com.julius.resame.service;

import com.julius.resame.repository.AppUserRepository;
import com.julius.resame.model.AppUser;
import com.julius.resame.dto.AppUserRequest;
import com.julius.resame.dto.AppUserResponse;
import com.julius.resame.dto.AppUserUpdateRequest;
import com.julius.resame.model.Book;
import com.julius.resame.repository.BookRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service 
@RequiredArgsConstructor 
public class AppUserService {
    private final AppUserRepository appUserRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;
    private static final String ANONYMOUS_USER_EMAIL = "deleted+user@resame.com";

    // CREATE USER avec vérification de l'existence et hachage du mot de passe
    @Transactional 
    public AppUserResponse createUser(AppUserRequest appUserRequest) {
        // vérifier si l'utilisateur existe déjà
        if (appUserRepository.findByEmail(appUserRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email " + appUserRequest.getEmail() + " already exists");
        }

        // Création du user à partir de la requête
        AppUser newUser = new AppUser();
        newUser.setEmail(appUserRequest.getEmail());
        newUser.setRole(appUserRequest.getRole());

        // hasher le mot de passe avant de le sauvegarder
        newUser.setPassword(passwordEncoder.encode(appUserRequest.getPassword()));

        // Sauvegarder le nouvel user
        newUser = appUserRepository.save(newUser);

        return new AppUserResponse(newUser);
    }

    // Méthode pour vérifier les droits d'accès
    private void verifyAccess(Long userId, AppUser currentUser) {
        if (!userId.equals(currentUser.getUserId()) && !currentUser.getRole().equals(AppUser.Role.ADMIN)) {
            throw new IllegalArgumentException("Insufficient permissions");
        }
    }

    // UPDATE USER
    @Transactional
    public AppUserResponse updateUser(Long userId, AppUserUpdateRequest appUserUpdateRequest, AppUser currentUser) {
        // récupération du user à partir de userId
        AppUser userToUpdate = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " does not exist"));

        // vérifier les droits
        verifyAccess(userId, currentUser);

        if (appUserUpdateRequest.getEmail() != null) {
            userToUpdate.setEmail(appUserUpdateRequest.getEmail());
        }
        if (appUserUpdateRequest.getPassword() != null) {
            userToUpdate.setPassword(passwordEncoder.encode(appUserUpdateRequest.getPassword()));
        }
        
        appUserRepository.save(userToUpdate);
        return new AppUserResponse(userToUpdate);
    }

    // Méthode pour récupérer ou créer un utilisateur anonyme
    private AppUser getOrCreateAnonymousUser() {
        return appUserRepository.findByEmail(ANONYMOUS_USER_EMAIL)
                .orElseGet(() -> {
                    AppUser newAnonymousUser = new AppUser();
                    newAnonymousUser.setEmail(ANONYMOUS_USER_EMAIL);
                    newAnonymousUser.setPassword(passwordEncoder.encode("anonymous"));
                    newAnonymousUser.setRole(AppUser.Role.USER);
                    return appUserRepository.save(newAnonymousUser);
                });
    }

    // DELETE USER
    @Transactional
    public void deleteUser(Long userId, AppUser currentUser) {
        // vérifier les droits
        verifyAccess(userId, currentUser);

        // Créer ou récupérer l'utilisateur anonyme
        AppUser anonymousUser = getOrCreateAnonymousUser();

        // réaffecter toutes les réservations (Book) à l'utilisateur anonyme
        List<Book> userBooks = bookRepository.findByUserUserId(userId);
        userBooks.forEach(book -> book.setUser(anonymousUser));

        appUserRepository.deleteById(userId);
    }

    // GETTER
    // GET ALL USERS
    public List<AppUserResponse> getAllUsers() {
        return appUserRepository.findAll().stream()
                .map(AppUserResponse::new)
                .toList();
    }

    // GET USER BY ID
    public AppUserResponse getUserById(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " does not exist"));
        return new AppUserResponse(user);
    }

    // USER BY EMAIL
    public AppUserResponse getUserByEmail(String email) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with email " + email + " does not exist"));
        return new AppUserResponse(user);
    }

}
