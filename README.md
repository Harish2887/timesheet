# Timesheet Application

A full-stack timesheet management application built with Spring Boot (backend) and React (frontend).

## Project Structure

- `backend/`: Spring Boot REST API
- `frontend/`: React frontend

## Prerequisites

- JDK 17
- Maven 3.8+
- Node.js is not required on your machine for production builds (the Maven build downloads Node 20.10.0 and npm 10.2.3 automatically). For local frontend dev, use Node 18+.
- MySQL 8.x

## Development Setup

### Database Setup

1. Create a MySQL database named `timesheet_db`.
2. Configure DB connection in `backend/src/main/resources/application.properties` (defaults are `root`/empty password on localhost).
3. Schema/tables are auto-created on startup (`spring.jpa.hibernate.ddl-auto=update`). Seed data (roles and holiday types) is initialized by `DataInitializer` on first run.

### Running Backend (Spring Boot) in Development Mode

```bash
cd backend
mvn spring-boot:run
```

The backend API will start on http://localhost:8080

### Running Frontend (React) in Development Mode

```bash
cd frontend
npm install
npm run dev
```

The frontend development server will start on http://localhost:3000, proxied to the backend for `/api` calls (see `vite.config.js`).

## Building for Production

The project is configured to build both frontend and backend into a single deployable JAR file.

```bash
cd backend
mvn clean install -DskipTests
```

This will:
1. Download a local Node.js (v20.10.0) and npm (10.2.3) via `frontend-maven-plugin`
2. Run `npm install` and `npm run build` in `frontend/`
3. Copy the built frontend to `backend/target/classes/static`
4. Package everything into `backend/target/timesheet-0.0.1-SNAPSHOT.jar`

## Running the Production Build

After building the application, you can run it with:

```bash
java -jar backend/target/timesheet-0.0.1-SNAPSHOT.jar
```

The application will be available at http://localhost:8080

Tip: After UI changes, restart the app or rebuild to avoid the browser caching old static assets; perform a hard refresh (Cmd/Ctrl+Shift+R).

## Configuration

### Backend Configuration

Main configuration file: `backend/src/main/resources/application.properties`.

Key settings:
- `spring.datasource.*` — database connection
- `jwt.secret` and `jwt.expiration` — JWT configuration used by `JwtUtils`
- `app.frontend.url` — used in email links
- `spring.mail.*` — SMTP settings for email
- Static resources are served from `classpath:/static/` (Vite build is copied here by Maven during packaging)

### Frontend Configuration

- Dev server proxy for `/api` is set in `frontend/vite.config.js` (defaults to `http://localhost:8080`). Override with `VITE_API_PROXY` during dev if needed.
- Axios base URL is relative in production via `src/services/api.interceptor.js`.

### Roles and Access

- Admin users land on `/admin` and can view:
  - Dashboard, User Management, All Timesheets overview
  - Per-user monthly details: `/admin/timesheets/:userId/:year/:month`
  - Export per user and per month
- Employees land on `/dashboard` and can submit detailed daily timesheets, including Support Hours.
- Pay/Subcontractors upload monthly PDF and reported hours.

### New Features

- Support Hours: Users can add “Support Hours” per day in the monthly timesheet form; values are stored on each `TimesheetEntry` and shown in admin details and export files.
- Admin Overview: `/admin/all-timesheets` shows all users’ latest monthly summaries with filters and quick actions.

## Deployment

The packaged JAR file in `backend/target/timesheet-0.0.1-SNAPSHOT.jar` is ready for deployment to any Java-capable environment.

For cloud deployments:
- Set the `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` environment variables to connect to your production database
- Set the `APP_JWT_SECRET` environment variable to a secure random value

## License

[MIT](LICENSE) 