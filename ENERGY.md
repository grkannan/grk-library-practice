# Complete Architectural Guide: Media Management with Spring Boot & Cloudinary

This guide breaks down every layer of an enterprise-grade Spring Boot application designed to manage media (images and videos) via Cloudinary. It includes full code, component responsibilities, error handling, DTO patterns, and step-by-step explanations.

---

## 1. Architectural Overview

```
[ HTTP Client (Postman / React) ]
              │
              ▼
   ┌──────────────────────┐
   │   Controller Layer   │  ── Parses multipart request, validates HTTP boundaries
   └──────────┬───────────┘
              │ Passes MultipartFile / IDs
              ▼
   ┌──────────────────────┐
   │    Service Layer     │  ── Orchestrates Cloudinary upload, builds entities, manages rollback
   └──────┬──────────┬────┘
          │          │
          │          ▼
          │   ┌──────────────────────┐
          │   │  Cloudinary Client   │  ── Streams bytes to CDN bucket, returns URLs
          │   └──────────────────────┘
          ▼
   ┌──────────────────────┐
   │   Repository Layer   │  ── Talks to database (MySQL/PostgreSQL) via Spring Data JPA
   └──────────┬───────────┘
              │
              ▼
   ┌──────────────────────┐
   │    Database (DB)     │  ── Stores only metadata (URLs, IDs, sizes), never raw binary files
   └──────────────────────┘
```

---

## 2. Configuration Layer

### Why this layer exists:
* Externalizes API keys to keep them out of source code.
* Configures Spring's `MultipartResolver` to accept large video files.
* Produces a thread-safe `Cloudinary` singleton bean managed by the Spring IoC container.

#### `src/main/resources/application.properties`
```properties
# Cloudinary API Credentials
cloudinary.cloud-name=dhsmy0jkh
cloudinary.api-key=YOUR_API_KEY
cloudinary.api-secret=YOUR_API_SECRET

# Increase limits for high-resolution images and large videos
spring.servlet.multipart.max-file-size=150MB
spring.servlet.multipart.max-request-size=150MB

# Database connection (MySQL example)
spring.datasource.url=jdbc:mysql://localhost:3306/media_db?createDatabaseIfNotExist=true&useSSL=false
spring.datasource.username=root
spring.datasource.password=rootpassword
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

#### `com/example/demo/config/CloudinaryConfig.java`
```java
package com.example.demo.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true // Ensures all generated delivery URLs use HTTPS
        ));
    }
}
```

---

## 3. Domain & Data Access Layer (Entity & Repository)

### Why this layer exists:
* Persists file metadata rather than BLOBs. Databases are bad at serving and caching raw video/image binaries; Cloudinary handles the caching and delivery, while your database keeps queryable references.

#### `com/example/demo/entity/MediaAsset.java`
```java
package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "media_assets")
public class MediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFilename;

    // The unique identifier returned by Cloudinary (needed to delete/update files)
    @Column(nullable = false, unique = true)
    private String publicId;

    // The public HTTPS CDN URL to show on frontends
    @Column(nullable = false, length = 1024)
    private String secureUrl;

    private String format;       // e.g., "mp4", "jpg", "png"
    private String resourceType; // "image" or "video"
    private Long sizeInBytes;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public MediaAsset() {}

    public MediaAsset(String originalFilename, String publicId, String secureUrl, 
                      String format, String resourceType, Long sizeInBytes) {
        this.originalFilename = originalFilename;
        this.publicId = publicId;
        this.secureUrl = secureUrl;
        this.format = format;
        this.resourceType = resourceType;
        this.sizeInBytes = sizeInBytes;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getSecureUrl() { return secureUrl; }
    public void setSecureUrl(String secureUrl) { this.secureUrl = secureUrl; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Long getSizeInBytes() { return sizeInBytes; }
    public void setSizeInBytes(Long sizeInBytes) { this.sizeInBytes = sizeInBytes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

#### `com/example/demo/repository/MediaRepository.java`
```java
package com.example.demo.repository;

import com.example.demo.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<MediaAsset, Long> {
    // Allows lookups by Cloudinary public_id
    Optional<MediaAsset> findByPublicId(String publicId);
}
```

---

## 4. DTO (Data Transfer Object) Layer

### Why this layer exists:
* Decouples internal database schema from the public API contract.
* Prevents leaking unnecessary internal fields to clients.

#### `com/example/demo/dto/MediaResponseDTO.java`
```java
package com.example.demo.dto;

import com.example.demo.entity.MediaAsset;
import java.time.LocalDateTime;

public record MediaResponseDTO(
    Long id,
    String fileName,
    String url,
    String fileType,
    String extension,
    Long size,
    LocalDateTime uploadedAt
) {
    public static MediaResponseDTO fromEntity(MediaAsset entity) {
        return new MediaResponseDTO(
            entity.getId(),
            entity.getOriginalFilename(),
            entity.getSecureUrl(),
            entity.getResourceType(),
            entity.getFormat(),
            entity.getSizeInBytes(),
            entity.getCreatedAt()
        );
    }
}
```

---

## 5. Service Layer (Business Logic)

### Why this layer exists:
* Contains all application logic and handles transactions.
* Streams the incoming multipart file to Cloudinary.
* Performs bidirectional cleanup: if an item is deleted, it deletes the file from Cloudinary first, then removes the record from MySQL.

#### `com/example/demo/service/MediaService.java`
```java
package com.example.demo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.dto.MediaResponseDTO;
import com.example.demo.entity.MediaAsset;
import com.example.demo.repository.MediaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class MediaService {

    private final Cloudinary cloudinary;
    private final MediaRepository mediaRepository;

    public MediaService(Cloudinary cloudinary, MediaRepository mediaRepository) {
        this.cloudinary = cloudinary;
        this.mediaRepository = mediaRepository;
    }

    @Transactional
    public MediaResponseDTO uploadMedia(MultipartFile file) throws IOException {
        // resource_type = "auto" instructs Cloudinary to detect images vs videos automatically
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
            "resource_type", "auto",
            "folder", "media_uploads"
        ));

        // Read metadata from Cloudinary response map
        String publicId = (String) uploadResult.get("public_id");
        String secureUrl = (String) uploadResult.get("secure_url");
        String format = (String) uploadResult.get("format");
        String resourceType = (String) uploadResult.get("resource_type");
        Long size = ((Number) uploadResult.get("bytes")).longValue();

        // Build and save entity
        MediaAsset asset = new MediaAsset(
            file.getOriginalFilename(),
            publicId,
            secureUrl,
            format,
            resourceType,
            size
        );

        MediaAsset saved = mediaRepository.save(asset);
        return MediaResponseDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<MediaResponseDTO> getAllMedia() {
        return mediaRepository.findAll()
                .stream()
                .map(MediaResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public MediaResponseDTO getMediaById(Long id) {
        MediaAsset asset = mediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Media not found with id: " + id));
        return MediaResponseDTO.fromEntity(asset);
    }

    @Transactional
    public void deleteMedia(Long id) throws IOException {
        MediaAsset asset = mediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Media not found with id: " + id));

        // 1. Delete remote asset from Cloudinary
        cloudinary.uploader().destroy(asset.getPublicId(), ObjectUtils.asMap(
            "resource_type", asset.getResourceType()
        ));

        // 2. Delete database entry
        mediaRepository.delete(asset);
    }
}
```

---

## 6. Controller Layer (Presentation / REST API)

### Why this layer exists:
* Intercepts incoming HTTP requests, validates file presence, and formats outgoing responses.

#### `com/example/demo/controller/MediaController.java`
```java
package com.example.demo.controller;

import com.example.demo.dto.MediaResponseDTO;
import com.example.demo.service.MediaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please provide a valid file"));
        }

        try {
            MediaResponseDTO response = mediaService.uploadMedia(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cloudinary upload failed", "details", ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<MediaResponseDTO>> listAll() {
        return ResponseEntity.ok(mediaService.getAllMedia());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(mediaService.getMediaById(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            mediaService.deleteMedia(id);
            return ResponseEntity.ok(Map.of("message", "Media deleted successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cloudinary deletion failed", "details", ex.getMessage()));
        }
    }
}
```

---

## 7. Global Exception Handling Layer

### Why this layer exists:
* Intercepts `MaxUploadSizeExceededException` when a user tries to upload a video that exceeds the configured file size limit (e.g., > 150MB), preventing messy stack traces.

#### `com/example/demo/exception/GlobalExceptionHandler.java`
```java
package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
            "error", "File too large",
            "message", "The uploaded file exceeds the configured maximum upload limit (150MB)."
        ));
    }
}
```

---

## 8. Summary of Responsibilities

| Layer | Primary Role | What It Talks To |
| :--- | :--- | :--- |
| **Config** | Loads API secrets and builds bean instances. | Environment properties (`application.properties`) |
| **Controller** | Handles HTTP requests, content-types, status codes. | Service Layer |
| **Service** | Executes business logic, coordinates Cloudinary and DB calls. | Cloudinary SDK & Repository |
| **Entity / DTO** | Represents database records and controls public API contracts. | JPA / JSON Serializer |
| **Repository** | Performs CRUD operations against SQL database. | Database Engine (MySQL/Postgres) |
| **Exception Handler** | Converts low-level framework errors into clean JSON responses. | Spring DispatcherServlet |
