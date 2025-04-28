package com.quera.imageservice.service.storage

import org.springframework.web.multipart.MultipartFile
import java.nio.file.Path
import java.util.UUID

/**
 * Defines the contract for image storage operations.
 * This abstraction allows for different storage implementations (local disk, S3, etc.)
 */
interface ImageStorageService {
    /**
     * Stores an image and returns the path where it was stored.
     * @throws StorageException if the file cannot be stored
     */
    fun store(imageId: UUID, file: MultipartFile): Path

    /**
     * Retrieves an image as a byte array.
     * @throws StorageException if the file cannot be retrieved
     */
    fun retrieve(imageId: UUID): ByteArray

    /**
     * Deletes an image.
     * @throws StorageException if the file cannot be deleted
     */
    fun delete(imageId: UUID)

    /**
     * Initializes the storage system (e.g., creates necessary directories).
     * @throws StorageException if initialization fails
     */
    fun init()
} 