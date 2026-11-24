# User Management System

A simple web application for managing users. It has registration, login, and a dashboard where a logged-in user can add, view, edit and delete users. Built with Spring Boot, MySQL and plain HTML, CSS and JavaScript.

## Features

- Register a new account and log in (session-based)
- Dashboard listing all users
- Add, edit and delete users
- Duplicate email detection
- You cannot delete your own account while logged in
- Responsive layout

## Tech Stack


Technology Stack:
Backend: Java, Spring Boot
Database: MySQL
Frontend: HTML, CSS, and JavaScript

## Getting Started

### 1. Create the database

```sql
CREATE DATABASE usermanagement;
```

The `users` table is created automatically on first run (`spring.jpa.hibernate.ddl-auto=update`).

### 2. Configure the database connection

Edit `src/main/resources/application.properties` and set your own MySQL credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/Your databse name
spring.datasource.username=username
spring.datasource.password=YOUR_PASSWORD
```

### 3. Run the application

```bash
# Windows
mvnw.cmd spring-boot:run


You can also run `UsermanagementSystemApplication` directly from IntelliJ.

### 4. Open the app

Go to <http://localhost:8080/>. It redirects to the login page. Register an account first, then log in.

## Pages

| URL               | Description                     |
|-------------------|---------------------------------|
| `/login.html`     | Log in                          |
| `/register.html`  | Create an account               |
| `/users.html`     | Dashboard with the user table   |
| `/add-user.html`  | Add a new user                  |
| `/edit-user.html?id=1` | Edit an existing user      |

## REST API

All endpoints are under `/api`. Endpoints other than register and login require an active session and return `401` if you are not logged in.

| Method | Endpoint          | Description                       |
|--------|-------------------|-----------------------------------|
| POST   | `/auth/register`  | Register a new user               |
| POST   | `/auth/login`     | Log in and start a session        |
| GET    | `/auth/me`        | Get the logged-in user            |
| POST   | `/auth/logout`    | Log out                           |
| GET    | `/users`          | List all users                    |
| GET    | `/users/{id}`     | Get one user                      |
| POST   | `/users`          | Add a user                        |
| PUT    | `/users/{id}`     | Update a user                     |
| DELETE | `/users/{id}`     | Delete a user                     |

```

### Validation rules

- **Name:** required, 2 to 100 characters
- **Email:** required, valid format, unique
- **Phone:** required, exactly 10 digits
- **Password:** at least 6 characters (optional on update; leave blank to keep the current one)
