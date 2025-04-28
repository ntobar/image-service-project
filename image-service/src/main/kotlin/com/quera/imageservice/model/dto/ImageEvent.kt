package com.quera.imageservice.model.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DTO for Server-Sent Events about image changes and system status.
 */
data class ImageEvent(
    val type: EventType,
    @JsonProperty("image_id")
    val imageId: String,
    @JsonProperty("image_name")
    val imageName: String
)

/**
 * Types of events that can be emitted through the SSE stream:
 * - UPLOAD: When a new image is uploaded
 * - DELETE: When an image is deleted
 * - HEARTBEAT: System status check, emitted every 5 seconds
 */
enum class EventType {
    UPLOAD,
    DELETE,
    HEARTBEAT
} 
