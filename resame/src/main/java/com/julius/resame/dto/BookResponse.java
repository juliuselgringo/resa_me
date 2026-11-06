package com.julius.resame.dto;

import java.time.LocalDateTime;

import com.julius.resame.model.Book;

public class BookResponse {
    private Long bookId;
    private String userEmail;
    private String bookableName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int quantityBooked;
    private Book.Status status;
    private LocalDateTime createdAt;
}
