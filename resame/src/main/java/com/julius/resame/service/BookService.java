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

        return book;
    }

    @Transactional 
    public Book updateBook(Long bookId, Book updatedBook, AppUser currentUser) {
        // vérifier que l'id du book correspond à l'id du book à mettre à jour
        if (!bookId.equals(updatedBook.getBookId())) {
            throw new IllegalArgumentException("Book ID mismatch");
        }
        // vérifier les droits
        if (!updatedBook.getUser().getUserId().equals(currentUser.getUserId()) && !currentUser.getRole().equals(AppUser.Role.ADMIN)) {
            throw new IllegalArgumentException("You are not authorized to update this book");
        }

        // vérifier l'existence du book
        if(!bookRepository.findById(bookId).isPresent()){
            throw new IllegalArgumentException("Book with ID " + bookId + " does not exist");
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

    @Transactional
    public Book markAsReturned(Long bookId, AppUser currentUser) {
        Book bookToMark = bookRepository.findById(bookId).orElseThrow(() -> new IllegalArgumentException("Book with ID " + bookId + " does not exist"));

        // vérifier les droits
        if (!bookToMark.getUser().getUserId().equals(currentUser.getUserId()) && !currentUser.getRole().equals(AppUser.Role.ADMIN)) {
            throw new IllegalArgumentException("You are not authorized to update this book");
        }

        // vérifier que le bookable n'est pas déjà marqué comme retourné
        if (bookToMark.getStatus() == Book.Status.RETURNED) {
            throw new IllegalArgumentException("Book is already marked as returned");
        }

        Bookable bookable = bookToMark.getBookable();
        bookable.setQuantity(bookable.getQuantity() + bookToMark.getQuantityBooked());
        bookToMark.setStatus(Book.Status.RETURNED);

        return bookToMark;
    }

    @Scheduled (cron = "0 0 * * * *") // exécuter toutes les heures
    @Transactional
    public void releaseExpiredBooks() {
        LocalDateTime now = LocalDateTime.now();
        // OVERDUE si endDate est dépassé et status est PENDING ou APPROVED
        List<Book> overdueBooks = bookRepository.findByEndDateBeforeAndStatusIn(now, List.of(Book.Status.PENDING, Book.Status.APPROVED));

        overdueBooks.forEach(book -> book.setStatus(Book.Status.OVERDUE));
    }

}
