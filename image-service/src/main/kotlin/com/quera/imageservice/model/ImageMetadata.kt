package com.quera.imageservice.model

import java.time.Instant
import java.util.UUID

/**
 * Represents metadata for a stored image.
 * This is the internal model used for storage and processing.
 */
data class ImageMetadata(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val size: Long,
    val mimeType: String,
    val uploadedAt: Instant = Instant.now(),
    val path: String // Path where the image is stored on disk
) 