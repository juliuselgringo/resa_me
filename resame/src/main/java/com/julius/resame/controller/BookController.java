package com.julius.resame.controller;

import com.julius.resame.service.BookService;
import com.julius.resame.model.Book;
import com.julius.resame.dto.BookRequest;
import com.julius.resame.dto.BookResponse;
import com.julius.resame.dto.BookUpdateRequest;
import com.julius.resame.model.AppUser;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

@RestController 
@RequiredArgsConstructor
@RequestMapping ("/api/books")
public class BookController {
    
    private final BookService bookService;

    // GET /api/books -> liste de toutes les réservations
    @GetMapping
    public List<BookResponse> getAllBooks() {
        return bookService.getAllBooks();
    }

    // GET /api/books/{bookId} -> récupérer une réservation par son ID
    @GetMapping("/{bookId}")
    public BookResponse getBookById(@PathVariable Long bookId) {
        return bookService.getBookById(bookId);
    }

    // GET /api/books/user/{userId} -> récupérer toutes les réservations d'un utilisateur par son ID
    @GetMapping("/user/{userId}")
    public List<BookResponse> getBooksByUserId(@PathVariable Long userId) {
        return bookService.getBooksByUserId(userId);
    }

    // POST /api/books -> créer une nouvelle réservation
    @PostMapping
    public BookResponse createBook(@Valid @RequestBody BookRequest bookRequest, @AuthenticationPrincipal AppUser currentUser) {
        return bookService.createBook(bookRequest, currentUser);
    }

    // PUT /api/books/{bookId} -> mettre à jour une réservation par son ID
    @PutMapping("/{bookId}")
    public BookResponse updateBook(@PathVariable Long bookId, @Valid @RequestBody BookUpdateRequest bookUpdateRequest, @AuthenticationPrincipal AppUser currentUser) {
        return bookService.updateBook(bookId, bookUpdateRequest, currentUser);
    }

    // POST /api/books/{bookId}/return -> retourner une réservation par son ID
    @PostMapping("/{bookId}/return")
    public BookResponse returnBook(@PathVariable Long bookId, @AuthenticationPrincipal AppUser currentUser) {
        return bookService.markAsReturned(bookId, currentUser);
    }

    // POST /api/books/{bookId}/approve -> approuver une réservation par son ID
    @PostMapping("/{bookId}/approve")
    public BookResponse approveBook(@PathVariable Long bookId, @AuthenticationPrincipal AppUser currentUser) {
        return bookService.approveBook(bookId, currentUser);
    }

    // POST /api/books/{bookId}/cancel -> annuler une réservation par son ID
    @PostMapping("/{bookId}/cancel")
    public BookResponse cancelBook(@PathVariable Long bookId, @AuthenticationPrincipal AppUser currentUser) {
        return bookService.cancelBook(bookId, currentUser);
    }

}
