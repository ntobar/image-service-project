# Image Service

A robust, high-performance RESTful service for managing image uploads and retrievals, built with Spring Boot and Kotlin.

## Overview

Image Service provides a secure and efficient way to upload, store, retrieve, and manage images through a RESTful API. It supports various image formats, handles metadata management, and includes real-time event notifications through Server-Sent Events (SSE).

## Technology Stack

- **Language:** Kotlin 1.9
- **Framework:** Spring Boot 3.2
- **Build Tool:** Gradle 8.13
- **JDK:** Eclipse Temurin 17
- **Documentation:** OpenAPI 3.0 (Swagger)
- **Event Streaming:** Server-Sent Events (SSE)

## Features

- High-performance image upload and retrieval
- Local disk storage with extensible storage interface for future storage provider support
- Comprehensive image metadata management
- Real-time event notifications for uploads and deletions
- Swagger UI documentation
- Error handling and validation
- Detailed logging for monitoring and debugging

## Getting Started

### Local Development

1. Ensure you have JDK 17 installed:
   ```bash
   java -version
   ```

2. Clone the repository:
3. Build the project:
   ```bash
   ./gradlew build
   ```

4. Run the application:
   ```bash
   ./gradlew bootRun
   ```

The service will be available at `http://localhost:8080`

### Docker Deployment

1. Build the Docker image:
   ```bash
   docker build -t image-service .
   ```

2. Run the container:
   ```bash
   docker run -p 8080:8080 -v /your/local/path:/app/uploads image-service
   ```

## API Endpoints

### Upload Image
**POST** `/api/images`  
Response: 201 Created
```bash
curl -X POST http://localhost:8080/api/images \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/image.jpg" \
  -F "name=my-image"
```

### Download Image
**GET** `/api/images/{id}`  
Response: 200 OK
```bash
# Download as attachment
curl -O http://localhost:8080/api/images/123e4567-e89b-12d3-a456-426614174000

# View in browser
curl http://localhost:8080/api/images/123e4567-e89b-12d3-a456-426614174000 \
  -H "Accept: image/*"
```

### Get Image Metadata
**GET** `/api/images/{id}/metadata`  
Response: 200 OK
```bash
curl http://localhost:8080/api/images/123e4567-e89b-12d3-a456-426614174000/metadata
```

### List All Images
**GET** `/api/images`  
Response: 200 OK
```bash
curl http://localhost:8080/api/images
```

### Delete Image
**DELETE** `/api/images/{id}`  
Response: 204 No Content
```bash
curl -X DELETE http://localhost:8080/api/images/123e4567-e89b-12d3-a456-426614174000
```

### Subscribe to Image Events
**GET** `/api/images/stream`  
Response: 200 OK (SSE)
```bash
curl -N http://localhost:8080/api/images/stream
```

## API Documentation

Swagger UI is available at: `http://localhost:8080/swagger-ui.html`

## Tested Endpoints

- [x] Image Upload
- [x] Image Download
- [x] Metadata Retrieval
- [x] Image Deletion
- [x] Image Listing
- [x] Event Streaming
- [x] Error Handling
- [x] Content Type Validation
- [x] File Size Validation

## Configuration

The following properties can be configured in `application.properties`:

```properties
# Storage location for images (default: uploads)
app.storage.location=uploads

# Maximum file upload size (default: 10MB)
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

## Error Handling

The service provides detailed error responses in a consistent format:

```json
{
    "error": "Error message description"
}
```

Common HTTP status codes:
- 400: Invalid request (e.g., unsupported image type)
- 404: Image not found
- 413: File too large
- 500: Server error

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details. 
