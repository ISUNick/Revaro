# Revaro

Revaro is a platform for finding and posting car meets, shows, track days, cruises, and other automotive events. Built with Spring Boot and Thymeleaf.

## Stack

- Java 21, Spring Boot 3.2
- Thymeleaf + Bootstrap 5.3
- Spring Security 6
- PostgreSQL 16 with Spring Data JPA
- Docker / Docker Compose

## Project structure

src/main/java/com/revaro/
config/ security and web MVC config
controller/ MVC controllers
dto/ form/request objects
entity/ JPA entities
enums/ EventType, Role, etc.
repository/ Spring Data repositories
security/ UserDetailsService and auth helpers
service/ business logic
util/ file upload and other helpers

src/main/resources/
static/css/ revaro.css
static/js/ page scripts
templates/
fragments/ shared layout
auth/ login, register
event/ create, edit, detail
admin/ admin dashboard
