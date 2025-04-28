package com.quera.imageservice.service

import com.quera.imageservice.model.ImageMetadata
import com.quera.imageservice.model.dto.EventType
import com.quera.imageservice.model.dto.ImageEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many
import reactor.core.scheduler.Schedulers
import reactor.util.concurrent.Queues
import java.time.Duration

/**
 * Service responsible for emitting image-related events to Server-Sent Events (SSE) subscribers.
 * Provides real-time notifications for image uploads, deletions, and system health checks.
 *
 * Features:
 * - Thread-safe event emission using Project Reactor's Sinks
 * - Multicast support with backpressure handling
 * - Periodic heartbeat events every 5 seconds
 * - Late subscriber buffering
 */
@Service
class ImageEventEmitter {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    // Using a multicast sink that buffers events for late subscribers
    private val sink: Many<ImageEvent> = Sinks.many()
        .multicast()
        .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false)

    private val heartbeatEvent = ImageEvent(
        type = EventType.HEARTBEAT,
        imageId = "heartbeat",
        imageName = "heartbeat"
    )

    /**
     * Subscribes to the event stream.
     * Combines image-related events with periodic heartbeat events.
     *
     * @return Infinite Flux of [ImageEvent]s including:
     *  - UPLOAD events when images are uploaded
     *  - DELETE events when images are deleted
     *  - HEARTBEAT events every 5 seconds
     */
    fun subscribe(): Flux<ImageEvent> = sink.asFlux()
        .mergeWith(
            Flux.interval(Duration.ZERO, Duration.ofSeconds(5))
                .map { heartbeatEvent }
                .subscribeOn(Schedulers.boundedElastic())
        )

    /**
     * Emits an upload event for a newly stored image.
     *
     * @param metadata Metadata of the uploaded image
     */
    fun emitUpload(metadata: ImageMetadata) {
        emit(ImageEvent(
            type = EventType.UPLOAD,
            imageId = metadata.id.toString(),
            imageName = metadata.name
        ))
    }

    /**
     * Emits a delete event for a removed image.
     *
     * @param metadata Metadata of the deleted image
     */
    fun emitDelete(metadata: ImageMetadata) {
        emit(ImageEvent(
            type = EventType.DELETE,
            imageId = metadata.id.toString(),
            imageName = metadata.name
        ))
    }

    /**
     * Internal helper for emitting events to the sink.
     * Handles emission failures gracefully by logging warnings.
     *
     * @param event The event to emit
     */
    private fun emit(event: ImageEvent) {
        sink.emitNext(
            event
        ) { _, _ ->
            logger.warn("Failed to emit event: $event")
            false // don't retry on failure
        }
    }
} 
