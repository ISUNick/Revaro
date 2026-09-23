# Revaro

Revaro is a web app for finding car meets, car shows, cruises, and track days. People post events, RSVP, comment, and earn Rev Points for being active. It's live at [revaromeet.com](https://revaromeet.com).

## Stack

- Java 21, Spring Boot 3.2 (Spring MVC, Spring Security, Spring Data JPA)
- PostgreSQL 16 with the `pg_trgm` extension for fuzzy search
- Thymeleaf templates, Bootstrap 5, plain JavaScript
- Cloudinary for image uploads, Resend for password reset emails
- Deployed on Railway with Docker, DNS through Cloudflare

## Features

- Search that handles typos (pg_trgm similarity), with filters for tags, organizer, location, event name, or accounts
- Results sorted by a mix of how soon an event is and how far away it is, using the browser's location
- Recurring events picked on a calendar, one event per date
- RSVPs, comments with @mentions, likes, and notifications
- Rev Points and a public leaderboard (all time, this week, top organizers)
- Reporting, an admin dashboard for moderation, and event ownership claims
- Profanity filter that censors posts and flags them for review

## Running locally

1. Start Postgres and enable trigram search once:
   ```sql
   CREATE DATABASE revaro;
   \c revaro
   CREATE EXTENSION IF NOT EXISTS pg_trgm;
   ```
2. Copy `.env.example` to `.env` and fill in the Cloudinary and Resend keys if you want image uploads and email to work.
3. Run it:
   ```
   ./mvnw spring-boot:run
   ```
   Then open http://localhost:8080.

Or run the app and database together with `docker compose up`.

## Project layout

```
src/main/java/com/revaro
  config/       security setup and seed data for tags
  controller/   MVC controllers plus a small JSON API for mentions
  service/      business logic
  repository/   Spring Data repositories, including the native search queries
  entity/       JPA entities
  dto/          form objects
src/main/resources
  templates/    Thymeleaf pages, shared pieces live in fragments/layout.html
  static/       revaro.css and page scripts
```
