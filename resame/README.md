# 📌 Resa-me - Système de Réservation

Système backend Spring Boot pour la gestion de réservations d'objets (ex: livres, outils, salles).
**Frontend**: Angular + Tailwind CSS | **Backend**: Spring Boot (Java 17) | **Base de données**: PostgreSQL

---

---

## 🗃️ **Modèle Logique de Données (MLD)**

| Entité | Champs | Relations | Description |
|--------|--------|-----------|-------------|
| **AppUser** | `user_id`, `email`, `password`, `role`, `created_at` | - | Utilisateur du système (USER ou ADMIN) |
| **Group** | `group_id`, `name`, `description`, `created_at` | - | Catégorie pour les objets réservables |
| **Bookable** | `bookable_id`, `name`, `description`, `quantity`, `created_at` | **→ Group** (`#group_id`) | Objet/ressource réservable (ex: livre, outil) |
| **Book** | `book_id`, `start_date`, `end_date`, `quantity_booked`, `status`, `created_at` | **→ AppUser** (`#user_id`), **→ Bookable** (`#bookable_id`) | Réservation (relation N:M entre utilisateurs et ressources) |

---

---

## 👥 **Rôles et Permissions**

| Rôle | Description | Permissions |
|------|-------------|-------------|
| **USER** | Utilisateur standard | Créer, modifier et annuler **ses propres réservations** (uniquement si `PENDING`) |
| **ADMIN** | Employé/Administrateur | **Toutes les actions** : Créer, modifier, annuler, approuver, retourner n'importe quelle réservation |

---

---

## 📅 **Workflow des Réservations (`Book`)**

### **Statuts Possibles**
| Statut | Description |
|--------|-------------|
| `PENDING` | Réservation créée, en attente d'approbation |
| `APPROVED` | Réservation validée par un ADMIN |
| `RETURNED` | Réservation terminée (objet rendu) |
| `CANCELED` | Réservation annulée (par l'utilisateur ou ADMIN) |
| `OVERDUE` | Réservation expirée (date de fin dépassée) |

---

### **Transitions de Statuts Autorisées**

```
PENDING -->|approveBook (ADMIN)| APPROVED
PENDING -->|cancelBook (USER/ADMIN)| CANCELED
APPROVED -->|markAsReturned (ADMIN)| RETURNED
OVERDUE -->|markAsReturned (ADMIN)| RETURNED
PENDING -->|releaseExpiredBooks (automatique)| OVERDUE
APPROVED -->|releaseExpiredBooks (automatique)| OVERDUE
```

---

### **Détails des Méthodes**

| Méthode | Qui peut l'appeler ? | Statut requis | Action |
|---------|----------------------|----------------|--------|
| **`createBook`** | USER, ADMIN | - | Crée une réservation avec statut `PENDING` et **décrémente le stock** du `Bookable` |
| **`updateBook`** | USER (ses réservations **PENDING**) ou ADMIN | `PENDING` | Modifie une réservation (dates, quantité) |
| **`cancelBook`** | USER (ses réservations **PENDING**) ou ADMIN | `PENDING` | Annule une réservation et **réincrémente le stock** |
| **`approveBook`** | **ADMIN uniquement** | `PENDING` | Approuve une réservation (pas de changement de stock) |
| **`markAsReturned`** | **ADMIN uniquement** | `APPROVED` ou `OVERDUE` | Marque une réservation comme retournée et **réincrémente le stock** |
| **`releaseExpiredBooks`** | **Automatique (toutes les heures)** | `PENDING` ou `APPROVED` (date de fin dépassée) | Passe les réservations expirées en `OVERDUE` |

---

---

## 📦 **Gestion du Stock (`Bookable.quantity`)**

| Action | Effet sur `Bookable.quantity` | Pourquoi ? |
|--------|-------------------------------|------------|
| **`createBook`** | ➖ (décrémente) | Réserve les objets |
| **`updateBook`** | ➖/➕ (ajuste) | Met à jour la quantité réservée |
| **`cancelBook`** | ➕ (réincrémente) | Libère les objets réservés |
| **`markAsReturned`** | ➕ (réincrémente) | Rend les objets disponibles |
| **`approveBook`** | ❌ (aucun) | Approbation logique, pas physique |

> **⚠️ Important** : Le stock est **toujours synchronisé** avec l'état des réservations.

---

---

## 🔒 **Règles de Sécurité et Validation**

### **Vérifications Systématiques**
Toutes les méthodes vérifient :
1. **L'existence** de la réservation (`bookRepository.findById`)
2. **La correspondance des IDs** (`bookId` ≡ `book.getBookId()`)
3. **Les permissions** (USER peut modifier **uniquement ses propres réservations**)
4. **Le statut** (ex: `cancelBook` ne fonctionne que sur `PENDING`)

### **Exceptions Levées**
| Condition | Exception |
|-----------|-----------|
| Réservation inexistante | `IllegalArgumentException("Book with ID X does not exist")` |
| ID mismatch | `IllegalArgumentException("Book ID mismatch")` |
| Permissions insuffisantes | `IllegalArgumentException("You are not authorized to update this book")` |
| Statut invalide | `IllegalArgumentException("Book have to be [STATUT] to be [ACTION]")` |
| Dates invalides | `IllegalArgumentException("Start date must be before end date")` |
| Stock insuffisant | `IllegalArgumentException("Quantity booked cannot exceed available quantity")` |

---

---

## ⚙️ **Tâches Planifiées (Cron Jobs)**

| Méthode | Fréquence | Action |
|---------|-----------|--------|
| **`releaseExpiredBooks`** | Toutes les heures (`0 0 * * * *`) | Passe les réservations `PENDING`/`APPROVED` expirées en `OVERDUE` |

---

---
## 📂 **Structure du Projet**
```
com/julius/resame/
├── config/          # Configuration Spring (SecurityConfig, etc.)
├── controller/     # REST API (à implémenter)
├── dto/            # Data Transfer Objects
├── exception/      # Gestion des erreurs
├── model/          # Entités JPA (AppUser, Book, Bookable, Group)
├── repository/     # Spring Data JPA (AppUserRepository, BookRepository, etc.)
└── service/        # Logique métier (AppUserService, BookService, etc.)
```


---
---
## 🚀 **Prochaines Étapes**

- [ ] Implémenter les **controllers REST** pour exposer les services
- [ ] Ajouter les **DTOs** pour les requêtes/réponses
- [ ] Créer les **tests unitaires** pour `BookService`
- [ ] Développer le **frontend Angular**
- [ ] Configurer la **sécurité Spring Security** (JWT, etc.)

---
---
## 📝 **Notes Techniques**

- **Java 17** + **Spring Boot 3.2.0**
- **Lombok** pour les getters/setters/constructeurs
- **Hibernate** pour la persistance (JPA)
- **PostgreSQL** pour la base de données
- **Maven** pour la gestion des dépendances