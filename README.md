# Daily Life Assistant

A cross-platform personal app where an AI model suggests recipes from your pantry and your theme for the day, with expense and task tracking planned for later phases.

| Layer | Stack |
| --- | --- |
| Frontend | React, TypeScript, Vite, Ionic React, TanStack Query |
| Mobile | Capacitor (Android, iOS, web) |
| Backend | Spring Boot 3, Java 17, Spring Security + JWT, Spring Data JPA |
| Database | MySQL 8 with Flyway migrations |
| AI | LLM API, called only from the backend |

## Phases

1. **Food (current):** pantry, day themes, AI recipe suggestions, favorites.
2. **Expenses:** budgets, charts, shopping list, Android SMS parsing.
3. **Tasks and bank sync:** reminders, Account Aggregator sandbox demo.

## Repository layout

```
backend/    Spring Boot API (package per feature under com.siva.assistant)
frontend/   React + Ionic app, Capacitor config
docker-compose.yml
.github/workflows/ci.yml
```

## Getting started

Copy `.env.example` to `.env` and set `JWT_SECRET` (32+ characters). `.env` is git-ignored.

### Database and backend (Docker)

```bash
docker compose up --build
```

The API is at http://localhost:8080 and Swagger UI at http://localhost:8080/swagger-ui.html.

### Backend without Docker for the app

Needs Java 17 and Maven. Start only MySQL, then run the app with the variables from `.env` exported:

```bash
docker compose up -d mysql
cd backend && mvn spring-boot:run
```

### Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

### Mobile (Capacitor)

```bash
cd frontend
npm run build
npx cap add android   # and `npx cap add ios` on a Mac with Xcode
npx cap sync
```

## Status

Done: monorepo skeleton, Flyway schema and seed data, register/login/refresh and `GET/PATCH /users/me` with JWT, ingredient search, day themes, pantry CRUD (single, bulk, expiring), login/register/pantry screens with session restore, CI.

Next: the AI package and `POST /recipes/suggest`, then favorites.

## Running the tests

```bash
cd backend && mvn verify      # unit tests
cd frontend && npm test       # Vitest
```
