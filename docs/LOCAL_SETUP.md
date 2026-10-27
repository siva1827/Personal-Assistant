# Local setup guide

How to run Daily Assistant (Spring Boot API + MySQL + React/Ionic app) on a new computer.
Works on Windows, macOS and Linux. Allow about 20 minutes the first time.

## 1. What you will end up with

| Part | URL | Notes |
| --- | --- | --- |
| Web app | http://localhost:5173 | React + Ionic, best viewed at phone width |
| API | http://localhost:8080 | Spring Boot |
| API docs (Swagger) | http://localhost:8080/swagger-ui.html | try endpoints in the browser |
| MySQL | localhost:3306 | database `assistant` |

## 2. Install the prerequisites

| Tool | Version | Check with |
| --- | --- | --- |
| Git | any recent | `git --version` |
| Java JDK | 17 or newer | `java -version` |
| Maven | 3.9 or newer | `mvn -v` |
| Node.js | 22 LTS or newer (includes npm) | `node -v` |
| MySQL 8 **or** Docker Desktop | MySQL 8.x, or Docker with Compose v2 | `docker compose version` |

Quick installs:

- **Windows (winget):** `winget install Git.Git EclipseAdoptium.Temurin.17.JDK Apache.Maven OpenJS.NodeJS.LTS Docker.DockerDesktop`
- **macOS (Homebrew):** `brew install git openjdk@17 maven node` and `brew install --cask docker`
- **Ubuntu/Debian:** `sudo apt install git openjdk-17-jdk maven` plus Node from https://nodejs.org and Docker from https://docs.docker.com/engine/install/

Open a **new terminal** after installing so the tools are on your PATH.

> Docker is the easiest route for the database. If you cannot use Docker, follow
> section 4B (MySQL installed directly).

## 3. Get the code

```bash
git clone https://github.com/siva1827/Personal-Assistant.git
cd Personal-Assistant
git checkout feat/initial-setup   # skip once this branch is merged into main
```

## 4. Start the database

### 4A. With Docker (recommended)

1. Create your settings file from the template and edit it:

   ```bash
   cp .env.example .env          # Windows PowerShell: Copy-Item .env.example .env
   ```

   In `.env` set:

   - `JWT_SECRET` to a random string of **at least 32 characters** (required)
   - `DB_PASSWORD` and `DB_ROOT_PASSWORD` to passwords of your choice

   Generate a secret with `openssl rand -base64 48` (macOS/Linux/Git Bash), or in
   PowerShell: `[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])`.

2. Start MySQL only, and wait for it to become healthy:

   ```bash
   docker compose up -d mysql
   docker compose ps          # STATUS should say "healthy"
   ```

   (`docker compose up --build` would also build and start the API in a container.
   For day-to-day development it is simpler to run the API yourself, see section 5.)

### 4B. Without Docker (MySQL installed directly)

1. Install MySQL 8 from https://dev.mysql.com/downloads/mysql/ and start the service.
2. Create the database and a user (use your own password):

   ```sql
   CREATE DATABASE assistant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'assistant'@'localhost' IDENTIFIED BY 'choose-a-password';
   GRANT ALL PRIVILEGES ON assistant.* TO 'assistant'@'localhost';
   ```

   The `utf8mb4` character set matters: category icons are emoji and need 4-byte characters.

Tables are created automatically on first start by the Flyway migrations in
`backend/src/main/resources/db/migration`. You never create tables by hand.

## 5. Run the API

Settings are passed as environment variables. Use the same values you put in `.env`.

**macOS / Linux / Git Bash**

```bash
cd backend
export JWT_SECRET='your-32-plus-character-secret'
export DB_PASSWORD='the DB_PASSWORD you chose'   # 4A: value from .env, 4B: your MySQL password
mvn spring-boot:run
```

**Windows PowerShell**

```powershell
cd backend
$env:JWT_SECRET = 'your-32-plus-character-secret'
$env:DB_PASSWORD = 'the DB_PASSWORD you chose'
mvn spring-boot:run
```

Defaults you can leave alone: `DB_URL=jdbc:mysql://localhost:3306/assistant`, `DB_USER=assistant`,
port `8080`. Override any of them with environment variables of the same name
(`DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`).

Two more optional settings:

- `APP_TIMEZONE` (default `Asia/Kolkata`): decides what "today" means for due dates, budgets and
  reminders. Set it to your own zone, for example `Europe/London`.
- `NOTIFICATION_INTERVAL` (default `PT30M`): how often reminders are generated for all users.
  Reminders are also refreshed whenever the app loads them, so this mostly matters for the
  future push messages.

Success looks like `Started AssistantApplication in ... seconds`. Check it:
open http://localhost:8080/swagger-ui.html.

## 6. Run the web app

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**, choose **Create an account**, and you are in. The app talks
to `http://localhost:8080/api/v1` by default. To point it elsewhere create `frontend/.env.local`:

```
VITE_API_URL=http://localhost:8080/api/v1
```

Tip: press F12, switch on the device toolbar and pick a phone to see the mobile layout.
The app follows your operating system's light or dark mode.

## 7. Quick smoke test

1. Register, sign out, sign in again, reload the page (you should stay signed in).
2. **Pantry:** search "tom", tap a result to add it, tap the item to set quantity and expiry.
3. **Spending:** tap **+**, enter an amount, pick a category, save. Open **Budgets** and set a limit.
4. **Tasks:** add a task with a due date, tick it off; make one repeat weekly and tick it off to see the next one appear. Switch to **Shopping list** and add an item.
5. **Notifications:** tap the bell on Home. A task due today, a budget near its limit or food about to expire shows up there. Profile has an optional **Device alerts** switch.
6. **Recipes** will be empty: AI recipe suggestions are not switched on yet.

## 8. Run the tests

```bash
cd backend  && mvn verify        # backend unit tests
cd frontend && npm test          # frontend tests
cd frontend && npm run lint && npm run build
```

## 9. Using it on your phone (same Wi-Fi)

The API only allows browser requests from known addresses. To open the app from a phone:

1. Find your computer's LAN IP (for example `192.168.1.20`).
2. Start the web app on all interfaces: `npm run dev -- --host`.
3. Start the API with `CORS_ORIGINS` including the address the phone will use:

   ```bash
   export CORS_ORIGINS='http://localhost:5173,http://192.168.1.20:5173'
   ```

4. In `frontend/.env.local` set `VITE_API_URL=http://192.168.1.20:8080/api/v1`, restart `npm run dev`.
5. Open `http://192.168.1.20:5173` on the phone. You may need to allow ports 5173 and 8080
   through the computer's firewall. Only do this on a network you trust.

## 10. Optional: Android app (Capacitor)

Needs Android Studio. From `frontend/`:

```bash
npm run build
npx cap add android      # first time only
npx cap sync
npx cap open android     # opens Android Studio; run on an emulator or device
```

The Android app cannot reach `localhost` on your computer. Set `VITE_API_URL` to your
computer's LAN IP (section 9) and rebuild. iOS needs a Mac with Xcode (`npx cap add ios`).

## 11. Troubleshooting

| Symptom | Likely cause and fix |
| --- | --- |
| API stops with `Could not resolve placeholder 'JWT_SECRET'` | `JWT_SECRET` is not set in the terminal that runs Maven. Set it (section 5). |
| API stops with `WeakKeyException` | `JWT_SECRET` is shorter than 32 characters. |
| `Communications link failure` / `Connection refused` on startup | MySQL is not running or not on port 3306. Run `docker compose ps`, or start the MySQL service. |
| `Access denied for user 'assistant'` | `DB_PASSWORD` does not match the password the database user was created with. With Docker, if you changed passwords after the first start, run `docker compose down -v` to reset the data volume, then start again. |
| `Data truncation: Incorrect string value` when saving a category | The database is not utf8mb4. Recreate it with the `CHARACTER SET utf8mb4` statement in 4B. |
| Browser shows a network error or CORS error | API not running, wrong `VITE_API_URL`, or your origin is not in `CORS_ORIGINS`. |
| Port 8080 or 5173 already in use | Stop the other program, or use `SERVER_PORT=8081` for the API and `npm run dev -- --port 5180` for the app (add the new origin to `CORS_ORIGINS`). |
| `npm install` fails with peer dependency errors | Use Node 22 or newer and delete `frontend/node_modules` and `frontend/package-lock.json` only as a last resort. |
| Page shows old data after switching accounts | Hard refresh (Ctrl+Shift+R), or sign out and in. |

### On a company network with SSL inspection

Corporate proxies re-sign HTTPS traffic, which can break downloads with certificate errors
(`SSL certificate problem: unable to get local issuer certificate`,
`PKIX path building failed`). These fixes keep verification switched on by using the
operating system's trusted certificates (Windows):

- Git: `git -c http.sslBackend=schannel clone ...` (or set it once with
  `git config --global http.sslBackend schannel`)
- Maven (PowerShell): `$env:MAVEN_OPTS = '-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT'`
  (bash: `export MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT`)
- npm: `npm config set cafile <path-to-your-company-root-ca.pem>` if `npm install` fails

Ask your IT team for the company root certificate if these are not enough. Do not disable
certificate checking.

## 12. Resetting

- **Wipe all app data (Docker):** `docker compose down -v` then `docker compose up -d mysql`.
- **Stop everything:** `Ctrl+C` in the API and app terminals, then `docker compose down`.

## 13. Security notes

- `.env` and `frontend/.env.local` are git-ignored. Never commit real secrets.
- Use a different `JWT_SECRET` for anything that is not your own laptop.
- Anything exposed beyond localhost (phone testing, hosting) should sit behind HTTPS.
