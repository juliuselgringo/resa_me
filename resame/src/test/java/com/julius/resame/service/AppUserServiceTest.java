package com.julius.resame.service;

import com.julius.resame.model.AppUser;
import com.julius.resame.repository.AppUserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest 
public class AppUserServiceTest {

    @Autowired 
    private AppUserService appUserService;

    @MockBean
    private AppUserRepository appUserRepository;

    @Test 
    public void testCreateUser_ShouldThrowIfEmailExists() {
        // Arrange
        AppUser existingUser = new AppUser();
        existingUser.setEmail("test@example.com");

        when(appUserRepository.findByEmail("test@example.com"))
            .thenReturn(java.util.Optional.of(existingUser));

        AppUser newUser = new AppUser();
        newUser.setEmail("test@example.com");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> appUserService.createUser(newUser));
    }

    @Test
    public void testCreateUser_ShouldHashPasswordAndSave() {
        // Arrange
        AppUser newUser = new AppUser();
        newUser.setEmail("new@example.com");
        newUser.setPassword("rawPassword");

        when(appUserRepository.findByEmail("new@example.com"))
            .thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class)))
            .thenReturn(newUser);

        // Act
        AppUser created = appUserService.createUser(newUser);

        // Assert
        assertNotNull(created);
        assertNotEquals("rawPassword", created.getPassword()); // Vérifie que c'est hashé
        verify(appUserRepository, times(1)).save(newUser); // Vérifie que la méthode save est appelé 1 fois avec newUser
    }
    
}
