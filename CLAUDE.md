# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Terrain is the primary REST API gateway for the CyVerse Discovery Environment. It's a Clojure application that validates
user authentication and coordinates calls to other web services.

## Code Formatting

- Please follow the [clojure community style guidelines][1] when generating new code.
- Please try to avoid repeated code when possible.
- Please try to keep line lenghts to 120 characters or fewer for readability.

## Common Development Commands

### Build and Run
- `lein uberjar` - Build the standalone JAR
- `lein ring server` - Start development server on port 31325
- `lein run` - Run the main application

### Code Quality
- `lein eastwood` - Run linter (configured to check for wrong-arity, wrong-ns-form, wrong-pre-post, wrong-tag,
  misplaced-docstrings)
- `lein cljfmt check` - Check code formatting
- `lein cljfmt fix` - Fix code formatting
- `lein ancient` - Check for outdated dependencies

### Testing
- `lein test` - Run all tests
- `lein test :only terrain.test-namespace` - Run specific test namespace
- `lein test2junit` - Run tests with JUnit XML output

### REPL Development
- `lein repl` - Start REPL
- nREPL server runs on port 7888 when application starts

## Architecture Overview

### Core Structure
The application follows a typical Clojure web service architecture with clear separation of concerns:

1. **Entry Point** (`src/terrain/core.clj`)
   - Main application initialization
   - Configuration loading from `terrain.properties`
   - nREPL server setup
   - NATS message queue connection

2. **Routing Layer** (`src/terrain/routes.clj` and `src/terrain/routes/`)
   - Compojure-based routing
   - Routes organized by domain (apps, filesystem, metadata, etc.)
   - Authentication middleware wrapping
   - Separate admin routes for privileged operations

3. **Service Layer** (`src/terrain/services/`)
   - Business logic implementation
   - Key service modules:
     - `admin.clj` - Miscellaneous administrative operations
     - `bags.clj` - Bag management for bulk operations
     - `bootstrap.clj` - Operations for preparing a user's workspace
     - `coge.clj` - Integration for CoGe (Comparative Genomics)
     - `collaborator_lists.clj` - Operations for managing lists of collaborators
     - `communities.clj` - Operations for managing communities in the Discovery Environment
     - `fileio/` - iRODS file I/O operations
     - `filesystem/` - iRODS filesystem operations
     - `metadata/` - App and data metadata management
     - `oauth.clj` - OAuth integration
     - `permanent_id_requests.clj` - Operations for managing permanent ID requests
     - `qms.clj` - Quota Management System integration
     - `requests.clj` - Operations for managing administrative requests
     - `sharing.clj` - Operations for sharing data
     - `subjects.clj` - Operations for searching for users and groups
     - `teams.clj` - Operations for managing teams in the Discovery Environment
     - `user_info.clj` - User profile management
     - `user_prefs.clj` - User preference management
     - `user_sessions.clj` - User session management

4. **Client Layer** (`src/terrain/clients/`)
   - External service integrations
   - HTTP clients for microservices communication

5. **Middleware** (`src/terrain/middleware.clj`)
   - Request/response transformation
   - User context injection
   - Logging and monitoring
   - Authentication

### Key Dependencies
- **Web Framework**: Compojure + Ring
- **HTTP Client**: clj-http
- **JSON**: Cheshire
- **Database**: PostgreSQL via Kameleon library
- **Message Queue**: NATS
- **Storage**: iRODS (via clj-jargon)
- **Authentication**: CyVerse custom auth + OAuth and Keycloak

### Configuration
The application expects configuration in `terrain.properties`, loaded from (in order):
1. `$IPLANT_CONF_DIR/terrain.properties`
2. Current working directory
3. Classpath resources
4. Fails if not found

### Service Integration Pattern
Terrain acts as an API gateway, delegating to specialized microservices:
- Metadata service for app/data metadata
- Permissions service for access control
- Groups service for team management
- Async tasks service for job management

Each integration typically follows the pattern:
1. Route handler validates request
2. Request and response bodies are validated by `plumatic/schema`
3. Service layer orchestrates calls to one or more clients
4. Clients make HTTP requests to external services
5. Responses are transformed and returned to caller

### Error Handling
- Terrain has middleware that automatically catches HTTP status code errors from client calls
- In most cases, there is **no need to explicitly catch and handle HTTP errors** (404, 500, etc.) in client functions
- The middleware will handle these errors and return appropriate responses to the API caller
- Only add explicit error handling when you need custom error messages or special error recovery logic

[1]: https://guide.clojure.style/
