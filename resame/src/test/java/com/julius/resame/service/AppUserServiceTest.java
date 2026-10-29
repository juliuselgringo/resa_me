package com.julius.resame.service;

import com.julius.resame.model.AppUser;
import com.julius.resame.model.Book;
import com.julius.resame.repository.AppUserRepository;
import com.julius.resame.repository.BookRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest 
public class AppUserServiceTest {

    @Autowired 
    private AppUserService appUserService;

    @MockBean
    private AppUserRepository appUserRepository;

    @MockBean 
    private  BookRepository bookRepository;

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

    @Test 
    public void testVerifyAccess_ShouldThrowIfIdMismatch() {
        // Arrange
        Long userId = 1L;
        AppUser targetUser = new AppUser();
        targetUser.setUserId(2L); // != userId
        AppUser  currentUser = new AppUser();
        currentUser.setUserId(1L); 
        currentUser.setRole(AppUser.Role.ADMIN); // droit admin

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> appUserService.updateUser(userId, targetUser, currentUser));
    }

    @Test
    public void testVerifyAccess_ShouldThrowIfInsufficientPermissions() {
        // Arrange
        Long userId = 1L;
        AppUser targetUser = new AppUser();
        targetUser.setUserId(1L);  // ← Même ID que userId
        AppUser currentUser = new AppUser();
        currentUser.setUserId(2L);  // ← Différent de userId
        currentUser.setRole(AppUser.Role.USER);  // ← Pas ADMIN !

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> appUserService.updateUser(userId, targetUser, currentUser));
    }

    @Test
    public void testUpdateUser_ShouldSucceedIfAccessValid() {
        // Arrange
        Long userId = 1L;
        AppUser targetUser = new AppUser();
        targetUser.setUserId(1L);  // ← Même ID
        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);  // ← Même user
        currentUser.setRole(AppUser.Role.USER);

        // Mock le repository pour éviter les appels réels
        when(appUserRepository.save(targetUser)).thenReturn(targetUser);

        // Act
        AppUser result = appUserService.updateUser(userId, targetUser, currentUser);

        // Assert
        assertEquals(targetUser, result);
        verify(appUserRepository, times(1)).save(targetUser);
    }

    @Test 
    public void testDeleteUser_ShouldReassignBooksToAnonymousUser() {
        // Arrange
        Long userId = 1L;
        AppUser targetUser = new AppUser();
        targetUser.setUserId(1L);  // ← Même ID
        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);  // ← Même user
        currentUser.setRole(AppUser.Role.USER);

        Book book = new Book();
        book.setUser(targetUser);
        List<Book> userBooks = List.of(book);

        // Mock
        when(appUserRepository.findByEmail("deleted+user@resame.com"))
            .thenReturn(Optional.empty());
        when(bookRepository.findByUserUserId(userId))
            .thenReturn(userBooks);
        when(appUserRepository.save(any(AppUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        appUserService.deleteUser(userId, targetUser, currentUser);

        // Assert
        // Vérifie création anonymous user
        verify(appUserRepository, times(1)).findByEmail("deleted+user@resame.com");
        verify(appUserRepository,times(1)).save(any(AppUser.class));

        // Vérifie la reassignation du book à anonymous user
        assertEquals("deleted+user@resame.com", book.getUser().getEmail());

    }

    @Test
    public void testDeleteUser_ShouldUseExistingAnonymousUser() {
        // Arrange
        Long userId = 1L;
        AppUser targetUser = new AppUser();
        targetUser.setUserId(userId);
        AppUser currentUser = new AppUser();
        currentUser.setUserId(userId);
        currentUser.setRole(AppUser.Role.USER);

        Book book = new Book();
        book.setUser(targetUser);
        List<Book> userBooks = List.of(book);

        AppUser anonymousUser = new AppUser();
        anonymousUser.setEmail("deleted+user@resame.com");

        // Mock : L'utilisateur anonyme existe déjà
        when(appUserRepository.findByEmail("deleted+user@resame.com"))
            .thenReturn(Optional.of(anonymousUser));
        when(bookRepository.findByUserUserId(userId))
            .thenReturn(userBooks);

        // Act
        appUserService.deleteUser(userId, targetUser, currentUser);

        // Assert
        verify(appUserRepository, times(1)).findByEmail("deleted+user@resame.com");
        verify(appUserRepository, never()).save(any(AppUser.class)); // ← Pas de création
        assertEquals("deleted+user@resame.com", book.getUser().getEmail());
    }
    
}
