package com.quera.imageservice.exception

/**
 * Custom exception for image service related errors.
 */
sealed class ImageServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Thrown when an image is not found.
 */
class ImageNotFoundException(id: String) : ImageServiceException("Image not found with id: $id")

/**
 * Thrown when an upload request is invalid.
 */
class InvalidImageException(message: String) : ImageServiceException(message)

/**
 * Thrown when there's an error processing the image.
 */
class ImageProcessingException(message: String, cause: Throwable? = null) : 
    ImageServiceException(message, cause) 