package com.julius.resame.service;

import com.julius.resame.repository.BookRepository;
import com.julius.resame.model.Book;
import com.julius.resame.dto.BookRequest;
import com.julius.resame.dto.BookResponse;
import com.julius.resame.dto.BookUpdateRequest;
import com.julius.resame.model.Bookable;
import com.julius.resame.repository.BookableRepository;
import com.julius.resame.model.AppUser;

import jakarta.transaction.Transactional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service 
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final BookableRepository bookableRepository;
    /*
    * CREATE BOOK
    */
    @Transactional
    public BookResponse createBook(BookRequest bookRequest, AppUser currentUser) {
        // Convertir BookRequest en Book
        Book book = new Book();
        Bookable bookable = bookableRepository.findById(bookRequest.getBookableId())
                .orElseThrow(() -> new IllegalArgumentException("Bookable with ID " + bookRequest.getBookableId() + " does not exist"));
        book.setBookable(bookable);
        book.setUser(currentUser);
        book.setStartDate(bookRequest.getStartDate());
        book.setEndDate(bookRequest.getEndDate());      
        book.setQuantityBooked(bookRequest.getQuantityBooked());

        // vérifier que startDate est avant endDate
        if (book.getStartDate().isAfter(book.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        // vérifier que bookable.quantity est supérieur à book.quantityBooked
        if (bookable.getQuantity() < book.getQuantityBooked()) {
            throw new IllegalArgumentException("Quantity booked cannot exceed available quantity");
        }

        // décrémenter la quantité disponible de bookable
        bookable.setQuantity(bookable.getQuantity() - book.getQuantityBooked());

        // status par défaut 
        book.setStatus(Book.Status.PENDING);

        // sauvegarder la réservation
        bookRepository.save(book);

        return new BookResponse(book);
    }

    /*
    * VERIFY EXISTS AND PERMISSIONS AND ID MATCH
    */
    // Vérifier l'existence, les permissions et le match id
    private void verifyPermissions(Book targetBook, AppUser currentUser) {        
        // vérifier les droits
        if (!targetBook.getUser().getUserId().equals(currentUser.getUserId()) && !currentUser.getRole().equals(AppUser.Role.ADMIN)) {
            throw new IllegalArgumentException("You are not authorized to update this book");
        }
        
    }

    /*
    * UPDATE BOOK
    */
    @Transactional 
    public BookResponse updateBook(Long bookId, BookUpdateRequest bookUpdateRequest, AppUser currentUser) {
        // convertir BookUpdateRequest en Book
        Book bookToUpdate = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book with ID " + bookId + " does not exist"));

        // vérifier permissions
        verifyPermissions(bookToUpdate, currentUser);

        // un utilisateur ne peux modifier que les réservations dont le status est PENDING
        if(!currentUser.getRole().equals(AppUser.Role.ADMIN) && bookToUpdate.getStatus() != Book.Status.PENDING) {
            throw new IllegalArgumentException("Only ADMIN can update non-PENDING books. Current status: " + bookToUpdate.getStatus());
        }

        // vérifier que startDate est avant endDate
        if (bookUpdateRequest.getStartDate().isAfter(bookUpdateRequest.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        // vérifier que bookable.quantity est supérieur à book.quantityBooked
        Bookable bookable = bookToUpdate.getBookable();
        if ((bookable.getQuantity()+bookToUpdate.getQuantityBooked()) < bookUpdateRequest.getQuantityBooked()) {
            throw new IllegalArgumentException("Quantity booked cannot exceed available quantity");
        }

        // décrémenter la quantité disponible de bookable si la quantité réservée a changé
        // récupérer l'ancien book pour comparer les quantités
        if (bookToUpdate.getQuantityBooked() != bookUpdateRequest.getQuantityBooked()) {
            bookable.setQuantity(bookable.getQuantity() + bookToUpdate.getQuantityBooked() - bookUpdateRequest.getQuantityBooked());
        }

        // mettre à jour les champs de la réservation
        bookToUpdate.setStartDate(bookUpdateRequest.getStartDate());
        bookToUpdate.setEndDate(bookUpdateRequest.getEndDate());
        bookToUpdate.setQuantityBooked(bookUpdateRequest.getQuantityBooked());

        bookRepository.save(bookToUpdate);

        return new BookResponse(bookToUpdate);

    }

    // changer le status d'une réservation si le status actuel est différent du nouveau status
    private Book changeBookStatus(Book bookToMark, Book.Status newStatus) {
        // vérifier que le book n'a pas déjà le status voulu
        if (bookToMark.getStatus() == newStatus) {
            throw new IllegalArgumentException("Book is already marked as " + newStatus);
        }
        bookToMark.setStatus(newStatus);
        return bookToMark;
    }

    // marqué le status d'une réservation RETURNED
    @Transactional
    public BookResponse markAsReturned(Long bookId, AppUser currentUser) {
        // vérifier que bookId existe
        Book bookToMark = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book with ID " + bookId + " does not exist"));

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

        return new BookResponse(bookToMark);
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
    public BookResponse approveBook(Long bookId, AppUser currentUser) {
        // Vérifier que la réservation existe
        Book bookToApprove = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book with ID " + bookId + " does not exist"));

        // only admin
        if(currentUser.getRole() != AppUser.Role.ADMIN) {
            throw new IllegalArgumentException("Only ADMIN can approve a book");
        }

        // Seul les réservation PENDING peuvent être approved
        if (bookToApprove.getStatus() != Book.Status.PENDING) {
            throw new IllegalArgumentException("Only PENDING books can be approved");
        }

        changeBookStatus(bookToApprove,Book.Status.APPROVED);

        return new BookResponse(bookToApprove);
    }

    // Annulation d'une réservation
    @Transactional 
    public BookResponse cancelBook(Long bookId, AppUser currentUser) {
        // Vérifier si la réservation existe
        Book bookToCancel = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book with ID " + bookId + " does not exist"));

        // Vérifier que bookToCancel a un status PENDING
        if(bookToCancel.getStatus() != Book.Status.PENDING) {
            throw new IllegalArgumentException("Book have to be PENDING to be CANCELED");
        }
        
        // changer le status
        bookToCancel = changeBookStatus(bookToCancel, Book.Status.CANCELED);

        // Réincrémenter les quantités
        Bookable bookable = bookToCancel.getBookable();
        bookable.setQuantity(bookable.getQuantity() + bookToCancel.getQuantityBooked());

        return new BookResponse(bookToCancel);
    }

    /**
     * GETTER
     */
    // GET ALL BOOKS
    public List<BookResponse> getAllBooks() {
        List<Book> books = bookRepository.findAll();
        List<BookResponse> booksResponse = new ArrayList<>();
        for (Book book : books) {
            booksResponse.add(new BookResponse(book));
        }
        return booksResponse;
    }

    // GET BOOK BY ID
    public BookResponse getBookById(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book with ID " + bookId + " does not exist"));
        return new BookResponse(book);
    }

    // GET BOOKS BY USER ID
    public List<BookResponse> getBooksByUserId(Long userId) {
        List<Book> booksByUser = bookRepository.findByUserUserId(userId);
        List<BookResponse> booksResponse = new ArrayList<>();
        booksResponse = booksByUser.stream().map(book -> new BookResponse(book)).toList(); 
        return booksResponse;
    }


}
