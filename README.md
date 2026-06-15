# Revaro

**Revaro** is a community-driven platform for discovering, organizing, and discussing automotive events — car meets, shows, track days, cruises, drag racing, autocross, and Cars & Coffee gatherings.

---

## Tech Stack

| Layer         | Technology                          |
|---------------|-------------------------------------|
| Backend       | Java 21, Spring Boot 3.2            |
| Frontend      | Thymeleaf, Bootstrap 5.3            |
| Security      | Spring Security 6                   |
| Persistence   | Spring Data JPA, PostgreSQL 16      |
| Build         | Maven 3.9                           |
| Container     | Docker, Docker Compose              |

---

## Quick Start (Docker)

**Prerequisites:** Docker & Docker Compose installed.

```bash
# 1. Clone the repo
git clone https://github.com/your-username/revaro.git
cd revaro

# 2. Copy env file and set a secure password
cp .env.example .env
# Edit .env → set POSTGRES_PASSWORD

# 3. Build and run
docker compose up --build

# 4. Open in browser
open http://localhost:8080
```

---

## Local Development

**Prerequisites:** Java 21, Maven 3.9, PostgreSQL 16

```bash
# 1. Create the database
psql -U postgres -c "CREATE USER revaro_user WITH PASSWORD 'revaro_pass';"
psql -U postgres -c "CREATE DATABASE revaro OWNER revaro_user;"

# 2. Run the application
./mvnw spring-boot:run
```

---

## Project Structure

```
src/main/java/com/revaro/
├── config/          # Spring config (security, web MVC)
├── controller/      # MVC controllers
├── dto/             # Data Transfer Objects
├── entity/          # JPA entities
├── enums/           # Enumerations (EventType, Role, etc.)
├── repository/      # Spring Data repositories
├── security/        # UserDetailsService, auth helpers
├── service/         # Business logic
└── util/            # Utilities (file upload, etc.)

src/main/resources/
├── static/
│   ├── css/         # revaro.css (global styles)
│   └── js/          # Page-specific JS
├── templates/
│   ├── fragments/   # layout.html (base layout)
│   ├── auth/        # login.html, register.html
│   ├── event/       # create, edit, detail pages
│   └── admin/       # admin dashboard pages
└── application.properties
```

---

## Build Phases

- [x] **Phase 1** – Project setup, Docker, layout, homepage shell
- [ ] **Phase 2** – Database entities & repositories
- [ ] **Phase 3** – Authentication (register, login, roles)
- [ ] **Phase 4** – Event management (CRUD, image upload)
- [ ] **Phase 5** – Homepage discovery (search, filter, sort)
- [ ] **Phase 6** – RSVP & comments
- [ ] **Phase 7** – Claim request system
- [ ] **Phase 8** – Admin dashboard
- [ ] **Phase 9** – Polish & deployment

---

## Design System

| Token              | Value     |
|--------------------|-----------|
| Primary color      | `#0b429c` |
| Main background    | `#1a1a1d` |
| Surface / cards    | `#2a2a2e` |
| Raised surface     | `#333338` |
| Going (green)      | `#2ecc71` |
| Interested (amber) | `#f39c12` |
