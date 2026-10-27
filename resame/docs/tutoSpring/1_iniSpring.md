# Initialisation d'un projet Spring

Dans le champ de recherche de vsCode:
```
> Spring Initializr: Generate a Maven Project
```

remplir les champs suivants:
- 4.1.2 vérifier version stable
- Java 17 vérifier version stable 
- domaine: com.julius
- nom du projet: resame

ajouter les dépendances:
- Spring Web
- Spring Data JPA (Object-Relational Mapping)
- Lombok (pour générer automatiquement les getters, setters, constructeurs, etc.)
- Validation (pour valider les entrées utilisateur)
- PostgreSQL Driver


# Structure fichiers

```
src/main/java/com/julius/resame/
├── config/          (fichiers de configuration Spring)
├── controller/      (REST API)
├── dto/             (Data Transfer Objects)
├── exception/       (gestion des erreurs)
├── model/           (entités JPA)
├── repository/      (interfaces JPA DAO)
└── service/         (logique métier)
```
