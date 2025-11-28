Pasteboard (Java) - Spring Boot project
=====================================

This project is a Java (Spring Boot) reimplementation of the uploaded Python `pasteboard.py`.
It supports Java 8 and is compatible with Spring Boot 2.3.x.

Quick start (with Java 8 and Maven):
1. Unzip the archive.
2. Edit `src/main/resources/application.properties` if you want to change the upload root or passcode.
   - Defaults:
     - pasteboard.upload-root = /opt/pasteboard/data
     - pasteboard.passcode = xdxd
3. Build and run:
   mvn package
   java -jar target/pasteboard-0.0.1-SNAPSHOT.jar

Endpoints (same names as original JS expects):
- GET  /           -> index (requires login)
- GET  /login      -> login page
- POST /login      -> login (form field `passcode`)
- GET  /logout     -> logout
- POST /upload_file -> multipart file upload (field name: file)
- POST /upload_text -> JSON body: { "text": "..." }
- GET  /list_periods
- GET  /list_files?year=YYYY&month=MM
- GET  /uploads/{year}/{month}/{filename}

Notes:
- Files are stored under `pasteboard.upload-root` (default /opt/pasteboard/data) in `YYYY/MM` folders.
- The login is a simple session flag (no Spring Security).
- For production, consider adding security, CSRF protection, and stronger session handling.

