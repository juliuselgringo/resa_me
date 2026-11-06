package com.julius.resame.service;

import com.julius.resame.model.Book;
import com.julius.resame.repository.BookRepository;
import com.julius.resame.repository.BookableRepository;
import com.julius.resame.model.Bookable;
import com.julius.resame.dto.BookRequest;
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

    // Vérifier que l'utilisateur a les droits pour mettre à jour la réservation
    @Test
    public void testUpdateBook_ShouldThrowIfUserNotAuthorized() {
        // Arrange
        Long bookId = 1L;

        Book updatedBook = new Book();
        updatedBook.setBookId(bookId);

        AppUser bookOwner = new AppUser();
        bookOwner.setUserId(2L); // Owner of the book
        updatedBook.setUser(bookOwner);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L); // Different user
        currentUser.setRole(AppUser.Role.USER);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(updatedBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.updateBook(bookId, updatedBook, currentUser);
        });
    }

    // Vérifier qu'un user classique ne peut modifier que les réservations dont le status est PENDING
    @Test
    public void testUpdateBook_ShouldThrowIfNonAdminUpdatesNonPending() {
        // Arrange
        Long bookId = 1L;

        Book updatedBook = new Book();
        updatedBook.setBookId(bookId);
        updatedBook.setStatus(Book.Status.APPROVED); // Non-PENDING status

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

    // Vérifier que la date de début est avant la date de fin lors de la mise à jour
    @Test
    public void testUpdateBook_ShouldThrowIfStartDateAfterEndDate() {
        // Arrange
        Long bookId = 1L;

        Book updatedBook = new Book();
        updatedBook.setBookId(bookId);
        updatedBook.setStartDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        updatedBook.setEndDate(LocalDateTime.of(2027, 1, 1, 0, 0)); // Invalid dates

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin can update any book
        updatedBook.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(updatedBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.updateBook(bookId, updatedBook, currentUser);
        });
    }

    // Vérifier que la quantité réservée ne dépasse pas la quantité disponible lors de la mise à jour
    @Test
    public void testUpdateBook_ShouldThrowIfQuantityBookedExceedsAvailable() {
        // Arrange
        Long bookId = 1L;

        Book updatedBook = new Book();
        updatedBook.setBookId(bookId);
        updatedBook.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        updatedBook.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        updatedBook.setQuantityBooked(10); // Booked quantity

        Bookable bookable = new Bookable();
        bookable.setQuantity(5); // Available quantity is less than booked quantity
        updatedBook.setBookable(bookable);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin can update any book
        updatedBook.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(updatedBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.updateBook(bookId, updatedBook, currentUser);
        });
    }

    // Vérifier que la réservation est sauvegardée et que la quantité disponible est décrémentée lors de la mise à jour
    @Test
    public void testUpdateBook_ShouldSaveBookAndDecrementQuantity() {
        // Arrange
        Long bookId = 1L;

        Book oldBook = new Book();
        oldBook.setBookId(bookId);
        oldBook.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        oldBook.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        oldBook.setQuantityBooked(1); // Booked quantity        

        Book updatedBook = new Book();
        updatedBook.setBookId(bookId);
        updatedBook.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        updatedBook.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        updatedBook.setQuantityBooked(3); // Booked quantity

        Bookable bookable = new Bookable();
        bookable.setQuantity(4); // Available quantity is more than booked quantity
        updatedBook.setBookable(bookable);
        oldBook.setBookable(bookable);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin can update any book
        updatedBook.setUser(currentUser);
        oldBook.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(oldBook));

        when(bookRepository.save(any(Book.class))).thenReturn(updatedBook);

        // Act
        bookService.updateBook(bookId, updatedBook, currentUser);

        // Assert
        assertEquals(2, bookable.getQuantity()); // Quantity should be decremented
        verify(bookRepository, times(1)).save(updatedBook); // Ensure save is called once
    }

    // Vérifier que admin peut mettre à jour un status APPROVED
    @Test
    public void testUpdateBook_ShouldSucceedIfAdminUpdatesApprovedBook() {
        // Arrange
        Long bookId = 1L;

        Book updatedBook = new Book();
        updatedBook.setBookId(bookId);
        updatedBook.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        updatedBook.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        updatedBook.setStatus(Book.Status.APPROVED); // Non-PENDING status
        updatedBook.setQuantityBooked(2);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin can update any book
        updatedBook.setUser(currentUser);

        Bookable bookable = new Bookable();
        bookable.setQuantity(5); // Available quantity is more than booked quantity

        updatedBook.setBookable(bookable);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(updatedBook));

        when(bookRepository.save(any(Book.class))).thenReturn(updatedBook);

        // Act
        Book result = bookService.updateBook(bookId, updatedBook, currentUser);

        // Assert
        assertEquals(Book.Status.APPROVED, result.getStatus());
        verify(bookRepository, times(1)).save(updatedBook); // Ensure save is called once
    }

    // Vérifier que la quantité n'est pas décrémentée si la quantité réservée est la même lors de la mise à jour
    @Test
    public void testUpdateBook_ShouldNotDecrementQuantityIfSameQuantityBooked() {
        // Arrange
        Long bookId = 1L;

        Book oldBook = new Book();
        oldBook.setBookId(bookId);
        oldBook.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        oldBook.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        oldBook.setQuantityBooked(3); // Booked quantity        

        Book updatedBook = new Book();
        updatedBook.setBookId(bookId);
        updatedBook.setStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        updatedBook.setEndDate(LocalDateTime.of(2027, 1, 2, 0, 0));
        updatedBook.setQuantityBooked(3); // Same booked quantity

        Bookable bookable = new Bookable();
        bookable.setQuantity(4); // Available quantity is more than booked quantity
        updatedBook.setBookable(bookable);
        oldBook.setBookable(bookable);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin can update any book
        updatedBook.setUser(currentUser);
        oldBook.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(oldBook));

        when(bookRepository.save(any(Book.class))).thenReturn(updatedBook);

        // Act
        bookService.updateBook(bookId, updatedBook, currentUser);

        // Assert
        assertEquals(4, bookable.getQuantity()); // Quantity should remain the same
        verify(bookRepository, times(1)).save(updatedBook); // Ensure save is called once
    }

    /*
    * MARK AS RETURNED
    */
    // Vérifier le ONLY ADMIN pour marquer une réservation comme RETURNED
    @Test
    public void testMarkAsReturned_ShouldThrowExceptionIfUserIsNotAdmin() {
        // Arrange
        Long bookId = 1L;
        Book bookToMark = new Book();
        bookToMark.setBookId(bookId);
        bookToMark.setStatus(Book.Status.APPROVED);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.USER); // Not admin
        bookToMark.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(bookToMark));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.markAsReturned(bookId, bookToMark, currentUser);
        });
    }

    // Vérifier que la réservation est bien APPROVED ou OVERDUE pour être marquée comme RETURNED
    @Test
    public void testMarkAsReturned_ShouldThrowExceptionIfBookNotApprovedOrOverdue() {
        // Arrange
        Long bookId = 1L;
        Book bookToMark = new Book();
        bookToMark.setBookId(bookId);
        bookToMark.setStatus(Book.Status.PENDING); // Not APPROVED or OVERDUE

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin
        bookToMark.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(bookToMark));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.markAsReturned(bookId, bookToMark, currentUser);
        });
    }

    // Vérifier que le status n'est pas changé si le status actuel est déjà RETURNED
    @Test
    public void testMarkAsReturned_ShouldThrowExceptionIfBookAlreadyReturned() {
        // Arrange
        Long bookId = 1L;
        Book bookToMark = new Book();
        bookToMark.setBookId(bookId);
        bookToMark.setStatus(Book.Status.RETURNED); // Already RETURNED

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin
        bookToMark.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(bookToMark));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.markAsReturned(bookId, bookToMark, currentUser);
        });
    }

    // Vérifier que la réservation est bien marquée comme RETURNED et que la quantité disponible est réincrémentée
    @Test
    public void testMarkAsReturned_ShouldMarkBookAsReturnedAndIncrementQuantity() {
        // Arrange
        Long bookId = 1L;
        Book bookToMark = new Book();
        bookToMark.setBookId(bookId);
        bookToMark.setStatus(Book.Status.APPROVED);
        bookToMark.setQuantityBooked(2);

        Bookable bookable = new Bookable();
        bookable.setQuantity(3); // Available quantity
        bookToMark.setBookable(bookable);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin
        bookToMark.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(bookToMark));

        // Act
        Book result = bookService.markAsReturned(bookId, bookToMark, currentUser);

        // Assert
        assertEquals(Book.Status.RETURNED, result.getStatus());
        assertEquals(5, bookable.getQuantity()); // Quantity should be incremented
    }

    /*
    * APPROVE BOOK
    */
    // Vérifier que seul un admin peut approuver une réservation
    @Test
    public void testApproveBook_ShouldThrowExceptionIfUserIsNotAdmin() {
        // Arrange
        Long bookId = 1L;
        Book bookToApprove = new Book();
        bookToApprove.setBookId(bookId);
        bookToApprove.setStatus(Book.Status.PENDING);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.USER); // Not admin
        bookToApprove.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(bookToApprove));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.approveBook(bookId, bookToApprove, currentUser);
        });
    }

    // Vérifier que seul un book PENDING peut être approuvé
    @Test
    public void testApproveBook_ShouldThrowExceptionIfBookNotPending() {
        // Arrange
        Long bookId = 1L;
        Book bookToApprove = new Book();
        bookToApprove.setBookId(bookId);
        bookToApprove.setStatus(Book.Status.CANCELED); // Not PENDING

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin
        bookToApprove.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(bookToApprove));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.approveBook(bookId, bookToApprove, currentUser);
        });
    }

    // Vérifier que la réservation est bien approuvée
    @Test
    public void testApproveBook_ShouldApproveBook() {
        // Arrange
        Long bookId = 1L;
        Book bookToApprove = new Book();
        bookToApprove.setBookId(bookId);
        bookToApprove.setStatus(Book.Status.PENDING);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.ADMIN); // Admin
        bookToApprove.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(bookToApprove));

        // Act
        Book result = bookService.approveBook(bookId, bookToApprove, currentUser);

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
        Book bookToCancel = new Book();
        bookToCancel.setBookId(bookId);
        bookToCancel.setStatus(Book.Status.APPROVED); // Not PENDING

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.USER); // User
        bookToCancel.setUser(currentUser);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(bookToCancel));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.cancelBook(bookId, bookToCancel, currentUser);
        });
    }

    // Vérifier que la réservation est bien annulée
    @Test
    public void testCancelBook_ShouldCancelBook() {
        // Arrange
        Long bookId = 1L;
        Book bookToCancel = new Book();
        bookToCancel.setBookId(bookId);
        bookToCancel.setStatus(Book.Status.PENDING);
        bookToCancel.setQuantityBooked(2);

        AppUser currentUser = new AppUser();
        currentUser.setUserId(1L);
        currentUser.setRole(AppUser.Role.USER); // User
        bookToCancel.setUser(currentUser);

        Bookable bookable = new Bookable();
        bookable.setQuantity(3); // Available quantity
        bookToCancel.setBookable(bookable);

        // Mock : Le book existe
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(bookToCancel));

        // Act
        Book result = bookService.cancelBook(bookId, bookToCancel, currentUser);

        // Assert
        assertEquals(Book.Status.CANCELED, result.getStatus());
        assertEquals(5, bookable.getQuantity()); // Quantity should be incremented (3+2)
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
