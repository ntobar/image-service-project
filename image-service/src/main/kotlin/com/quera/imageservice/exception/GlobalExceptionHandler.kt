package com.quera.imageservice.exception

import com.quera.imageservice.model.dto.ApiResponse
import org.apache.catalina.connector.ClientAbortException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ImageNotFoundException::class)
    fun handleImageNotFound(ex: ImageNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.message ?: "Image not found"))
    }

    @ExceptionHandler(InvalidImageException::class)
    fun handleInvalidImage(ex: InvalidImageException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.message ?: "Invalid image"))
    }

    @ExceptionHandler(ImageProcessingException::class)
    fun handleImageProcessing(ex: ImageProcessingException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Image processing error", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Failed to process image"))
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceeded(ex: MaxUploadSizeExceededException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("File size exceeds maximum allowed size"))
    }

    @ExceptionHandler(ClientAbortException::class)
    fun handleClientAbort(ex: ClientAbortException) {
        // Just log it at debug level since this is a client-side abort
        logger.debug("Client aborted the request", ex)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericError(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected error", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("An unexpected error occurred"))
    }
} 
