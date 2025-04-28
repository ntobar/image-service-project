# Image Service

A modern image viewing service with real-time updates and multi-frame viewing capabilities.

## Features

- **Backend (Kotlin + Spring Boot)**
  - RESTful API for image CRUD operations
  - Server-Sent Events (SSE) for real-time updates
  - Local disk storage for images
  - In-memory metadata storage
  - Automatic image ordering by upload time
  - Multi-user support
  - Comprehensive unit and integration tests

- **Frontend (React + TypeScript + Vite + Tailwind CSS)**
  - Real-time image cycling with SSE
  - Single and multi-frame viewing modes
  - Adjustable cycle speeds (slow/normal/fast)
  - Up to 10 independent frames
  - Responsive design with Tailwind CSS
  - Component and hook unit tests

## Quick Start

### Prerequisites
- Docker
- Docker Compose

### Running the Service

1. Clone the repository:
   ```bash
   git clone https://github.com/ntobar/image-service-project.git
   cd image-service-project
   ```

2. Build and start the services:
   ```bash
   # Build the images first
   docker-compose build

   # Start the services
   docker-compose up -d
   ```

   Note: The first build might take a few minutes as it needs to:
   - Download base images
   - Install backend dependencies and build the JAR
   - Install frontend dependencies and build the assets

3. Wait for services to be ready:
   ```bash
   # Check service status
   docker-compose ps
   ```
   Both services should show as "running".

4. Access the application:
   - Frontend: http://localhost
   - Backend API: http://localhost:8080/api
   - API Documentation: http://localhost:8080/swagger-ui/index.html

The service will automatically create necessary directories and start both frontend and backend containers.

## Usage

### Uploading Images

You have two options for uploading images:

1. Using Swagger UI (Recommended for testing):
   - Open http://localhost:8080/swagger-ui/index.html
   - Navigate to the "Images" section
   - Use the POST /api/images endpoint
   - Upload your image and optionally set a custom name

2. Using curl or any HTTP client:
   ```bash
   curl -X POST http://localhost/api/images \
     -F "file=@/path/to/image.jpg" \
     -F "name=my-image-name"
   ```

Supported image formats:
- JPEG
- PNG
- GIF
- WebP

### Viewing Images

1. Open http://localhost in your browser
2. Choose between:
   - Single Frame: Shows one frame at a time, cycling through all images
   - Multi-Frame: Shows multiple images cycling independently through all images

### Multi-Frame Features

- Add/remove frames (up to 10)
- Adjust cycle speed (slow: 5s, normal: 3s, fast: 1s)
- Each frame maintains its own position in the cycle

### API Endpoints

All endpoints are documented and testable via Swagger UI: http://localhost:8080/swagger-ui/index.html

Main endpoints:
- `POST /api/images`: Upload new image
- `GET /api/images`: List all images
- `GET /api/images/{id}`: Download image
- `GET /api/images/{id}/metadata`: Get image metadata
- `DELETE /api/images/{id}`: Delete image
- `GET /api/images/stream`: SSE endpoint for real-time updates

## Architecture

### Backend

- RESTful service built with Kotlin and Spring Boot
- Uses Server-Sent Events for real-time notifications
- Local disk storage for images with configurable path
- In-memory storage for metadata (resets on service restart)
- Automatic image ordering by upload timestamp
- OpenAPI documentation with Swagger UI

### Real-Time Updates Implementation

The service uses Server-Sent Events (SSE) for efficient real-time image updates:

#### SSE Architecture
- Reactive endpoints using Spring WebFlux
- Single `/api/images/stream` endpoint handles all real-time events
- Event types:
  - `image.uploaded`: New image added to system
  - `image.deleted`: Image removed from system
  - `heartbeat`: Connection health check during inactivity

#### Event Flow
- Image upload/delete operations trigger events to `ImageEventEmitter`
- Events are broadcast to all connected clients through Kotlin Flow
- Each client maintains its own subscription to the event stream
- Events include minimal metadata to reduce payload size:
  ```json
  {
    "type": "image.uploaded",
    "data": {
      "id": "uuid",
      "name": "filename",
      "timestamp": "iso-date"
    }
  }
  ```

#### Performance Optimizations
- Non-blocking I/O throughout the event chain
- Backpressure handling with Kotlin Flow
- Events are buffered and delivered in order
- Cold observable pattern prevents missed events
- Automatic resource cleanup on client disconnect

### Health Monitoring

The service implements a robust health monitoring system:

#### Server-Side Events (SSE) Heartbeats
- Primary health monitoring mechanism
- Heartbeat events sent only during periods of inactivity (when no image updates are being streamed)
- Purpose is to verify server is alive when no other events are being sent
- 30-second interval between heartbeats during inactive periods
- Detects network interruptions and connection drops
- Automatically handles reconnection attempts with exponential backoff

#### Fallback Health Checks
- Activates only when SSE heartbeats are missed
- Frontend switches to polling mode automatically
- Polls backend health endpoint until SSE connection can be restored
- Handles various failure scenarios:
  - Soft failures (temporary network issues, brief service interruptions)
  - Hard failures (service crash, container down)
  - Connection timeouts
- Recovery strategies:
  - Continuous attempts to restore SSE connection
  - Visual indicators for degraded connection status
  - Automatic retry for failed operations
  - Returns to SSE mode once connection is restored

### Multi-User Support

The service is designed to handle multiple concurrent users efficiently:

#### Real-Time Updates
- All connected users receive real-time updates through SSE
- Each user maintains their own SSE connection
- Updates are broadcast to all connected clients simultaneously
- Zero impact on performance when adding more viewers

#### Independent User Sessions
- Each user has an independent view and control of their UI
- Frame configurations are maintained per-user
- Cycle speeds and positions are tracked separately
- No interference between different users' views

#### Scalability
- Stateless design allows horizontal scaling
- SSE connections are lightweight and efficient
- Independent user sessions don't share state
- Memory usage scales linearly with number of connections

#### Storage Scalability
- Modular storage provider interface allows easy switching of storage backends
- Current local disk implementation can be replaced without changing application logic
- Ready for cloud storage providers:
  - Amazon S3
  - Google Cloud Storage
  - Azure Blob Storage
- Storage implementation details isolated through `ImageStorageService` interface
- Metadata and file storage can be scaled independently
- Zero downtime storage migration possible through abstraction layer

### Frontend

- React with TypeScript for type safety
- Real-time updates using EventSource API
- Fallback polling mechanism for health monitoring
- Responsive design with Tailwind CSS
- Independent frame management for multi-view

#### Build System
- Powered by Vite for modern frontend tooling:
  - Lightning-fast HMR (Hot Module Replacement)
  - Native ESM-based dev server
  - Optimized production builds with rollup
  - Zero-config TypeScript support
  - Smart dependency pre-bundling
- Development benefits:
  - Sub-second cold starts
  - Instant file updates
  - True on-demand compilation
  - Optimized caching for dependencies
  - TypeScript transpilation without type checking in dev

## Testing

### Backend Testing

#### Unit Tests
- JUnit 5 for test framework
- Mockk for Kotlin-friendly mocking
- WebTestClient for reactive endpoint testing
- Test coverage includes:
  - Controller layer (request/response handling)
  - Service layer business logic
  - Repository operations
  - Storage operations
  - Event emission and handling

#### Integration Tests
- Full application context testing
- End-to-end flow validation
- Tests cover:
  - Image upload/download flows
  - SSE connection and event broadcasting
  - Storage operations
  - Error scenarios

### Frontend Testing

#### Test Framework
- Vitest for fast, ESM-native testing
- React Testing Library for component testing
- Jest DOM matchers for DOM assertions

#### Component Tests
- ImageViewer component testing
  - Single/multi-frame mode switching
  - Frame addition/removal with limits
  - Speed control functionality
  - Connection status display
  - Control state management
- ImageCarousel component testing
  - Image cycling with interval control
  - Frame offset management
  - Error state handling
  - Loading and empty states

#### Hook Tests
- useImageStream hook testing
  - Basic connection establishment
  - Connection status management
  - Error state handling
  - Basic event handling with heartbeats

### Test Automation

## Development

### Local Development

If you want to develop locally without Docker:

#### Backend (image-service/)
```bash
cd image-service
./gradlew bootRun
```

#### Frontend (image-platform/)
```bash
cd image-platform
npm install
npm run dev
```

### Docker Development

When using Docker (as described in Quick Start):
- All dependencies are installed automatically during image build
- No need to run `npm install` or `./gradlew` commands manually
- Dependencies are cached in Docker layers for faster rebuilds
- Build process is handled by `docker-compose build`

## Configuration

### Backend

Edit `image-service/src/main/resources/application.properties`:
```properties
# File upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### Frontend

Environment variables in `image-platform/.env`:
```env
VITE_API_BASE_URL=http://localhost:8080
```

## License

[License Type] - See LICENSE file for details 