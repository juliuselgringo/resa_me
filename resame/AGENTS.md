# Projet resa-me - Contexte Technique

## Stack
- **Backend**: Spring Boot (Java 17)
- **Frontend**: Angular + Tailwind CSS
- **Base de données**: PostgreSQL

## Modèle Logique de Données (MLD)

```
AppUser = (user_id INT, email VARCHAR(100), password VARCHAR(100), role VARCHAR(50), created_at DATETIME);
Group = (group_id INT, name VARCHAR(100), description VARCHAR(200), created_at DATETIME);
Bookable = (bookable_id INT, name VARCHAR(100), description VARCHAR(200), quantity INT, created_at DATETIME, #group_id);
book = (#user_id, #bookable_id, book_id INT, start_date DATETIME, end_date DATETIME, quantity_booked INT, status VARCHAR(50), created_at DATETIME);

```

## Entités

### AppUser
- **PK**: user_id (INT)
- Champs: email, password, role, created_at
- Rôle: Gestion des utilisateurs et authentication

### Bookable  
- **PK**: bookable_id (INT)
- Champs: name, description, quantity, created_at
- Rôle: Articles/ressources réservables (stock géré par quantity)

### Book
- **PK**: book_id (INT)
- **FK**: user_id → AppUser.user_id
- **FK**: bookable_id → Bookable.bookable_id
- Champs: start_date, end_date, quantity_booked, status, created_at
- Rôle: Réservations (relation N:M entre utilisateurs et ressources)

### Group
- **PK**: group_id (INT)
- Champs: name, description, created_at
- Rôle: Catégorisation des Bookable

## Architecture Backend

```
com/julius/resame/
├── config/          (Configuration Spring)
├── controller/      (REST API)
├── dto/             (Data Transfer Objects)
├── exception/       (Gestion des erreurs)
├── model/           (Entités JPA)
├── repository/      (Spring Data JPA)
└── service/         (Logique métier)
```

## Configuration
- `application.properties` **exclu du repo** (via `.gitignore`)
- Utiliser `application.properties.example` comme template

## Comportement attendu
- Guide uniquement : pas de code généré sans demande explicite.
- Réponses courtes. Explications sur demande.
- Bonnes pratiques Spring : layers, DTO, validation, security.