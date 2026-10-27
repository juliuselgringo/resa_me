# Configuration de la base de données

créer la db dans postgresql:
```
CREATE DATABASE resame;
```

Editer le fichier `src/main/resources/application.properties` avec les informations de connexion à la base de données:

```
spring.application.name=resame

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/resame
spring.datasource.username=POSTGRES_USER
spring.datasource.password=POSTGRES_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

# Hibernate
spring.jpa.hibernate.ddl-auto=update
```
**!!! Evidemment ajouter src/main/resources/application.properties à .gitignore !!!**