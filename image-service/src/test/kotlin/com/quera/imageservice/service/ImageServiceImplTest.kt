package com.quera.imageservice.service

import com.quera.imageservice.exception.ImageNotFoundException
import com.quera.imageservice.exception.ImageProcessingException
import com.quera.imageservice.exception.InvalidImageException
import com.quera.imageservice.exception.StorageException
import com.quera.imageservice.model.ImageMetadata
import com.quera.imageservice.model.dto.ImageUploadRequest
import com.quera.imageservice.repository.ImageMetadataRepository
import com.quera.imageservice.service.storage.ImageStorageService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import java.io.IOException
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

/**
 * Unit tests for [ImageServiceImpl].
 * Tests image operations and error handling.
 */
@ExtendWith(MockitoExtension::class)
class ImageServiceImplTest {

    @Mock
    private lateinit var storageService: ImageStorageService

    @Mock
    private lateinit var metadataRepository: ImageMetadataRepository

    @Mock
    private lateinit var eventEmitter: ImageEventEmitter

    private lateinit var imageService: ImageServiceImpl

    private val testId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val testPath = Path.of("uploads/$testId")

    @BeforeEach
    fun setup() {
        imageService = ImageServiceImpl(storageService, metadataRepository, eventEmitter)
    }

    /**
     * Tests successful image upload with valid data.
     */
    @Test
    fun `uploadImage should store file and metadata for valid image`() {
        // Given
        val file = MockMultipartFile(
            "test.jpg",
            "test.jpg",
            "image/jpeg",
            "test data".toByteArray()
        )
        val request = ImageUploadRequest(file)
        val metadata = ImageMetadata(
            id = testId,
            name = "test.jpg",
            size = file.size,
            mimeType = "image/jpeg",
            path = testPath.toString()
        )

        whenever(storageService.store(any(), any())).thenReturn(testPath)
        whenever(metadataRepository.save(any())).thenReturn(metadata)

        // When
        val result = imageService.uploadImage(request)

        // Then
        assertEquals(metadata, result)
        verify(eventEmitter).emitUpload(metadata)
    }

    /**
     * Tests error handling when storage service fails during upload.
     */
    @Test
    fun `uploadImage should handle storage service failure`() {
        // Given
        val file = MockMultipartFile(
            "test.jpg",
            "test.jpg",
            "image/jpeg",
            "test data".toByteArray()
        )
        val request = ImageUploadRequest(file)

        whenever(storageService.store(any(), any())).thenThrow(
            StorageException("Failed to store file", IOException("Disk full"))
        )

        // When/Then
        assertThrows<ImageProcessingException> {
            imageService.uploadImage(request)
        }

        // Verify no metadata was saved or events emitted
        verify(metadataRepository, never()).save(any())
        verify(eventEmitter, never()).emitUpload(any())
    }

    /**
     * Tests rejection of invalid MIME type.
     */
    @Test
    fun `uploadImage should reject invalid mime type`() {
        // Given
        val file = MockMultipartFile(
            "test.txt",
            "test.txt",
            "text/plain",
            "test data".toByteArray()
        )
        val request = ImageUploadRequest(file)

        // When/Then
        assertThrows<InvalidImageException> {
            imageService.uploadImage(request)
        }
    }

    /**
     * Tests rejection of oversized files.
     */
    @Test
    fun `uploadImage should reject oversized file`() {
        // Given
        val file = MockMultipartFile(
            "large.jpg",
            "large.jpg",
            "image/jpeg",
            ByteArray(11 * 1024 * 1024) // 11MB
        )
        val request = ImageUploadRequest(file)

        // When/Then
        assertThrows<InvalidImageException> {
            imageService.uploadImage(request)
        }
    }

    /**
     * Tests successful image download.
     */
    @Test
    fun `getImageFile should return file data for existing image`() {
        // Given
        val imageData = "test data".toByteArray()
        whenever(metadataRepository.findById(testId)).thenReturn(createTestMetadata())
        whenever(storageService.retrieve(testId)).thenReturn(imageData)

        // When
        val result = imageService.getImageFile(testId)

        // Then
        assertEquals(imageData, result)
    }

    /**
     * Tests image download for non-existent image.
     */
    @Test
    fun `getImageFile should throw exception for non-existent image`() {
        // Given
        whenever(metadataRepository.findById(testId)).thenReturn(null)

        // When/Then
        assertThrows<ImageNotFoundException> {
            imageService.getImageFile(testId)
        }
    }

    /**
     * Tests successful metadata retrieval.
     */
    @Test
    fun `getImageMetadata should return metadata for existing image`() {
        // Given
        val metadata = createTestMetadata()
        whenever(metadataRepository.findById(testId)).thenReturn(metadata)

        // When
        val result = imageService.getImageMetadata(testId)

        // Then
        assertEquals(metadata, result)
    }

    /**
     * Tests metadata retrieval for non-existent image.
     */
    @Test
    fun `getImageMetadata should throw exception for non-existent image`() {
        // Given
        whenever(metadataRepository.findById(testId)).thenReturn(null)

        // When/Then
        assertThrows<ImageNotFoundException> {
            imageService.getImageMetadata(testId)
        }
    }

    /**
     * Tests successful image deletion.
     */
    @Test
    fun `deleteImage should remove file and metadata for existing image`() {
        // Given
        val metadata = createTestMetadata()
        whenever(metadataRepository.findById(testId)).thenReturn(metadata)

        // When
        imageService.deleteImage(testId)

        // Then
        verify(storageService).delete(testId)
        verify(metadataRepository).delete(testId)
        verify(eventEmitter).emitDelete(metadata)
    }

    /**
     * Tests image deletion for non-existent image.
     */
    @Test
    fun `deleteImage should throw exception for non-existent image`() {
        // Given
        whenever(metadataRepository.findById(testId)).thenReturn(null)

        // When/Then
        assertThrows<ImageNotFoundException> {
            imageService.deleteImage(testId)
        }
    }

    private fun createTestMetadata() = ImageMetadata(
        id = testId,
        name = "test.jpg",
        size = 100L,
        mimeType = "image/jpeg",
        uploadedAt = Instant.now(),
        path = testPath.toString()
    )
} 