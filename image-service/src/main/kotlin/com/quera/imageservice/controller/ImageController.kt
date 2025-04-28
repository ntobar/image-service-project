package com.quera.imageservice.controller

import com.quera.imageservice.exception.ImageNotFoundException
import com.quera.imageservice.exception.InvalidImageException
import com.quera.imageservice.model.dto.ApiResponse
import com.quera.imageservice.model.dto.ImageMetadataResponse
import com.quera.imageservice.model.dto.ImageUploadRequest
import com.quera.imageservice.service.ImageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.multipart.MultipartFile
import java.util.*

/**
 * REST controller for image management operations.
 * Handles upload, download, metadata, and deletion of images.
 */
@RestController
@RequestMapping("/api/images")
@Tag(name = "Images", description = "Image management endpoints")
class ImageController(private val imageService: ImageService) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Uploads a new image with optional custom name.
     *
     * @param file The image file to upload
     * @param name Optional custom name for the image
     * @return Response containing the stored image metadata
     * @throws InvalidImageException if the image is invalid or unsupported
     */
    @Operation(summary = "Upload a new image")
    @ApiResponses(value = [
        SwaggerResponse(responseCode = "201", description = "Image uploaded successfully"),
        SwaggerResponse(responseCode = "400", description = "Invalid input"),
        SwaggerResponse(responseCode = "500", description = "Server error")
    ])
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @Parameter(description = "Image file to upload")
        @RequestParam("file") file: MultipartFile,
        @Parameter(description = "Optional custom name for the image")
        @RequestParam("name", required = false) name: String?
    ): ResponseEntity<ApiResponse<ImageMetadataResponse>> {
        val request = ImageUploadRequest(file, name)
        val metadata = imageService.uploadImage(request)
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(metadata.toResponse()))
    }

    /**
     * Retrieves metadata for an image.
     *
     * @param id The ID of the image
     * @return Response containing the image metadata
     * @throws ImageNotFoundException if the image doesn't exist
     */
    @Operation(summary = "Get image metadata by ID")
    @ApiResponses(value = [
        SwaggerResponse(responseCode = "200", description = "Image metadata found"),
        SwaggerResponse(responseCode = "404", description = "Image not found")
    ])
    @GetMapping("/{id}/metadata")
    fun getImageMetadata(
        @Parameter(description = "Image ID")
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<ImageMetadataResponse>> {
        val metadata = imageService.getImageMetadata(id)
        logger.info("Retrieved metadata for image: id={}, name={}, size={}, type={}", 
            metadata.id, metadata.name, metadata.size, metadata.mimeType)
        return ResponseEntity.ok(ApiResponse.success(metadata.toResponse()))
    }

    /**
     * Downloads an image file.
     *
     * @param id The ID of the image to download
     * @param accept Optional Accept header to determine content disposition
     * @return Response containing the image data
     * @throws ImageNotFoundException if the image doesn't exist
     */
    @Operation(summary = "Download image file by ID")
    @ApiResponses(value = [
        SwaggerResponse(responseCode = "200", description = "Image file found"),
        SwaggerResponse(responseCode = "404", description = "Image not found")
    ])
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getImage(
        @Parameter(description = "Image ID")
        @PathVariable id: UUID,
        @RequestHeader(value = "Accept", required = false) accept: String?
    ): ResponseEntity<ByteArray> {
        val metadata = imageService.getImageMetadata(id)
        val imageData = imageService.getImageFile(id)
        
        // Determine if we should display inline or as attachment based on Accept header
        val disposition = if (accept?.startsWith("image/") == true) "inline" else "attachment"
        
        logger.info("Successfully retrieved image: id={}, name={}, size={}, type={}", 
            metadata.id, metadata.name, metadata.size, metadata.mimeType)
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(metadata.mimeType))
            .contentLength(imageData.size.toLong())
            .header(HttpHeaders.CONTENT_DISPOSITION, "$disposition; filename=\"${metadata.name}\"")
            .body(imageData)
    }

    /**
     * Deletes an image.
     *
     * @param id The ID of the image to delete
     * @throws ImageNotFoundException if the image doesn't exist
     */
    @Operation(summary = "Delete image by ID")
    @ApiResponses(value = [
        SwaggerResponse(responseCode = "204", description = "Image deleted successfully"),
        SwaggerResponse(responseCode = "404", description = "Image not found")
    ])
    @DeleteMapping("/{id}")
    fun deleteImage(
        @Parameter(description = "Image ID")
        @PathVariable id: UUID
    ): ResponseEntity<Unit> {
        imageService.deleteImage(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Lists metadata for all stored images.
     *
     * @return Response containing list of all image metadata
     */
    @Operation(summary = "List all images metadata")
    @ApiResponses(value = [
        SwaggerResponse(responseCode = "200", description = "List of all image metadata")
    ])
    @GetMapping
    fun getAllImages(): ResponseEntity<ApiResponse<List<ImageMetadataResponse>>> {
        val images = imageService.getAllImages()
        logger.info("Retrieved metadata for {} images", images.size)
        return ResponseEntity.ok(ApiResponse.success(images.map { it.toResponse() }))
    }

    /**
     * Converts internal metadata to response DTO.
     */
    private fun com.quera.imageservice.model.ImageMetadata.toResponse() = ImageMetadataResponse(
        id = id.toString(),
        name = name,
        size = size,
        mimeType = mimeType,
        uploadedAt = uploadedAt.toString()
    )
} 
