package com.quera.imageservice.controller

import com.quera.imageservice.model.ImageMetadata
import org.springframework.mock.web.MockMultipartFile
import java.time.Instant
import java.util.UUID

/**
 * Test fixtures for controller tests.
 * Provides common test data and helper methods.
 */
object TestFixtures {
    val TEST_IMAGE_ID: UUID = UUID.fromString("1cfea927-4e0b-41bd-8d2e-9d734546fa78")
    const val TEST_IMAGE_NAME = "test-image.jpg"
    const val TEST_IMAGE_SIZE = 1024L
    const val TEST_IMAGE_TYPE = "image/jpeg"
    
    fun createTestImageMetadata(
        id: UUID = TEST_IMAGE_ID,
        name: String = TEST_IMAGE_NAME,
        size: Long = TEST_IMAGE_SIZE,
        mimeType: String = TEST_IMAGE_TYPE,
        uploadedAt: Instant = Instant.parse("2024-04-26T02:28:52.330727Z"),
        path: String = "uploads/$id"
    ) = ImageMetadata(
        id = id,
        name = name,
        size = size,
        mimeType = mimeType,
        uploadedAt = uploadedAt,
        path = path
    )

    fun createTestMultipartFile(
        name: String = TEST_IMAGE_NAME,
        contentType: String = TEST_IMAGE_TYPE,
        content: ByteArray = ByteArray(TEST_IMAGE_SIZE.toInt()) { 1 }
    ) = MockMultipartFile(
        "file",
        name,
        contentType,
        content
    )
} 