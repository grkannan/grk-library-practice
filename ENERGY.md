# Media Upload API Documentation

This controller manages file uploads (images and videos) to Cloudinary via a Spring Boot REST API.

---

## 1. Controller Code

```java
package com.example.demo.controller;

import com.example.demo.service.CloudinaryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaUploadController {

    private final CloudinaryService cloudinaryService;

    public MediaUploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Upload an image or video file to Cloudinary.
     *
     * @param file The file sent as multipart/form-data
     * @return Metadata including CDN secure URL, public ID, format, and media type
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please provide a non-empty file"));
        }

        try {
            Map<String, Object> result = cloudinaryService.uploadFile(file);

            // Extract Cloudinary metadata
            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            String format = (String) result.get("format");
            String resourceType = (String) result.get("resource_type"); // "image" or "video"

            // TODO: Persist metadata (url, publicId, etc.) into your database entity

            return ResponseEntity.ok(Map.of(
                "url", url,
                "publicId", publicId,
                "type", resourceType,
                "format", format
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Upload failed",
                "message", e.getMessage()
            ));
        }
    }
}
```

---

## 2. API Endpoint Details

* **Method**: `POST`
* **URL**: `/api/media/upload`
* **Content-Type**: `multipart/form-data`
* **Body Form Field**: `file` (Binary file)

---

## 3. Sample Response

### Success Response (`200 OK`)

```json
{
  "publicId": "springboot_uploads/sample_vid123",
  "format": "mp4",
  "type": "video",
  "url": "[https://res.cloudinary.com/dhsmy0jkh/video/upload/v1715000000/springboot_uploads/sample_vid123.mp4](https://res.cloudinary.com/dhsmy0jkh/video/upload/v1715000000/springboot_uploads/sample_vid123.mp4)"
}
```

### Error Response (`400 Bad Request` or `500 Internal Server Error`)

```json
{
  "error": "Upload failed",
  "message": "File size exceeds configured limit"
}
```

---

## 4. Testing with cURL

```bash
curl -X POST http://localhost:8080/api/media/upload \
  -F "file=@/path/to/your/image-or-video.mp4"
```