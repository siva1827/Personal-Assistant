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

For a full step-by-step guide to running this on a new computer, see [docs/LOCAL_SETUP.md](docs/LOCAL_SETUP.md).

Quick version: copy `.env.example` to `.env` and set `JWT_SECRET` (32+ characters). `.env` is git-ignored.

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

Done: monorepo skeleton, Flyway schema and seed data, register/login/refresh and `GET/PATCH /users/me` with JWT, ingredient search, day themes, pantry CRUD (single, bulk, expiring), recipe history/detail/delete, favorites, and the matching screens (login, register, pantry with editing, recipes, recipe detail, profile) with session restore, CI.

Phase 2: expense categories (defaults created per user), expenses with quick-add, a bank-SMS paste parser, monthly summary with a daily chart, per-category monthly budgets with OK / near / over status, a shopping list (including a recipe's missing ingredients), and the Spending, Budgets and Shopping screens. Still to do: automatic SMS reading on Android (Capacitor plugin) and the budget alert job.

Phase 3 (started): tasks with due dates and a Tasks tab. Still to do: push reminders, recurring tasks, Account Aggregator sandbox demo.

On hold: the AI provider integration (`POST /recipes/suggest`, prompt builder, validator). Recipes are read from the database, so this plugs in without changing the existing endpoints.

Next: reminders for tasks, the Android SMS reader, then the AI suggestions when you are ready.

## Running the tests

```bash
cd backend && mvn verify      # unit tests
cd frontend && npm test       # Vitest
```
