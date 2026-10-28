package com.julius.resame.service;

import com.julius.resame.repository.AppUserRepository;
import com.julius.resame.model.AppUser;
import com.julius.resame.model.Book;
import com.julius.resame.repository.BookRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;

import java.util.Optional;
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
    public AppUser createUser(AppUser appUser) {
        // vérifier si l'utilisateur existe déjà
        if (appUserRepository.findByEmail(appUser.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email " + appUser.getEmail() + " already exists");
        }
        // hasher le mot de passe avant de le sauvegarder
        appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));
        return appUserRepository.save(appUser);
    }

    // Méthode pour vérifier les droits d'accès et la correspondance des IDs
    private void verifyAccess(Long userId, AppUser targetUser, AppUser currentUser) {
        // vérifier que userId correspond à targetUser
        if (!userId.equals(targetUser.getUserId())){
            throw new IllegalArgumentException("User ID mismatch");
        }
        if (!userId.equals(currentUser.getUserId()) && !currentUser.getRole().equals(AppUser.Role.ADMIN)) {
            throw new IllegalArgumentException("Insufficient permissions");
        }
    }

    // UPDATE USER
    @Transactional
    public AppUser updateUser(Long userId, AppUser updatedUser, AppUser currentUser) {
        // vérifier les droits et correspondance des IDs
        verifyAccess(userId, updatedUser, currentUser);
        
        appUserRepository.save(updatedUser);
        return updatedUser;
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
    public void deleteUser(Long userId, AppUser deletedUser, AppUser currentUser) {
        // vérifier les droits et correspondance des IDs
        verifyAccess(userId, deletedUser, currentUser);

        // Créer ou récupérer l'utilisateur anonyme
        AppUser anonymousUser = getOrCreateAnonymousUser();

        // réaffecter toutes les réservations (Book) à l'utilisateur anonyme
        List<Book> userBooks = bookRepository.findByUserUserId(userId);
        userBooks.forEach(book -> book.setUser(anonymousUser));

        appUserRepository.deleteById(userId);
    }

    // GETTER
    // GET USER BY ID
    public Optional<AppUser> getUserById(Long userId) {
        return appUserRepository.findById(userId);
    }

    // USER BY EMAIL
    public Optional<AppUser> getUserByEmail(String email) {
        return appUserRepository.findByEmail(email);
    }

    // GET ALL USERS
    public List<AppUser> getAllUsers() {
        return appUserRepository.findAll();
    }

}
