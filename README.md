# Nakama Hub Backend

Backend REST API for a content-sharing platform focused on anime, manga, series, and general discussions.

## 📖 API Documentation

You can explore the full API documentation and test endpoints via Postman:

[![Postman Docs](https://img.shields.io/badge/Postman-API_Docs-orange)](https://documenter.getpostman.com/view/46853536/2sB3WqvgX9)

## 🚀 Technologies Used

- Java 25
- Spring Boot
- Spring Security (JWT)
- JPA / Hibernate
- MySQL
- Maven

## 📦 Features

### 🔐 Authentication
- User registration and login
- Password encryption with BCrypt
- JWT token generation and validation
- Role-based access control (USER, MODERATOR, ADMIN)

### 📝 Posts
- Create and retrieve posts
- Content types: ANIME, MANGA, SERIES, GENERAL
- Automatic timestamps (`createdAt`, `updatedAt`)
- Validation logic for series vs content type
- Category assignment

### 💬 Comments *(in progress)*
- Comment model and endpoints
- Linked to posts and users

## 🧠 Development Workflow

This project follows GitFlow. Each feature is developed in its own branch and merged via Pull Requests.

### Example branches:
- `feature/auth` → [PR #1](https://github.com/JomaorX/nakama-hub-backend/pull/1): Authentication and post module
- `feature/comments` → [PR #2](https://github.com/JomaorX/nakama-hub-backend/pull/2): Comment module 
- `feature/post` → [PR #3](https://github.com/JomaorX/nakama-hub-backend/pull/3): Update post module
## 📂 Folder Structure
```
src/
├── main/
│   ├── java/
│   ├── java/
│   │  └── com/nakamahub/
│   │       ├── config/
│   │       ├── controllers/
│   │       ├── dtos/
│   │       ├── models/
│   │       ├── repositories/
│   │       ├── security/
│   │       └── services/
│   └── resources/
│       └── application.properties
```

## 📌 Notes

• This project is under active development

• Comments module is next

• Future plans: likes, moderation tools, user profiles

## 📬 Contact

Made with ❤️ by José Miguel Martínez [LinkedIn](https://www.linkedin.com/in/martinez97pro) • [GitHub](https://github.com/JomaorX) • [Portfolio](https://jomaor.dev)
