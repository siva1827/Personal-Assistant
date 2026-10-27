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
npx cap sync android   # the Android project is already in frontend/android
```

## Status

Done: monorepo skeleton, Flyway schema and seed data, register/login/refresh and `GET/PATCH /users/me` with JWT, ingredient search, day themes, pantry CRUD (single, bulk, expiring), recipe history/detail/delete, favorites, and the matching screens (login, register, pantry with editing, recipes, recipe detail, profile) with session restore, CI.

Phase 2: expense categories (defaults created per user), expenses with quick-add, a bank-SMS paste parser, monthly summary with a daily chart, per-category monthly budgets with OK / near / over status, a shopping list (including a recipe's missing ingredients), and the Spending, Budgets and Shopping screens. Android SMS import: a small native plugin reads bank alerts on the phone, they are parsed on-device into drafts you confirm, and a fingerprint stops duplicates (see [docs/LOCAL_SETUP.md](docs/LOCAL_SETUP.md), section 10).

Phase 3: tasks with due dates, daily/weekly/monthly repeating tasks (completing one creates the next), and a notification centre. A scheduled job creates reminders for tasks due or overdue, budgets near or over their limit, and pantry items about to expire; they show under the bell on Home, with optional device alerts while the app is open. Still to do: push messages when the app is closed (needs a Firebase project), Account Aggregator sandbox demo.

On hold: the AI provider integration (`POST /recipes/suggest`, prompt builder, validator). Recipes are read from the database, so this plugs in without changing the existing endpoints.

Next: try the SMS import on a real phone and tune the parser to your bank, push messages via Firebase, then the AI suggestions when you are ready.

## Running the tests

```bash
cd backend && mvn verify      # unit tests
cd frontend && npm test       # Vitest
```
