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
@Table(name = "bookable")
public class Bookable {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookableId;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column (length = 255)
    private String description;

    @Column (nullable = false)
    private Integer quantity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;
}
