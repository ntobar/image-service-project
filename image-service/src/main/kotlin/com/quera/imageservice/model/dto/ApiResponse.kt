package com.quera.imageservice.model.dto

/**
 * Generic wrapper for API responses to ensure consistent response format.
 */
data class ApiResponse<T>(
    val data: T? = null,
    val error: String? = null
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(data = data)
        fun <T> error(message: String): ApiResponse<T> = ApiResponse(error = message)
    }
}

/**
 * DTO for image metadata responses to avoid exposing internal path information.
 */
data class ImageMetadataResponse(
    val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val uploadedAt: String
) 