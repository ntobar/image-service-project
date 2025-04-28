package com.quera.imageservice.model.dto

import org.springframework.web.multipart.MultipartFile

/**
 * DTO for handling image upload requests.
 * Uses Spring's MultipartFile for handling the actual file upload.
 */
data class ImageUploadRequest(
    val file: MultipartFile,
    val name: String? = null // Optional custom name, will default to original filename if not provided
) 