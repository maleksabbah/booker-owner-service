# booker-owner-service

Owner-facing API for the Booker football court booking platform. Facility owners use this service to register facilities, manage courts (sport, surface, capacity, pricing), define weekly availability windows, create blackout periods, and manage booking lifecycle — marking players as seated on arrival, completing games, or flagging no-shows.

Built with Spring Boot 3.5. All endpoints under `/owner`.

## Architecture

The service follows a standard layered Spring architecture where each layer has one responsibility and depends only on the layer beneath it:

- **Controllers** — thin HTTP handlers. They translate between HTTP requests/responses and DTOs and call the service layer. No business logic lives here.
- **Services** — the business logic. Orchestrates the work, enforces ownership checks (every query filters by the authenticated owner's user ID), and coordinates repositories. Knows nothing about HTTP.
- **Repositories** — data access. Spring Data JPA interfaces that the service layer calls. Raw SQL only where JPA can't express it (the compare-and-swap state transition UPDATEs).
- **Entities** — the JPA/ORM models the repositories persist and return. Mapped to the shared PostgreSQL schema with `jpa.ddl-auto: validate`.
- **DTOs** — the request/response shapes exchanged at the API boundary, kept separate from internal entities.
- **Config** — Spring Security filter chain, JWT configuration, exception handling.

This separation keeps the HTTP layer swappable, the business logic testable in isolation, and the data layer free to change without touching the rest.

The service connects directly to the shared PostgreSQL database (no Liquibase — schema is owned by `booker-database`). It reads and writes facilities, courts, availability, and blackouts. It reads bookings and booking_participants. It writes booking state transitions and idempotency_records.

## Key endpoints

```
POST   /owner/facilities                    Create a facility
GET    /owner/facilities                    List owned facilities
GET    /owner/facilities/{id}               Facility details
PATCH  /owner/facilities/{id}               Update facility
DELETE /owner/facilities/{id}               Soft-delete facility

POST   /owner/facilities/{id}/courts        Add a court
GET    /owner/facilities/{id}/courts        List courts at facility
GET    /owner/courts/{id}                   Court details
PATCH  /owner/courts/{id}                   Update court
DELETE /owner/courts/{id}                   Soft-delete court

GET    /owner/courts/{id}/availability      Weekly availability schedule
PUT    /owner/courts/{id}/availability      Replace weekly schedule

POST   /owner/courts/{id}/blackouts         Create a blackout period
GET    /owner/courts/{id}/blackouts         List blackouts
DELETE /owner/blackouts/{id}                Remove a blackout

GET    /owner/courts/{id}/bookings          Court booking schedule
GET    /owner/bookings/{id}                 Booking details + participants
POST   /owner/bookings/{id}/seat            CONFIRMED → SEATED
POST   /owner/bookings/{id}/complete        SEATED → COMPLETED
POST   /owner/bookings/{id}/no-show         CONFIRMED → NO_SHOW
```

## State transitions

Booking state changes use compare-and-swap atomic UPDATEs — the UPDATE includes a `WHERE state = :expectedState` clause so concurrent transitions are detected (zero rows affected = stale state). All three transition endpoints require an `Idempotency-Key` header for safe retries.

Every owner-scoped query filters by the authenticated user's ownership chain (`facility.owner_user_id = :me`), so an owner can never see or modify another owner's data.

## Tech stack

Java 21, Spring Boot 3.5, Spring Security, JWT, Spring Data JPA, Hibernate, PostgreSQL 16, Lombok, Maven, Docker.

Part of a multi-service system — see the [platform overview](https://github.com/maleksabbah/booker-deploy) for the full architecture, booking flow, and the other services.
