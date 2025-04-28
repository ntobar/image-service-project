package com.quera.imageservice.service

import com.quera.imageservice.model.ImageMetadata
import com.quera.imageservice.model.dto.ImageUploadRequest
import org.springframework.core.io.Resource
import java.util.UUID

/**
 * Service interface for managing images and their metadata.
 * Coordinates storage and metadata operations while providing a clean API for controllers.
 */
interface ImageService {
    /**
     * Uploads a new image and stores its metadata.
     * @param request The upload request containing the image file and optional custom name
     * @return Metadata of the stored image
     * @throws IllegalArgumentException if the request is invalid
     */
    fun uploadImage(request: ImageUploadRequest): ImageMetadata

    /**
     * Retrieves metadata for an image.
     * @param id The ID of the image
     * @return The image metadata if found
     * @throws NoSuchElementException if the image doesn't exist
     */
    fun getImageMetadata(id: UUID): ImageMetadata

    /**
     * Retrieves the image file as a byte array.
     * @param id The ID of the image
     * @return The image data as bytes
     * @throws NoSuchElementException if the image doesn't exist
     */
    fun getImageFile(id: UUID): ByteArray

    /**
     * Deletes an image and its metadata.
     * @param id The ID of the image to delete
     * @throws NoSuchElementException if the image doesn't exist
     */
    fun deleteImage(id: UUID)

    /**
     * Lists all image metadata in insertion order.
     * @return List of all image metadata
     */
    fun getAllImages(): List<ImageMetadata>
} 