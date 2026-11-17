package com.julius.resame.service;

import com.julius.resame.model.Book;
import com.julius.resame.repository.BookRepository;
import com.julius.resame.repository.BookableRepository;
import com.julius.resame.model.Bookable;
import com.julius.resame.dto.BookRequest;
import com.julius.resame.dto.BookResponse;
import com.julius.resame.dto.BookUpdateRequest;
import com.julius.resame.model.AppUser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@SpringBootTest
public class BookServiceTest {
    
    @Autowired
    private BookService bookService;

    @MockBean 
    private BookRepository bookRepository;

    @MockBean 
    private BookableRepository bookableRepository;

    /**
    * CREATE BOOK
    */
    // dates de début et de fin invalides
    @Test 
    public void testCreateBook_ShouldThrowIfStartDateAfterEndDate() {
        // Arrange
        Bookable bookable = new Bookable();
        bookable.setBookableId(1L);
        bookable.setQuantity(10);

        BookRequest bookRequest = new BookRequest();
        bookRequest.setBookableId(bookable.getBookableId());
        bookRequest.setStartDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        bookRequest.setEndDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        bookRequest.setQuantityBooked(2);

        AppUser user = new AppUser();
        user.setEmail("example@test.com");
        user.setRole(AppUser.Role.ADMIN);

        // Mock
        when(bookableRepository.findById(any(Long.class)))
            .thenReturn(Optional.of(bookable));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.createBook(bookRequest, user);
        });

    }

    // Vérifier que la quantité réservée ne dépasse pas la quantité disponible
    @Test 
    public void testCreateBook_ShouldThrowIfQuantityBookedExceedsAvailable() {
        // Arrange
        Bookable bookable = new Bookable();
        bookable.setBookableId(1L);
        bookable.setQuantity(5); // Available quantity is less than booked quantity

        BookRequest bookRequest = new BookRequest();
        bookRequest.setBookableId(bookable.getBookableId());
        bookRequest.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        bookRequest.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        bookRequest.setQuantityBooked(10);

        AppUser user = new AppUser();
        user.setEmail("example@test.com");
        user.setRole(AppUser.Role.ADMIN);

        // Mock
        when(bookableRepository.findById(any(Long.class)))
            .thenReturn(Optional.of(bookable));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.createBook(bookRequest, user);
        });

    }

    // Vérifier que la réservation est sauvegardée et que la quantité disponible est décrémentée
    @Test 
    public void testCreateBook_ShouldSaveBookAndDecrementQuantity() {
        // Arrange
        Book book = new Book();
        book.setBookId(1L);

        Bookable bookable = new Bookable();
        bookable.setBookableId(1L);
        bookable.setQuantity(4);

        BookRequest bookRequest = new BookRequest();
        bookRequest.setBookableId(bookable.getBookableId());
        bookRequest.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        bookRequest.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        bookRequest.setQuantityBooked(2);

        AppUser user = new AppUser();
        user.setEmail("example@test.com");
        user.setRole(AppUser.Role.ADMIN);

        // Mock
        when(bookableRepository.findById(any(Long.class)))
            .thenReturn(Optional.of(bookable));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        // Act
        BookResponse bookResponse = bookService.createBook(bookRequest, user);

        // Assert
        assertEquals(Book.Status.PENDING, bookResponse.getStatus());
        assertEquals(2, bookable.getQuantity()); // Quantity should be decremented
        verify(bookRepository, times(1)).save(any(Book.class)); // Ensure save is called once
    }

    /**
     * UPDATE BOOK
     */
    // Vérifier que la réservation existe avant de la mettre à jour
    @Test
    public void testUpdateBook_ShouldThrowIfBookNotFound() {
        // Arrange
        Long bookId = 999L;

        BookUpdateRequest bookUpdateRequest = new BookUpdateRequest();

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.USER);

        // Mock : Le book n'existe pas
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.updateBook(bookId, bookUpdateRequest, currentUser);
        });

    }

    // Vérifier que l'utilisateur a les droits pour mettre à jour la réservation
    @Test
    public void testUpdateBook_ShouldThrowIfUserNotAuthorized() {
        // Arrange
        Long bookId = 1L;

        Book existingBook = new Book();
        existingBook.setBookId(bookId);

        BookUpdateRequest bookUpdateRequest = new BookUpdateRequest();

        AppUser bookOwner = new AppUser();
        bookOwner.setUserId(2L); // Owner of the book
        existingBook.setUser(bookOwner);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L); // Different user
        currentUser.setRole(AppUser.Role.USER);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.updateBook(bookId, bookUpdateRequest, currentUser);
        });
    }

    // Vérifier qu'un user classique ne peut modifier que les réservations dont le status est PENDING
    @Test
    public void testUpdateBook_ShouldThrowIfNonAdminUpdatesNonPending() {
        // Arrange
        Long bookId = 1L;

        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setStatus(Book.Status.APPROVED); // Non-PENDING status

        BookUpdateRequest bookUpdateRequest = new BookUpdateRequest();

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.USER);
        existingBook.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.updateBook(bookId, bookUpdateRequest, currentUser);
        });
    }

    // Vérifier que la date de début est avant la date de fin lors de la mise à jour
    @Test
    public void testUpdateBook_ShouldThrowIfStartDateAfterEndDate() {
        // Arrange
        Long bookId = 1L;

        Book existingBook = new Book();
        existingBook.setBookId(bookId);

        BookUpdateRequest bookUpdateRequest = new BookUpdateRequest();
        bookUpdateRequest.setStartDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        bookUpdateRequest.setEndDate(LocalDateTime.of(2027, 1, 1, 0, 0)); // Invalid dates

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin can update any book
        existingBook.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.updateBook(bookId, bookUpdateRequest, currentUser);
        });
    }

    // Vérifier que la quantité réservée ne dépasse pas la quantité disponible lors de la mise à jour
    @Test
    public void testUpdateBook_ShouldThrowIfQuantityBookedExceedsAvailable() {
        // Arrange
        Long bookId = 1L;

        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setQuantityBooked(10);
        
        BookUpdateRequest bookUpdateRequest = new BookUpdateRequest();
        bookUpdateRequest.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        bookUpdateRequest.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        bookUpdateRequest.setQuantityBooked(16); // Booked quantity

        Bookable bookable = new Bookable();
        bookable.setQuantity(5); // Available quantity is less than booked quantity
        existingBook.setBookable(bookable);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin can update any book
        existingBook.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.updateBook(bookId, bookUpdateRequest, currentUser);
        });
    }

    // Vérifier que la réservation est sauvegardée et que la quantité disponible est décrémentée lors de la mise à jour
    @Test
    public void testUpdateBook_ShouldSaveBookAndDecrementQuantity() {
        // Arrange
        Long bookId = 1L;

        Bookable bookable = new Bookable();
        bookable.setQuantity(4);

        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        existingBook.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        existingBook.setQuantityBooked(1); // ancienne quantité réservée
        existingBook.setBookable(bookable);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN);
        existingBook.setUser(currentUser);

        BookUpdateRequest bookUpdateRequest = new BookUpdateRequest();
        bookUpdateRequest.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        bookUpdateRequest.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        bookUpdateRequest.setQuantityBooked(3); // nouvelle quantité

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act
        bookService.updateBook(bookId, bookUpdateRequest, currentUser);

        // Assert
        assertEquals(2, bookable.getQuantity()); // 4 + 1 - 3 = 2
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    // Vérifier que admin peut mettre à jour un status APPROVED
    @Test
    public void testUpdateBook_ShouldSucceedIfAdminUpdatesApprovedBook() {
        // Arrange
        Long bookId = 1L;

        Bookable bookable = new Bookable();
        bookable.setQuantity(5);

        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        existingBook.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        existingBook.setStatus(Book.Status.APPROVED); // Non-PENDING status
        existingBook.setQuantityBooked(2);
        existingBook.setBookable(bookable);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin can update any book
        existingBook.setUser(currentUser);

        BookUpdateRequest bookUpdateRequest = new BookUpdateRequest();
        bookUpdateRequest.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        bookUpdateRequest.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        bookUpdateRequest.setQuantityBooked(2);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act
        BookResponse result = bookService.updateBook(bookId, bookUpdateRequest, currentUser);

        // Assert
        assertEquals(Book.Status.APPROVED, result.getStatus());
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    // Vérifier que la quantité n'est pas décrémentée si la quantité réservée est la même lors de la mise à jour
    @Test
    public void testUpdateBook_ShouldNotDecrementQuantityIfSameQuantityBooked() {
        // Arrange
        Long bookId = 1L;

        Bookable bookable = new Bookable();
        bookable.setQuantity(4);

        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        existingBook.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        existingBook.setQuantityBooked(3); // ancienne quantité
        existingBook.setBookable(bookable);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN);
        existingBook.setUser(currentUser);

        BookUpdateRequest bookUpdateRequest = new BookUpdateRequest();
        bookUpdateRequest.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        bookUpdateRequest.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        bookUpdateRequest.setQuantityBooked(3); // même quantité

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act
        bookService.updateBook(bookId, bookUpdateRequest, currentUser);

        // Assert
        assertEquals(4, bookable.getQuantity()); // Quantity should remain the same
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    /*
    * MARK AS RETURNED
    */
    // Vérifier le ONLY ADMIN pour marquer une réservation comme RETURNED
    @Test
    public void testMarkAsReturned_ShouldThrowExceptionIfUserIsNotAdmin() {
        // Arrange
        Long bookId = 1L;
        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setStatus(Book.Status.APPROVED);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.USER); // Not admin

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.markAsReturned(bookId, currentUser);
        });
    }

    // Vérifier que la réservation est bien APPROVED ou OVERDUE pour être marquée comme RETURNED
    @Test
    public void testMarkAsReturned_ShouldThrowExceptionIfBookNotApprovedOrOverdue() {
        // Arrange
        Long bookId = 1L;
        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setStatus(Book.Status.PENDING); // Not APPROVED or OVERDUE

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.markAsReturned(bookId, currentUser);
        });
    }

    // Vérifier que le status n'est pas changé si le status actuel est déjà RETURNED
    @Test
    public void testMarkAsReturned_ShouldThrowExceptionIfBookAlreadyReturned() {
        // Arrange
        Long bookId = 1L;
        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setStatus(Book.Status.RETURNED); // Already RETURNED

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.markAsReturned(bookId, currentUser);
        });
    }

    // Vérifier que la réservation est bien marquée comme RETURNED et que la quantité disponible est réincrémentée
    @Test
    public void testMarkAsReturned_ShouldMarkBookAsReturnedAndIncrementQuantity() {
        // Arrange
        Long bookId = 1L;
        Bookable bookable = new Bookable();
        bookable.setQuantity(3); // Available quantity

        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setStatus(Book.Status.APPROVED);
        existingBook.setQuantityBooked(2);
        existingBook.setBookable(bookable);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setEmail("admin@test.com");
        currentUser.setRole(AppUser.Role.ADMIN); // Admin
        existingBook.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act
        BookResponse result = bookService.markAsReturned(bookId, currentUser);

        // Assert
        assertEquals(Book.Status.RETURNED, result.getStatus());
        assertEquals(5, bookable.getQuantity()); // 3 + 2 = 5
    }

    /*
    * APPROVE BOOK
    */
    // Vérifier que seul un admin peut approuver une réservation
    @Test
    public void testApproveBook_ShouldThrowExceptionIfUserIsNotAdmin() {
        // Arrange
        Long bookId = 1L;
        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setStatus(Book.Status.PENDING);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.USER); // Not admin

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.approveBook(bookId, currentUser);
        });
    }

    // Vérifier que seul un book PENDING peut être approuvé
    @Test
    public void testApproveBook_ShouldThrowExceptionIfBookNotPending() {
        // Arrange
        Long bookId = 1L;
        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setStatus(Book.Status.CANCELED); // Not PENDING

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.approveBook(bookId, currentUser);
        });
    }

    // Vérifier que la réservation est bien approuvée
    @Test
    public void testApproveBook_ShouldApproveBook() {
        // Arrange
        Long bookId = 1L;
        Bookable bookable = new Bookable();
        bookable.setName("Bookable 1");

        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setStatus(Book.Status.PENDING);
        existingBook.setQuantityBooked(1);
        existingBook.setBookable(bookable);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setEmail("admin@test.com");
        currentUser.setRole(AppUser.Role.ADMIN); // Admin
        existingBook.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act
        BookResponse result = bookService.approveBook(bookId, currentUser);

        // Assert
        assertEquals(Book.Status.APPROVED, result.getStatus());
    }

    /*
    * CANCEL BOOK
    */
   // Vérifier que seul un book PENDING peut être annulé
    @Test
    public void testCancelBook_ShouldThrowExceptionIfBookNotPending() {
        // Arrange
        Long bookId = 1L;
        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setStatus(Book.Status.APPROVED); // Not PENDING

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.USER); // User

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.cancelBook(bookId, currentUser);
        });
    }

    // Vérifier que la réservation est bien annulée
    @Test
    public void testCancelBook_ShouldCancelBook() {
        // Arrange
        Long bookId = 1L;
        Bookable bookable = new Bookable();
        bookable.setQuantity(3); // Available quantity

        Book existingBook = new Book();
        existingBook.setBookId(bookId);
        existingBook.setStatus(Book.Status.PENDING);
        existingBook.setQuantityBooked(2);
        existingBook.setBookable(bookable);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setEmail("user@test.com");
        currentUser.setRole(AppUser.Role.USER); // User
        existingBook.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(existingBook));

        // Act
        BookResponse result = bookService.cancelBook(bookId, currentUser);

        // Assert
        assertEquals(Book.Status.CANCELED, result.getStatus());
        assertEquals(5, bookable.getQuantity()); // 3 + 2 = 5
    }

    // scheduler pour marquer les réservations en retard
    @Test 
    public void testReleaseExpiredBooks_ShouldReturnBooksWithStatusOverdue() {
        // Arrange
        Book book1 = new Book();
        book1.setBookId(1L);
        book1.setStatus(Book.Status.PENDING);
        Book book2 = new Book();
        book2.setBookId(2L);
        book2.setStatus(Book.Status.APPROVED);
        List<Book> overdueBooks = Arrays.asList(book1, book2);

        when(bookRepository.findByEndDateBeforeAndStatusIn(any(LocalDateTime.class), anyList()))
            .thenReturn(overdueBooks);

        // Act
        bookService.releaseExpiredBooks();

        // Assert
        assertEquals(Book.Status.OVERDUE, book1.getStatus());
        assertEquals(Book.Status.OVERDUE, book2.getStatus());
        verify(bookRepository, times(1)).findByEndDateBeforeAndStatusIn(any(LocalDateTime.class), anyList());
    }
}
