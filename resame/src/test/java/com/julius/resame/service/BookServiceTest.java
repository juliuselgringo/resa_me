package com.julius.resame.service;

import com.julius.resame.model.Book;
import com.julius.resame.repository.BookRepository;
import com.julius.resame.model.Bookable;
import com.julius.resame.model.AppUser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class BookServiceTest {
    
    @Autowired
    private BookService bookService;

    @MockBean 
    private BookRepository bookRepository;

    /**
    * CREATE BOOK
    */
    // dates de début et de fin invalides
    @Test 
    public void testCreateBook_ShouldThrowIfStartDateAfterEndDate() {
        // Arrange
        Book book = new Book();
        book.setStartDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        book.setEndDate(LocalDateTime.of(2027, 1, 1, 0, 0));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.createBook(book);
        });

    }

    // Vérifier que la quantité réservée ne dépasse pas la quantité disponible
    @Test 
    public void testCreateBook_ShouldThrowIfQuantityBookedExceedsAvailable() {
        // Arrange
        Book book = new Book();
        book.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        book.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        book.setQuantityBooked(10);

        Bookable bookable = new Bookable();
        bookable.setQuantity(5); // Available quantity is less than booked quantity
        book.setBookable(bookable);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.createBook(book);
        });

    }

    // Vérifier que la réservation est sauvegardée et que la quantité disponible est décrémentée
    @Test 
    public void testCreateBook_ShouldSaveBookAndDecrementQuantity() {
        // Arrange
        Book book = new Book();
        book.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        book.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        book.setQuantityBooked(3);

        Bookable bookable = new Bookable();
        bookable.setQuantity(5); // Available quantity is more than booked quantity
        book.setBookable(bookable);

        when(bookRepository.save(any(Book.class))).thenReturn(book);

        // Act
        Book createdBook = bookService.createBook(book);

        // Assert
        assertEquals(Book.Status.PENDING, createdBook.getStatus());
        assertEquals(2, bookable.getQuantity()); // Quantity should be decremented
        verify(bookRepository, times(1)).save(book); // Ensure save is called once
    }

    /**
     * UPDATE BOOK
     */
    // Vérifier que la réservation existe avant de la mettre à jour
    @Test
    public void testUpdateBook_ShouldThrowIfBookNotFound() {
        // Arrange
        Long bookId = 999L;
        
        Book updatedBook = new Book();
        updatedBook.setBookId(bookId);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.USER);
        updatedBook.setUser(currentUser);

        // Mock : Le book n'existe pas
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.updateBook(bookId, updatedBook, currentUser);
        });

    }

    // Vérifier que l'id du book correspond à l'id du book à mettre à jour
    @Test 
    public void testUpdateBook_ShouldThrowIfBookIdMismatch() {
        // Arrange
        Long bookId = 99L;

        Book updatedBook = new Book();
        updatedBook.setBookId(100L); // Mismatched ID

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.USER);
        updatedBook.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(updatedBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.updateBook(bookId, updatedBook, currentUser);
        });
    }
}
