package com.quera.imageservice.controller

import com.quera.imageservice.model.dto.ImageEvent
import com.quera.imageservice.service.ImageEventEmitter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.io.IOException

/**
 * Handles Server-Sent Events (SSE) streaming for image operations.
 * Provides real-time notifications for image uploads, deletions, and system health.
 *
 * @property imageEventEmitter Service that generates image-related events
 */
@RestController
@RequestMapping("/api/images")
@Tag(name = "Image Stream", description = "Server-Sent Events endpoint for image updates")
class ImageStreamController(private val imageEventEmitter: ImageEventEmitter) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Establishes SSE connection for streaming image events.
     * Sends UPLOAD, DELETE, and HEARTBEAT events in real-time.
     * Heartbeat events are sent every 5 seconds.
     *
     * @return Flux of [ImageEvent] representing the SSE stream
     * @throws IOException if client connection is lost
     */
    @Operation(
        summary = "Subscribe to image events",
        description = "Server-Sent Events stream for real-time image notifications"
    )
    @ApiResponses(value = [
        SwaggerResponse(
            responseCode = "200",
            description = "SSE stream of image events"
        )
    ])
    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamEvents(): Flux<ImageEvent> {
        logger.info("Client subscribed to image events stream")
        return imageEventEmitter.subscribe()
            .doOnCancel {
                logger.debug("Client unsubscribed from image events stream")
            }
            .doOnError { error ->
                when (error) {
                    is IOException -> logger.debug("Client connection closed: ${error.message}")
                    else -> logger.error("Error in image events stream", error)
                }
            }
            .onErrorResume { error ->
                when (error) {
                    is IOException -> Flux.empty()
                    else -> Flux.error(error)
                }
            }
    }
} 
