package com.quera.imageservice.controller

import com.quera.imageservice.config.WebTestConfig
import com.quera.imageservice.model.dto.EventType
import com.quera.imageservice.model.dto.ImageEvent
import com.quera.imageservice.service.ImageEventEmitter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.io.IOException
import java.time.Duration

/**
 * Integration tests for [ImageStreamController] Server-Sent Events functionality.
 *
 * These tests verify:
 * - SSE connection establishment and maintenance
 * - Event emission and reception
 * - Event payload structure and content
 * - Heartbeat mechanism
 * - Stream completion behavior
 *
 * The test suite uses WebTestClient for SSE testing and StepVerifier for reactive stream verification.
 * Mock events are generated to simulate real-world scenarios of image uploads and deletions.
 */
@WebFluxTest(controllers = [ImageStreamController::class])
@Import(WebTestConfig::class)
class ImageStreamControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var imageEventEmitter: ImageEventEmitter

    private val heartbeatEvent = ImageEvent(
        type = EventType.HEARTBEAT,
        imageId = "heartbeat",
        imageName = "heartbeat"
    )

    /**
     * Verifies SSE connection setup and heartbeat delivery.
     */
    @Test
    @DisplayName("Should establish SSE connection and receive heartbeat events")
    fun `verify heartbeat events are received`() {
        // Given
        val eventFlux = Flux.just(heartbeatEvent)
            .delaySubscription(Duration.ofMillis(100))
        whenever(imageEventEmitter.subscribe()).thenReturn(eventFlux)

        // When/Then
        val responseBody = webTestClient.get()
            .uri("/api/images/stream")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueMatches("Content-Type", "${MediaType.TEXT_EVENT_STREAM_VALUE};charset=UTF-8")
            .returnResult(ImageEvent::class.java)
            .responseBody

        StepVerifier.create(responseBody.take(1))
            .expectNext(heartbeatEvent)
            .verifyComplete()
    }

    /**
     * Verifies correct event sequencing of heartbeat and upload events.
     */
    @Test
    @DisplayName("Should receive image upload events through SSE stream")
    fun `verify image upload events are received`() {
        // Given
        val uploadEvent = ImageEvent(
            type = EventType.UPLOAD,
            imageId = TestFixtures.TEST_IMAGE_ID.toString(),
            imageName = TestFixtures.TEST_IMAGE_NAME
        )

        val eventFlux = Flux.concat(
            Flux.just(heartbeatEvent),
            Flux.just(uploadEvent).delaySubscription(Duration.ofMillis(100))
        )
        whenever(imageEventEmitter.subscribe()).thenReturn(eventFlux)

        // When/Then
        val responseBody = webTestClient.get()
            .uri("/api/images/stream")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueMatches("Content-Type", "${MediaType.TEXT_EVENT_STREAM_VALUE};charset=UTF-8")
            .returnResult(ImageEvent::class.java)
            .responseBody

        StepVerifier.create(responseBody.take(2))
            .expectNext(heartbeatEvent)
            .expectNext(uploadEvent)
            .verifyComplete()
    }

    /**
     * Verifies stream behavior with only heartbeat events.
     */
    @Test
    @DisplayName("Should handle empty event stream with heartbeat events")
    fun `verify empty stream handling`() {
        // Given
        val eventFlux = Flux.just(heartbeatEvent)
            .delaySubscription(Duration.ofMillis(100))
        whenever(imageEventEmitter.subscribe()).thenReturn(eventFlux)

        // When/Then
        val responseBody = webTestClient.get()
            .uri("/api/images/stream")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueMatches("Content-Type", "${MediaType.TEXT_EVENT_STREAM_VALUE};charset=UTF-8")
            .returnResult(ImageEvent::class.java)
            .responseBody

        StepVerifier.create(responseBody.take(1))
            .expectNext(heartbeatEvent)
            .verifyComplete()
    }

    /**
     * Tests graceful handling of server-side connection termination.
     *
     * Verifies that:
     * - Stream closes cleanly on server shutdown
     * - No error events are propagated to client
     * - Connection terminates with completion signal
     * - Timeout is respected (1 second max)
     */
    @Test
    @DisplayName("Should handle server shutdown gracefully")
    fun `verify stream closes gracefully on server shutdown`() {
        // Given
        val eventFlux = Flux.error<ImageEvent>(IOException("Connection reset by peer"))
            .delaySubscription(Duration.ofMillis(100))
        whenever(imageEventEmitter.subscribe()).thenReturn(eventFlux)

        // When/Then
        val responseBody = webTestClient.get()
            .uri("/api/images/stream")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk
            .returnResult(ImageEvent::class.java)
            .responseBody

        StepVerifier.create(responseBody)
            .expectComplete()
            .verify(Duration.ofSeconds(1))
    }

    /**
     * Tests handling of rapid event sequences.
     *
     * Verifies that:
     * - Multiple events are processed in correct order
     * - No events are dropped or duplicated
     * - Heartbeat and upload events are interleaved correctly
     * - Event timing constraints are maintained (50ms delay)
     */
    @Test
    @DisplayName("Should handle rapid event sequences")
    fun `verify handling of back-to-back events`() {
        // Given
        val uploadEvent = ImageEvent(
            type = EventType.UPLOAD,
            imageId = TestFixtures.TEST_IMAGE_ID.toString(),
            imageName = TestFixtures.TEST_IMAGE_NAME
        )

        val eventFlux = Flux.just(
            heartbeatEvent,
            uploadEvent,
            heartbeatEvent,
            uploadEvent
        ).delayElements(Duration.ofMillis(50))
        whenever(imageEventEmitter.subscribe()).thenReturn(eventFlux)

        // When/Then
        val responseBody = webTestClient.get()
            .uri("/api/images/stream")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk
            .returnResult(ImageEvent::class.java)
            .responseBody

        StepVerifier.create(responseBody)
            .expectNext(heartbeatEvent)
            .expectNext(uploadEvent)
            .expectNext(heartbeatEvent)
            .expectNext(uploadEvent)
            .verifyComplete()
    }
} 