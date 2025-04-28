package com.quera.imageservice.integration

import com.quera.imageservice.model.dto.ApiResponse
import com.quera.imageservice.model.dto.EventType
import com.quera.imageservice.model.dto.ImageEvent
import com.quera.imageservice.model.dto.ImageMetadataResponse
import com.quera.imageservice.service.ImageService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import reactor.test.StepVerifier
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.UUID
import kotlin.io.path.exists

/**
 * Integration tests for Image Service REST APIs.
 * Tests the complete request/response lifecycle using real application context.
 *
 * Features tested:
 * - Image upload/download with validation
 * - Metadata operations and listing
 * - SSE event streaming with proper reactive verification
 * - Error handling and edge cases
 * - Resource cleanup
 *
 * @property webTestClient WebTestClient instance for making HTTP requests
 * @property imageService Service layer for direct interactions when needed
 * @property storageDir Temporary directory for file storage during tests
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "app.storage.location=\${java.io.tmpdir}/image-service-test-\${random.uuid}"
])
@DisplayName("Image Service Integration Tests")
class ImageServiceIntegrationTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var imageService: ImageService

    @TempDir
    lateinit var storageDir: Path

    private val testImageResource = ClassPathResource("/test-images/valid-image.jpg")
    private val invalidImageResource = ClassPathResource("/test-images/invalid-file.txt")
    private val largeImageResource = ClassPathResource("/test-images/large-image.jpg")

    @BeforeEach
    fun setup() {
        webTestClient = webTestClient.mutate()
            .responseTimeout(Duration.ofSeconds(30))
            .build()
    }

    @AfterEach
    fun cleanup() {
        // Clean up any uploaded files
        if (storageDir.exists()) {
            Files.walk(storageDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete)
        }
    }

    /**
     * Creates a multipart request body for image upload testing.
     *
     * @param resource The ClassPathResource containing the image file
     * @param filename Optional custom filename
     * @param contentType The content type of the file
     * @return MultipartInserter configured with the file data
     */
    private fun createImageUploadBody(
        resource: ClassPathResource,
        filename: String? = null,
        contentType: String = MediaType.IMAGE_JPEG_VALUE
    ): BodyInserters.MultipartInserter {
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("file", resource)
            .filename(filename ?: resource.filename ?: "test-image.jpg")
            .contentType(MediaType.parseMediaType(contentType))
        
        filename?.let { 
            bodyBuilder.part("name", it)
        }
        
        return BodyInserters.fromMultipartData(bodyBuilder.build())
    }

    /**
     * Helper method to upload a test image and return its metadata.
     * Used as a setup step for tests that require an existing image.
     *
     * @param resource The image resource to upload
     * @param filename Optional custom filename
     * @param contentType The content type of the image
     * @return The metadata response of the uploaded image
     */
    private fun uploadTestImage(
        resource: ClassPathResource = testImageResource,
        filename: String? = null,
        contentType: String = MediaType.IMAGE_JPEG_VALUE
    ): ImageMetadataResponse {
        return webTestClient.post()
            .uri("/api/images")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(createImageUploadBody(resource, filename, contentType))
            .exchange()
            .expectStatus().isCreated
            .expectBody<ApiResponse<ImageMetadataResponse>>()
            .returnResult()
            .responseBody!!
            .data!!
    }

    @Nested
    @DisplayName("Upload Endpoint Tests")
    inner class UploadEndpoint {
        @Test
        @DisplayName("Should successfully upload valid image")
        fun uploadValidImage() {
            // Given
            val filename = "test-upload.jpg"

            // When/Then
            webTestClient.post()
                .uri("/api/images")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(createImageUploadBody(testImageResource, filename))
                .exchange()
                .expectStatus().isCreated
                .expectBody<ApiResponse<ImageMetadataResponse>>()
                .value { response ->
                    val metadata = response.data!!
                    assert(metadata.id.isNotBlank()) { "Image ID should not be blank" }
                    assert(metadata.name == filename) { "Image name should match uploaded filename" }
                    assert(metadata.mimeType == MediaType.IMAGE_JPEG_VALUE) { "MIME type should match uploaded file" }
                }
        }

        @Test
        @DisplayName("Should reject upload with invalid MIME type")
        fun rejectInvalidMimeType() {
            // When/Then
            webTestClient.post()
                .uri("/api/images")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(createImageUploadBody(
                    invalidImageResource,
                    "test.txt",
                    MediaType.TEXT_PLAIN_VALUE
                ))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody<ApiResponse<ImageMetadataResponse>>()
                .value { response ->
                    assert(response.error?.contains("Unsupported image type") == true) {
                        "Error message should indicate invalid image type"
                    }
                }
        }

        @Test
        @DisplayName("Should reject oversized file upload")
        fun rejectOversizedFile() {
            // When/Then
            webTestClient.post()
                .uri("/api/images")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(createImageUploadBody(largeImageResource))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody<ApiResponse<ImageMetadataResponse>>()
                .value { response ->
                    assert(response.error?.contains("exceeds maximum allowed size") == true) {
                        "Error message should indicate file size limit exceeded"
                    }
                }
        }
    }

    @Nested
    @DisplayName("Download Endpoint Tests")
    inner class DownloadEndpoint {
        @Test
        @DisplayName("Should successfully download existing image")
        fun downloadExistingImage() {
            // Given
            val uploaded = uploadTestImage()
            
            // When/Then
            webTestClient.get()
                .uri("/api/images/${uploaded.id}")
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.IMAGE_JPEG_VALUE)
                .expectHeader().exists("Content-Disposition")
                .expectBody()
                .consumeWith { response ->
                    assert(response.responseBody != null && response.responseBody!!.isNotEmpty()) {
                        "Response body should contain image data"
                    }
                }
        }

        @Test
        @DisplayName("Should return 404 for non-existent image")
        fun downloadNonExistentImage() {
            // When/Then
            webTestClient.get()
                .uri("/api/images/${UUID.randomUUID()}")
                .exchange()
                .expectStatus().isNotFound
                .expectBody<ApiResponse<Unit>>()
                .value { response ->
                    assert(response.error?.contains("not found") == true) {
                        "Error message should indicate image not found"
                    }
                }
        }
    }

    @Nested
    @DisplayName("Metadata Endpoint Tests")
    inner class MetadataEndpoint {
        @Test
        @DisplayName("Should fetch metadata for existing image")
        fun fetchExistingMetadata() {
            // Given
            val uploaded = uploadTestImage()
            
            // When/Then
            webTestClient.get()
                .uri("/api/images/${uploaded.id}/metadata")
                .exchange()
                .expectStatus().isOk
                .expectBody<ApiResponse<ImageMetadataResponse>>()
                .value { response ->
                    val metadata = response.data!!
                    assert(metadata.id == uploaded.id) { "Image ID should match uploaded image" }
                    assert(metadata.name == uploaded.name) { "Image name should match uploaded image" }
                    assert(metadata.mimeType == uploaded.mimeType) { "MIME type should match uploaded image" }
                }
        }

        @Test
        @DisplayName("Should return 404 for non-existent metadata")
        fun fetchNonExistentMetadata() {
            // When/Then
            webTestClient.get()
                .uri("/api/images/${UUID.randomUUID()}/metadata")
                .exchange()
                .expectStatus().isNotFound
                .expectBody<ApiResponse<Unit>>()
                .value { response ->
                    assert(response.error?.contains("not found") == true) {
                        "Error message should indicate metadata not found"
                    }
                }
        }
    }

    @Nested
    @DisplayName("Delete Endpoint Tests")
    inner class DeleteEndpoint {
        @Test
        @DisplayName("Should successfully delete existing image")
        fun deleteExistingImage() {
            // Given
            val uploaded = uploadTestImage()
            
            // When
            webTestClient.delete()
                .uri("/api/images/${uploaded.id}")
                .exchange()
                .expectStatus().isNoContent

            // Then
            webTestClient.get()
                .uri("/api/images/${uploaded.id}")
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @DisplayName("Should return 404 for non-existent image deletion")
        fun deleteNonExistentImage() {
            // When/Then
            webTestClient.delete()
                .uri("/api/images/${UUID.randomUUID()}")
                .exchange()
                .expectStatus().isNotFound
                .expectBody<ApiResponse<Unit>>()
                .value { response ->
                    assert(response.error?.contains("not found") == true) {
                        "Error message should indicate image not found"
                    }
                }
        }
    }

    @Nested
    @DisplayName("SSE Stream Endpoint Tests")
    inner class StreamEndpoint {
        @Test
        @DisplayName("Should receive events for image operations")
        fun receiveImageOperationEvents() {
            // Given - Setup SSE connection
            val eventFlux = webTestClient.get()
                .uri("/api/images/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk
                .returnResult(ImageEvent::class.java)
                .responseBody

            // When - Perform image operations
            val uploadedId = uploadTestImage().id

            // Then - Verify events
            StepVerifier.create(eventFlux)
                .expectSubscription()
                .expectNextMatches { event -> 
                    event.type == EventType.HEARTBEAT
                }
                .expectNextMatches { event ->
                    event.type == EventType.UPLOAD && event.imageId == uploadedId
                }
                .thenCancel()
                .verify(Duration.ofSeconds(5))
        }

        @Test
        @DisplayName("Should handle multiple concurrent subscribers")
        fun handleConcurrentSubscribers() {
            // Given - Setup multiple SSE connections and collect events
            val events1 = mutableListOf<ImageEvent>()
            val events2 = mutableListOf<ImageEvent>()

            val eventFlux1 = webTestClient.get()
                .uri("/api/images/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk
                .returnResult(ImageEvent::class.java)
                .responseBody
                .doOnNext { events1.add(it) }

            val eventFlux2 = webTestClient.get()
                .uri("/api/images/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk
                .returnResult(ImageEvent::class.java)
                .responseBody
                .doOnNext { events2.add(it) }

            // Subscribe to both streams
            val subscription1 = eventFlux1.subscribe()
            val subscription2 = eventFlux2.subscribe()

            // When - Upload an image
            val uploadedId = uploadTestImage().id

            // Then - Wait briefly and verify events
            Thread.sleep(2000) // Give time for events to propagate

            // Cleanup subscriptions
            subscription1.dispose()
            subscription2.dispose()

            // Verify both subscribers received events
            fun verifyEvents(events: List<ImageEvent>, subscriberNum: Int) {
                // Should have at least one heartbeat
                assert(events.any { it.type == EventType.HEARTBEAT }) {
                    "Subscriber $subscriberNum did not receive any heartbeat events"
                }

                // Should have the upload event
                assert(events.any { it.type == EventType.UPLOAD && it.imageId == uploadedId }) {
                    "Subscriber $subscriberNum did not receive the upload event"
                }
            }

            verifyEvents(events1, 1)
            verifyEvents(events2, 2)
        }

        @Test
        @DisplayName("Should maintain connection with periodic heartbeats")
        fun maintainHeartbeats() {
            // Given
            val eventFlux = webTestClient.get()
                .uri("/api/images/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk
                .returnResult(ImageEvent::class.java)
                .responseBody

            // Then
            StepVerifier.create(eventFlux)
                .expectSubscription()
                .expectNextMatches { it.type == EventType.HEARTBEAT }
                .expectNextMatches { it.type == EventType.HEARTBEAT }
                .thenCancel()
                .verify(Duration.ofSeconds(10))
        }
    }
} 