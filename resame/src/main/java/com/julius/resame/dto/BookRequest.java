package com.julius.resame.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter 
@Setter 
@NoArgsConstructor
public class BookRequest {
    @NotNull private Long bookableId;
    @NotNull private LocalDateTime startDate;
    @NotNull private LocalDateTime endDate;
    @Min(1) private int quantityBooked;

}
