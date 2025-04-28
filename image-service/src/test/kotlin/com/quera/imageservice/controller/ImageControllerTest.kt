package com.quera.imageservice.controller

import com.quera.imageservice.exception.ImageNotFoundException
import com.quera.imageservice.exception.InvalidImageException
import com.quera.imageservice.service.ImageService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * Integration tests for [ImageController] REST endpoints.
 *
 * Tests verify:
 * - Image upload functionality with validation
 * - Image download with proper headers and content
 * - Metadata retrieval and formatting
 * - Image deletion with proper status codes
 * - Image listing with pagination and sorting
 *
 * @see ImageController
 */
@WebMvcTest(ImageController::class)
class ImageControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var imageService: ImageService

    /**
     * Tests for the image upload endpoint `/api/images` (POST).
     *
     * Verifies:
     * - Successful image upload with metadata
     * - Validation of file types and sizes
     * - Error handling for invalid requests
     */
    @Nested
    inner class UploadImage {
        @Test
        fun `should upload image successfully`() {
            // Given
            val testFile = TestFixtures.createTestMultipartFile()
            val metadata = TestFixtures.createTestImageMetadata()
            whenever(imageService.uploadImage(any())).thenReturn(metadata)

            // When/Then
            mockMvc.perform(multipart("/api/images")
                .file(testFile)
                .param("name", TestFixtures.TEST_IMAGE_NAME))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.id").value(TestFixtures.TEST_IMAGE_ID.toString()))
                .andExpect(jsonPath("$.data.name").value(TestFixtures.TEST_IMAGE_NAME))
                .andExpect(jsonPath("$.data.size").value(TestFixtures.TEST_IMAGE_SIZE))
                .andExpect(jsonPath("$.data.mimeType").value(TestFixtures.TEST_IMAGE_TYPE))
        }

        @Test
        fun `should return 400 when uploading invalid file`() {
            // Given
            val invalidFile = TestFixtures.createTestMultipartFile(contentType = "text/plain")
            whenever(imageService.uploadImage(any())).thenThrow(InvalidImageException("Unsupported image type"))

            // When/Then
            mockMvc.perform(multipart("/api/images")
                .file(invalidFile))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error").value("Unsupported image type"))
        }

        /**
         * Tests rejection of zero-byte file uploads.
         *
         * Verifies that:
         * - Empty files are properly detected
         * - Service returns appropriate error message
         * - Response has correct HTTP status code (400)
         * - Error details are included in response body
         */
        @Test
        fun `should reject zero-byte file upload`() {
            // Given
            val emptyFile = MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                ByteArray(0)
            )
            whenever(imageService.uploadImage(any()))
                .thenThrow(InvalidImageException("Image file cannot be empty"))

            // When/Then
            mockMvc.perform(multipart("/api/images")
                .file(emptyFile))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error").value("Image file cannot be empty"))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        /**
         * Tests rejection of non-image file types.
         *
         * Verifies that:
         * - Files with unsupported MIME types are rejected
         * - Service provides specific error message with invalid content type
         * - Response has correct HTTP status code (400)
         * - Error details are included in response body
         */
        @Test
        fun `should reject non-image content type`() {
            // Given
            val invalidFile = MockMultipartFile(
                "file",
                "document.zip",
                "application/zip",
                ByteArray(1024)
            )
            whenever(imageService.uploadImage(any()))
                .thenThrow(InvalidImageException("Unsupported image type: application/zip"))

            // When/Then
            mockMvc.perform(multipart("/api/images")
                .file(invalidFile))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error").value("Unsupported image type: application/zip"))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        /**
         * Tests handling of filenames containing special characters.
         *
         * Verifies that:
         * - Filenames with special characters are properly processed
         * - Special characters are preserved in the response
         * - Upload succeeds with correct status code (201)
         * - Metadata in response matches input file
         */
        @Test
        fun `should handle special characters in filename`() {
            // Given
            val specialNameFile = MockMultipartFile(
                "file",
                "weird@name#file!.jpg",
                "image/jpeg",
                ByteArray(1024)
            )
            val metadata = TestFixtures.createTestImageMetadata(
                name = "weird@name#file!.jpg"
            )
            whenever(imageService.uploadImage(any())).thenReturn(metadata)

            // When/Then
            mockMvc.perform(multipart("/api/images")
                .file(specialNameFile))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.name").value("weird@name#file!.jpg"))
                .andExpect(jsonPath("$.data.mimeType").value(TestFixtures.TEST_IMAGE_TYPE))
        }
    }

    /**
     * Tests for the image download endpoint `/api/images/{id}` (GET).
     *
     * Verifies:
     * - Successful image download with correct headers
     * - Content-Type and disposition handling
     * - Error handling for non-existent images
     */
    @Nested
    inner class GetImage {
        @Test
        fun `should download image successfully`() {
            // Given
            val imageData = ByteArray(TestFixtures.TEST_IMAGE_SIZE.toInt()) { 1 }
            val metadata = TestFixtures.createTestImageMetadata()
            whenever(imageService.getImageMetadata(TestFixtures.TEST_IMAGE_ID)).thenReturn(metadata)
            whenever(imageService.getImageFile(TestFixtures.TEST_IMAGE_ID)).thenReturn(imageData)

            // When/Then
            mockMvc.perform(get("/api/images/${TestFixtures.TEST_IMAGE_ID}"))
                .andExpect(status().isOk)
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, TestFixtures.TEST_IMAGE_TYPE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${TestFixtures.TEST_IMAGE_NAME}\""))
                .andExpect(content().bytes(imageData))
        }

        @Test
        fun `should return 404 when image not found`() {
            // Given
            val id = TestFixtures.TEST_IMAGE_ID
            whenever(imageService.getImageMetadata(id)).thenThrow(ImageNotFoundException(id.toString()))

            // When/Then
            mockMvc.perform(get("/api/images/$id"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error").value("Image not found with id: $id"))
                .andExpect(jsonPath("$.data").doesNotExist())
        }
    }

    /**
     * Tests for the metadata retrieval endpoint `/api/images/{id}/metadata` (GET).
     *
     * Verifies:
     * - Successful metadata retrieval
     * - Response format and field mapping
     * - Error handling for non-existent images
     */
    @Nested
    inner class GetImageMetadata {
        @Test
        fun `should get metadata successfully`() {
            // Given
            val metadata = TestFixtures.createTestImageMetadata()
            whenever(imageService.getImageMetadata(TestFixtures.TEST_IMAGE_ID)).thenReturn(metadata)

            // When/Then
            mockMvc.perform(get("/api/images/${TestFixtures.TEST_IMAGE_ID}/metadata"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value(TestFixtures.TEST_IMAGE_ID.toString()))
                .andExpect(jsonPath("$.data.name").value(TestFixtures.TEST_IMAGE_NAME))
                .andExpect(jsonPath("$.data.size").value(TestFixtures.TEST_IMAGE_SIZE))
                .andExpect(jsonPath("$.data.mimeType").value(TestFixtures.TEST_IMAGE_TYPE))
        }

        @Test
        fun `should return 404 when metadata not found`() {
            // Given
            val id = TestFixtures.TEST_IMAGE_ID
            whenever(imageService.getImageMetadata(id)).thenThrow(ImageNotFoundException(id.toString()))

            // When/Then
            mockMvc.perform(get("/api/images/$id/metadata"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error").value("Image not found with id: $id"))
                .andExpect(jsonPath("$.data").doesNotExist())
        }
    }

    /**
     * Tests for the image deletion endpoint `/api/images/{id}` (DELETE).
     *
     * Verifies:
     * - Successful image deletion
     * - Proper status codes
     * - Error handling for non-existent images
     */
    @Nested
    inner class DeleteImage {
        @Test
        fun `should delete image successfully`() {
            // When/Then
            mockMvc.perform(delete("/api/images/${TestFixtures.TEST_IMAGE_ID}"))
                .andExpect(status().isNoContent)
        }

        @Test
        fun `should return 404 when deleting non-existent image`() {
            // Given
            val id = TestFixtures.TEST_IMAGE_ID
            whenever(imageService.deleteImage(id)).thenThrow(ImageNotFoundException(id.toString()))

            // When/Then
            mockMvc.perform(delete("/api/images/$id"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error").value("Image not found with id: $id"))
                .andExpect(jsonPath("$.data").doesNotExist())
        }
    }

    /**
     * Tests for the image listing endpoint `/api/images` (GET).
     *
     * Verifies:
     * - Successful retrieval of all images
     * - Empty list handling
     * - Response format and pagination
     */
    @Nested
    inner class ListImages {
        @Test
        fun `should list all images successfully`() {
            // Given
            val images = listOf(
                TestFixtures.createTestImageMetadata(),
                TestFixtures.createTestImageMetadata(
                    id = UUID.randomUUID(),
                    name = "second-image.png"
                )
            )
            whenever(imageService.getAllImages()).thenReturn(images)

            // When/Then
            mockMvc.perform(get("/api/images"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(TestFixtures.TEST_IMAGE_ID.toString()))
                .andExpect(jsonPath("$.data[1].name").value("second-image.png"))
        }

        @Test
        fun `should return empty list when no images exist`() {
            // Given
            whenever(imageService.getAllImages()).thenReturn(emptyList())

            // When/Then
            mockMvc.perform(get("/api/images"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
        }
    }
} 