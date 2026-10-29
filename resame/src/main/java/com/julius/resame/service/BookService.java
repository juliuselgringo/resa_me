package com.julius.resame.service;

import com.julius.resame.repository.BookRepository;
import com.julius.resame.model.Book;
import com.julius.resame.model.Bookable;
import com.julius.resame.model.AppUser;

import jakarta.transaction.Transactional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Service 
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;

    @Transactional
    public Book createBook(Book book) {
        // vérifier que startDate est avant endDate
        if (book.getStartDate().isAfter(book.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        // vérifier que bookable.quantity est supérieur à book.quantity_booked
        Bookable bookable = book.getBookable();
        if (bookable.getQuantity() < book.getQuantityBooked()) {
            throw new IllegalArgumentException("Quantity booked cannot exceed available quantity");
        }

        // décrémenter la quantité disponible de bookable
        bookable.setQuantity(bookable.getQuantity() - book.getQuantityBooked());
        bookRepository.save(book);

        // status par défaut 
        book.setStatus(Book.Status.PENDING);

        return book;
    }

    // Vérifier l'existence, les permissions et le match id
    private void verifyExistsAndPermissionsAndIdMatch(Long bookId, Book targetBook, AppUser currentUser) {
        // vérifier l'existence du book
        if(!bookRepository.findById(bookId).isPresent()){
            throw new IllegalArgumentException("Book with ID " + bookId + " does not exist");
        }
        // vérifier que l'id du book correspond à l'id du book à mettre à jour
        if (!bookId.equals(targetBook.getBookId())) {
            throw new IllegalArgumentException("Book ID mismatch");
        }
        // vérifier les droits
        if (!targetBook.getUser().getUserId().equals(currentUser.getUserId()) && !currentUser.getRole().equals(AppUser.Role.ADMIN)) {
            throw new IllegalArgumentException("You are not authorized to update this book");
        }
        
    }

    @Transactional 
    public Book updateBook(Long bookId, Book updatedBook, AppUser currentUser) {
        // vérifier id match et permissions
        verifyExistsAndPermissionsAndIdMatch(bookId, updatedBook, currentUser);

        // un utilisateur ne peux modifier que les réservations dont le status est PENDING
        if(!currentUser.getRole().equals(AppUser.Role.ADMIN) && updatedBook.getStatus() != Book.Status.PENDING) {
            throw new IllegalArgumentException("Only ADMIN can update non-PENDING books. Current status: " + updatedBook.getStatus());
        }

        // vérifier que startDate est avant endDate
        if (updatedBook.getStartDate().isAfter(updatedBook.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        // vérifier que bookable.quantity est supérieur à book.quantity_booked
        Bookable bookable = updatedBook.getBookable();
        if (bookable.getQuantity() < updatedBook.getQuantityBooked()) {
            throw new IllegalArgumentException("Quantity booked cannot exceed available quantity");
        }

        // décrémenter la quantité disponible de bookable si la quantité réservée a changé
        // récupérer l'ancien book pour comparer les quantités
        Book oldBook = bookRepository.findById(bookId).get();
        if (oldBook.getQuantityBooked() != updatedBook.getQuantityBooked()) {
            bookable.setQuantity(bookable.getQuantity() + oldBook.getQuantityBooked() - updatedBook.getQuantityBooked());
        }

        bookRepository.save(updatedBook);

        return updatedBook;

    }

    // changer le status d'une réservation
    public Book changeBookStatus(Book bookToMark, Book.Status newStatus) {
        // vérifier que le book n'a pas déjà le status voulu
        if (bookToMark.getStatus() == newStatus) {
            throw new IllegalArgumentException("Book is already marked as " + newStatus);
        }
        bookToMark.setStatus(newStatus);
        return bookToMark;
    }

    // marqué le status d'une réservation RETURNED
    @Transactional
    public Book markAsReturned(Long bookId, Book bookToMark,AppUser currentUser) {
        // vérifier id match et permissions
        verifyExistsAndPermissionsAndIdMatch(bookId, bookToMark, currentUser);

        // ONLY ADMIN
        if(currentUser.getRole() != AppUser.Role.ADMIN) {
            throw new IllegalArgumentException("Only ADMIN can mark as returned a book");
        }

        // Vérifier que la réservation bookToMark a bien été APPROVED ou OVERDUE
        if(bookToMark.getStatus() != Book.Status.APPROVED && bookToMark.getStatus() != Book.Status.OVERDUE){
            throw new IllegalArgumentException("Book have to be APPROVED or OVERDUE to be RETURNED");
        }

        // réincrémenter les quantités
        Bookable bookable = bookToMark.getBookable();
        bookable.setQuantity(bookable.getQuantity() + bookToMark.getQuantityBooked());

        // changer le status
        bookToMark = changeBookStatus(bookToMark, Book.Status.RETURNED);

        return bookToMark;
    }

    // Vérification et changement auto des status des réservations non retournée en OVERDUE
    @Scheduled (cron = "0 0 * * * *") // exécuter toutes les heures
    @Transactional
    public void releaseExpiredBooks() {
        LocalDateTime now = LocalDateTime.now();
        // OVERDUE si endDate est dépassé et status est PENDING ou APPROVED
        List<Book> overdueBooks = bookRepository.findByEndDateBeforeAndStatusIn(now, List.of(Book.Status.PENDING, Book.Status.APPROVED));

        overdueBooks.forEach(book -> book.setStatus(Book.Status.OVERDUE));
    }

    // Approuver une réservation
    @Transactional
    public Book approveBook(Long bookId, Book bookToApprove, AppUser currentUser) {
        verifyExistsAndPermissionsAndIdMatch(bookId, bookToApprove, currentUser);

        // only admin
        if(currentUser.getRole() != AppUser.Role.ADMIN) {
            throw new IllegalArgumentException("Only ADMIN can approve a book");
        }

        // Seul les réservation PENDING peuvent être approved
        if (bookToApprove.getStatus() != Book.Status.PENDING) {
            throw new IllegalArgumentException("Only PENDING books can be approved");
        }

        changeBookStatus(bookToApprove,Book.Status.APPROVED);
        return bookToApprove;
    }

    // Annulation d'une réservation
    @Transactional 
    public Book cancelBook(Long bookId, Book bookToCancel, AppUser currentUser) {
        // Vérifier match id et permissions
        verifyExistsAndPermissionsAndIdMatch(bookId, bookToCancel, currentUser);

        // Vérifier que bookToCancel a un status PENDING
        if(bookToCancel.getStatus() != Book.Status.PENDING) {
            throw new IllegalArgumentException("Book have to be PENDING to be CANCELED")
        }
        
        // changer le status
        bookToCancel = changeBookStatus(bookToCancel, Book.Status.CANCELED);

        // Réincrémenter les quantités
        Bookable bookable = bookToCancel.getBookable();
        bookable.setQuantity(bookable.getQuantity() + bookToCancel.getQuantityBooked());

        return bookToCancel;
    }

}
