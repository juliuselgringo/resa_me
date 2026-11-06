package com.julius.resame.dto;

import com.julius.resame.model.Book;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class BookResponse {
    private Long bookId;
    private String userEmail;
    private String bookableName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int quantityBooked;
    private Book.Status status;
    private LocalDateTime createdAt;

    public BookResponse(Book book) {
        this.bookId = book.getBookId();
        this.userEmail = book.getUser().getEmail();
        this.bookableName = book.getBookable().getName();
        this.startDate = book.getStartDate();
        this.endDate = book.getEndDate();
        this.quantityBooked = book.getQuantityBooked();
        this.status = book.getStatus();
        this.createdAt = book.getCreatedAt();
    }
}
