package com.julius.resame.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter 
@Setter 
public class BookUpdateRequest {
    @NotNull private LocalDateTime startDate;   // optionnel
    @NotNull private LocalDateTime endDate;     // optionnel

    @NotNull
    @Min(value = 1, message = "La quantité réservée doit être au moins 1")
    private Integer quantityBooked;    // optionnel

}
