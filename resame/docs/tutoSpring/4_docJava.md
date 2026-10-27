# Chronologie du développement

## Objet Optional et List

**`Optional<T>`** et **`List<T>`** servent des cas différents :

---

### **`Optional<Group>`**
- **Contient 0 ou 1 élément** (jamais null)
- **Pourquoi ?** Quand tu cherches **UN seul** élément qui **peut ne pas exister**
- **Exemple** : `findById(Long id)` → un Group **ou rien**
- **Méthodes utiles** :
  - `.isPresent()` → `true` si le Group existe
  - `.get()` → Récupère le Group (ou lance `NoSuchElementException`)
  - `.orElse(...)` → Retourne une valeur par défaut si vide

---

### **`List<Group>`**
- **Contient 0, 1 ou plusieurs éléments**
- **Pourquoi ?** Quand tu cherches **plusieurs** résultats
- **Exemple** : `findAll()`, `findByNameContaining("test")` → une liste de Groups

---

## cron et @Scheduled

Cron Expression

┌───────────── second (0-59) │ ┌───────────── minute (0 - 59) │ │ ┌───────────── hour (0 - 23) │ │ │ ┌───────────── day of the month (1 - 31) │ │ │ │ ┌───────────── month (1 - 12) (or JAN-DEC) │ │ │ │ │ ┌───────────── day of the week (0 - 7) │ │ │ │ │ │ (or MON-SUN -- 0 or 7 is Sunday) │ │ │ │ │ │ * * * * * *

Meaning second minute hour day month day_of_week

0 0 * * * *

top of every hour of every day

*/10 * * * * *

every ten seconds

0 0 8-10 * * *

8, 9 and 10 o’clock of every day

0 0 6,19 * * *

6:00 AM and 7:00 PM every day

0 0/30 8-10 * * *

8:00, 8:30, 9:00, 9:30, 10:00 and 10:30 every day

0 0 9-17 * * MON-FRI

on the hour nine-to-five weekdays

0 0 0 25 12 ?

every Christmas Day at midnight