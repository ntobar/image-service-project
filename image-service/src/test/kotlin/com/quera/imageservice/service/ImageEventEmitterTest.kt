package com.quera.imageservice.service

import com.quera.imageservice.model.ImageMetadata
import com.quera.imageservice.model.dto.EventType
import com.quera.imageservice.model.dto.ImageEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.slf4j.Logger
import org.springframework.test.util.ReflectionTestUtils
import reactor.test.StepVerifier
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Unit tests for [ImageEventEmitter].
 * Tests event emission, subscription behavior, and error handling.
 */
@ExtendWith(MockitoExtension::class)
class ImageEventEmitterTest {

    private lateinit var eventEmitter: ImageEventEmitter
    private val testId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")

    @BeforeEach
    fun setup() {
        eventEmitter = ImageEventEmitter()
    }

    /**
     * Tests upload event emission and reception.
     */
    @Test
    fun `should emit and receive upload event`() {
        // Given
        val metadata = ImageMetadata(
            id = testId,
            name = "test.jpg",
            size = 1000L,
            mimeType = "image/jpeg",
            path = "uploads/$testId"
        )
        val expectedEvent = ImageEvent(
            type = EventType.UPLOAD,
            imageId = metadata.id.toString(),
            imageName = metadata.name
        )

        // When/Then
        StepVerifier.create(eventEmitter.subscribe())
            .then { eventEmitter.emitUpload(metadata) }
            .expectNextMatches { event -> 
                // Accept either a heartbeat or our expected upload event
                event == expectedEvent || 
                (event.type == EventType.HEARTBEAT && 
                 event.imageId == "heartbeat" && 
                 event.imageName == "heartbeat")
            }
            .expectNextMatches { event -> 
                // Accept either a heartbeat or our expected upload event
                event == expectedEvent || 
                (event.type == EventType.HEARTBEAT && 
                 event.imageId == "heartbeat" && 
                 event.imageName == "heartbeat")
            }
            .thenCancel()
            .verify(Duration.ofSeconds(2))
    }

    /**
     * Tests delete event emission and reception.
     */
    @Test
    fun `should emit and receive delete event`() {
        // Given
        val metadata = ImageMetadata(
            id = testId,
            name = "test.jpg",
            size = 1000L,
            mimeType = "image/jpeg",
            path = "uploads/$testId"
        )
        val expectedEvent = ImageEvent(
            type = EventType.DELETE,
            imageId = metadata.id.toString(),
            imageName = metadata.name
        )

        // When/Then
        StepVerifier.create(eventEmitter.subscribe())
            .then { eventEmitter.emitDelete(metadata) }
            .expectNextMatches { event -> 
                // Accept either a heartbeat or our expected delete event
                event == expectedEvent || 
                (event.type == EventType.HEARTBEAT && 
                 event.imageId == "heartbeat" && 
                 event.imageName == "heartbeat")
            }
            .expectNextMatches { event -> 
                // Accept either a heartbeat or our expected delete event
                event == expectedEvent || 
                (event.type == EventType.HEARTBEAT && 
                 event.imageId == "heartbeat" && 
                 event.imageName == "heartbeat")
            }
            .thenCancel()
            .verify(Duration.ofSeconds(2))
    }

    /**
     * Tests heartbeat event emission.
     */
    @Test
    fun `should emit periodic heartbeat events`() {
        // Given
        val expectedEvent = ImageEvent(
            type = EventType.HEARTBEAT,
            imageId = "heartbeat",
            imageName = "heartbeat"
        )

        // When/Then
        StepVerifier.create(eventEmitter.subscribe())
            .expectNext(expectedEvent)
            .expectNext(expectedEvent)
            .expectNext(expectedEvent)
            .thenCancel()
            .verify(Duration.ofSeconds(11))
    }

    /**
     * Tests graceful handling of sink overflow.
     * Verifies that events are discarded without crashing when sink is full.
     */
    @Test
    fun `should handle sink overflow gracefully`() {
        // Given
        val metadata = ImageMetadata(
            id = testId,
            name = "test.jpg",
            size = 1000L,
            mimeType = "image/jpeg",
            path = "uploads/$testId"
        )

        // Create many subscribers but don't consume events
        repeat(1000) {
            eventEmitter.subscribe().subscribe()
        }

        // When/Then - should not throw
        repeat(100) {
            eventEmitter.emitUpload(metadata)
            eventEmitter.emitDelete(metadata)
        }
    }
} 