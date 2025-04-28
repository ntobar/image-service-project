package com.quera.imageservice.service

import com.quera.imageservice.exception.ImageNotFoundException
import com.quera.imageservice.exception.ImageProcessingException
import com.quera.imageservice.exception.InvalidImageException
import com.quera.imageservice.model.ImageMetadata
import com.quera.imageservice.model.dto.ImageUploadRequest
import com.quera.imageservice.repository.ImageMetadataRepository
import com.quera.imageservice.service.storage.ImageStorageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import java.util.UUID

/**
 * Implementation of [ImageService] that coordinates image storage, metadata management, and event emission.
 * Handles validation, storage operations, and maintains consistency between storage and metadata.
 *
 * @property imageStorageService Service for physical storage of image files
 * @property imageMetadataRepository Repository for image metadata persistence
 * @property imageEventEmitter Service for emitting image-related events
 */
@Service
class ImageServiceImpl(
    private val imageStorageService: ImageStorageService,
    private val imageMetadataRepository: ImageMetadataRepository,
    private val imageEventEmitter: ImageEventEmitter
) : ImageService {

    private val logger = LoggerFactory.getLogger(javaClass)
    
    private val allowedMimeTypes = setOf(
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp"
    )

    /**
     * Validates and processes an image upload request.
     * Stores the image file, creates metadata, and emits an upload event.
     *
     * @param request Upload request containing the image file and optional name
     * @return Metadata of the stored image
     * @throws InvalidImageException if the image is invalid
     * @throws ImageProcessingException if storage or processing fails
     */
    override fun uploadImage(request: ImageUploadRequest): ImageMetadata {
        validateUploadRequest(request)
        
        val id = UUID.randomUUID()
        try {
            // Store the file first
            val storagePath = imageStorageService.store(id, request.file)
            
            // Create and store metadata
            val metadata = ImageMetadata(
                id = id,
                name = request.name ?: request.file.originalFilename ?: "unnamed",
                size = request.file.size,
                mimeType = request.file.contentType ?: MimeTypeUtils.IMAGE_JPEG_VALUE,
                path = storagePath.toString()
            )
            
            val savedMetadata = imageMetadataRepository.save(metadata)
            logger.info("Successfully uploaded image: {}", savedMetadata)
            
            // Emit upload event
            imageEventEmitter.emitUpload(savedMetadata)
            
            return savedMetadata
            
        } catch (e: Exception) {
            logger.error("Failed to upload image", e)
            throw ImageProcessingException("Failed to process image upload", e)
        }
    }

    /**
     * Retrieves metadata for an image by its ID.
     *
     * @param id Unique identifier of the image
     * @return The image metadata
     * @throws ImageNotFoundException if the image doesn't exist
     */
    override fun getImageMetadata(id: UUID): ImageMetadata {
        return imageMetadataRepository.findById(id)
            ?: throw ImageNotFoundException(id.toString())
    }

    /**
     * Retrieves the binary data of an image by its ID.
     * Verifies metadata existence before attempting retrieval.
     *
     * @param id Unique identifier of the image
     * @return The image data as a byte array
     * @throws ImageNotFoundException if the image doesn't exist
     * @throws ImageProcessingException if retrieval fails
     */
    override fun getImageFile(id: UUID): ByteArray {
        // Verify metadata exists first
        getImageMetadata(id)
        
        return try {
            imageStorageService.retrieve(id)
        } catch (e: Exception) {
            logger.error("Failed to retrieve image file with id: {}", id, e)
            throw ImageProcessingException("Failed to retrieve image file", e)
        }
    }

    /**
     * Deletes an image and its associated metadata.
     * Emits a delete event upon successful deletion.
     *
     * @param id Unique identifier of the image to delete
     * @throws ImageNotFoundException if the image doesn't exist
     * @throws ImageProcessingException if deletion fails
     */
    override fun deleteImage(id: UUID) {
        // Get metadata first for the event emission
        val metadata = getImageMetadata(id)
        
        try {
            imageStorageService.delete(id)
            imageMetadataRepository.delete(id)
            logger.info("Successfully deleted image with id: {}", id)
            
            // Emit delete event
            imageEventEmitter.emitDelete(metadata)
            
        } catch (e: Exception) {
            logger.error("Failed to delete image with id: {}", id, e)
            throw ImageProcessingException("Failed to delete image", e)
        }
    }

    /**
     * Lists all stored images in insertion order.
     *
     * @return List of all image metadata
     */
    override fun getAllImages(): List<ImageMetadata> {
        return imageMetadataRepository.findAll()
    }

    /**
     * Validates an image upload request.
     * Checks file presence, MIME type, and size constraints.
     *
     * @param request The upload request to validate
     * @throws InvalidImageException if validation fails
     */
    private fun validateUploadRequest(request: ImageUploadRequest) {
        when {
            request.file.isEmpty -> {
                throw InvalidImageException("Image file cannot be empty")
            }
            !allowedMimeTypes.contains(request.file.contentType) -> {
                throw InvalidImageException("Unsupported image type: ${request.file.contentType}")
            }
            request.file.size > MAX_FILE_SIZE -> {
                throw InvalidImageException("Image size exceeds maximum allowed size of ${MAX_FILE_SIZE / (1024 * 1024)}MB")
            }
        }
    }

    companion object {
        /** Maximum allowed file size (10MB) */
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB
    }
} 