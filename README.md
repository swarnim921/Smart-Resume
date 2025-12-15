# TalentSync - Smart Resume & Job Matching Platform

A Spring Boot (Java 21) backend with MongoDB Atlas & GridFS storage for secure resume uploads, JWT authentication, role-based access control, and REST APIs for resume management, job listings, and user authentication. Designed to integrate seamlessly with a frontend UI and a separate ML/NLP model for automated resume-job matching.

## Features

- **Java 21 + Spring Boot 3.1.4**
- **MongoDB Atlas** with GridFS for scalable file storage
- **JWT Authentication** with role-based access (ROLE_USER, ROLE_ADMIN)
- **Resume Management** - Upload/download resumes (PDF, up to 20MB)
- **Job Listings** - Job posting and management system
- **ML Integration Ready** - Architecture prepared for AI-powered matching
- **REST APIs** for all operations
- **Postman Collection** included for testing

## Quick Start

### Prerequisites
- Java 21
- Maven 3.6+
- MongoDB Atlas account

### Setup

1. **Configure MongoDB**
   - Update `src/main/resources/application.properties`
   - Replace MongoDB URI with your Atlas connection string

2. **Configure JWT Secret**
   - Set `app.jwt.secret` to a secure 32+ character string

3. **Build & Run**
   ```bash
   mvn clean package
   mvn spring-boot:run
   ```

4. **Test with Postman**
   - Import `postman_collection.json`
   - Start with signup/signin to get JWT token
   - Use token for authenticated endpoints

## API Endpoints

### Authentication
- `POST /api/auth/signup` - User registration
- `POST /api/auth/signin` - User login (returns JWT token)

### Resume Management
- `POST /api/resumes/upload` - Upload resume (requires auth)
- `GET /api/resumes/{id}/download` - Download resume

### Admin
- `GET /api/admin/users` - List all users (admin only)

### Health Check
- `GET /actuator/health` - Application health status

## Architecture Notes

- **GridFS** stores file chunks in MongoDB; `ResumeMeta` stores metadata and references
- **Admin endpoints** require `ROLE_ADMIN` (create admin user via DB or admin setup endpoint)
- **JWT tokens** expire after 24 hours
- **File uploads** support up to 20MB per file

## Future Enhancements

- ML-powered resume-job matching
- Skill extraction and gap analysis
- Course recommendations
- Advanced analytics dashboard

## Tech Stack

- Spring Boot 3.1.4
- Java 21
- MongoDB Atlas
- Spring Security + JWT
- GridFS
- Lombok
- Maven
