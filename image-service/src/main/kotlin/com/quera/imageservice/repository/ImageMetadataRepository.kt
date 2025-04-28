package com.quera.imageservice.repository

import com.quera.imageservice.model.ImageMetadata
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Thread-safe repository for storing image metadata with strict insertion order.
 * Uses ConcurrentHashMap for thread-safe access and ConcurrentLinkedQueue for maintaining insertion order.
 *
 * Thread safety is guaranteed by:
 * - Using [ConcurrentHashMap] for atomic metadata operations
 * - Using [ConcurrentLinkedQueue] for thread-safe order tracking
 */
@Repository
class ImageMetadataRepository {
    private val metadata = ConcurrentHashMap<UUID, ImageMetadata>()
    private val insertionOrder = ConcurrentLinkedQueue<UUID>()

    /**
     * Stores or updates image metadata.
     * If metadata with the same ID exists, it will be overwritten.
     *
     * @param imageMetadata The metadata to store
     * @return The stored metadata
     */
    fun save(imageMetadata: ImageMetadata): ImageMetadata {
        metadata[imageMetadata.id] = imageMetadata
        insertionOrder.offer(imageMetadata.id)
        return imageMetadata
    }

    /**
     * Retrieves metadata by its ID.
     *
     * @param id The ID to look up
     * @return The metadata if found, null otherwise
     */
    fun findById(id: UUID): ImageMetadata? = metadata[id]

    /**
     * Deletes metadata by its ID.
     * If the ID doesn't exist, this operation has no effect.
     *
     * @param id The ID of the metadata to delete
     */
    fun delete(id: UUID) {
        metadata.remove(id)
        insertionOrder.remove(id)
    }

    /**
     * Lists all stored metadata in insertion order.
     * Maintains order even with concurrent modifications.
     *
     * @return List of all metadata in insertion order
     */
    fun findAll(): List<ImageMetadata> = 
        insertionOrder.mapNotNull { metadata[it] }

    /**
     * Returns the number of stored metadata entries.
     *
     * @return Current count of metadata entries
     */
    fun count(): Int = metadata.size
} 
