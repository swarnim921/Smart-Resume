Smart Resume / Job Matcher - Backend (Final)

This project includes:
- Java 21 + Spring Boot 3.1.4
- MongoDB Atlas (GridFS for file storage)
- JWT authentication with role-based access (ROLE_USER, ROLE_ADMIN)
- REST endpoints for signup/signin, upload/download resumes, admin listing users
- Postman collection provided (postman_collection.json)

How to use:
1. Replace MongoDB Atlas URI in src/main/resources/application.properties
2. Set app.jwt.secret to a secure 32+ character string
3. Build & run:
   mvn clean package
   mvn spring-boot:run
4. Use Postman to test signup/signin/upload. After signup you receive a token in response.

Notes:
- GridFS stores file chunks in the database; ResumeMeta stores metadata and references the GridFS id.
- Admin-only endpoints require a user with role ROLE_ADMIN (create admin via DB or implement an admin signup flow).
