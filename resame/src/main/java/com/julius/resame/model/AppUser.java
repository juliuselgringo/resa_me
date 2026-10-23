package com.julius.resame.model;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import java.time.LocalDateTime;


@Getter 
@Setter 
@NoArgsConstructor
@AllArgsConstructor 
@Entity
@Table (name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 100)
    @Email(message = "Email invalide")
    private String email;

    @Column (nullable = false, length = 100)
    // @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    @Enumerated (EnumType.STRING)
    @Column (length = 50)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum Role {
        USER,
        ADMIN
    }

}
