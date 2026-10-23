package com.julius.resame.model;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter 
@Setter 
@NoArgsConstructor
@AllArgsConstructor 
@Entity 
@Table (name = "book")
public class Book {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long bookId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne
    @JoinColumn(name = "bookable_id", nullable = false)
    private Bookable bookable;

    @Column (nullable = false)
    private LocalDateTime startDate;

    @Column (nullable = false)
    private LocalDateTime endDate;

    @Column (nullable = false)
    private Integer quantityBooked;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 50)
    private Status status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum Status {
        PENDING,
        APPROVED,
        CANCELLED
    }
}
