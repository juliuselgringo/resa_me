package com.julius.resame.controller;

import com.julius.resame.service.BookService;
import com.julius.resame.model.Book;
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

@RestController 
@RequiredArgsConstructor
@RequestMapping ("/api/books")
public class BookController {
    
    private final BookService bookService;

    // GET /api/books -> liste de toutes les réservations
    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    // GET /api/books/{bookId} -> récupérer une réservation par son ID
    @GetMapping("/{bookId}")
    public Book getBookById(@PathVariable Long bookId) {
        return bookService.getBookById(bookId);
    }

    // GET /api/books/user/{userId} -> récupérer toutes les réservations d'un utilisateur par son ID
    @GetMapping("/user/{userId}")
    public List<Book> getBooksByUserId(@PathVariable Long userId) {
        return bookService.getBooksByUserId(userId);
    }

    // POST /api/books -> créer une nouvelle réservation
    @PostMapping
    public Book createBook(@RequestBody Book book) {
        return bookService.createBook(book);
    }

    // PUT /api/books/{bookId} -> mettre à jour une réservation par son ID
    @PutMapping("/{bookId}")
    public Book updateBook(@PathVariable Long bookId, @RequestBody Book updatedBook, @AuthenticationPrincipal AppUser currentUser) {
        return bookService.updateBook(bookId, updatedBook, currentUser);
    }

}
