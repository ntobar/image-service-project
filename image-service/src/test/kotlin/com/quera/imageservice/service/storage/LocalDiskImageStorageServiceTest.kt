package com.quera.imageservice.service.storage

import com.quera.imageservice.exception.StorageException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockMultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [LocalDiskImageStorageService].
 * Tests file storage operations and error handling.
 */
class LocalDiskImageStorageServiceTest {

    private lateinit var storageService: LocalDiskImageStorageService
    private val testId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        storageService = LocalDiskImageStorageService(tempDir.toString())
        storageService.init()
    }

    /**
     * Tests successful file storage.
     */
    @Test
    fun `store should save file to disk`() {
        // Given
        val content = "test image data".toByteArray()
        val file = MockMultipartFile(
            "test.jpg",
            "test.jpg",
            "image/jpeg",
            content
        )

        // When
        val storedPath = storageService.store(testId, file)

        // Then
        assertTrue(Files.exists(storedPath))
        assertContentEquals(content, Files.readAllBytes(storedPath))
    }

    /**
     * Tests IOException handling during file storage.
     */
    @Test
    fun `store should handle IOException during file save`() {
        // Given
        val readOnlyDir = tempDir.resolve("readonly")
        Files.createDirectory(readOnlyDir)
        readOnlyDir.toFile().setReadOnly()
        
        val storageService = LocalDiskImageStorageService(readOnlyDir.toString())
        storageService.init()

        val file = MockMultipartFile(
            "test.jpg",
            "test.jpg",
            "image/jpeg",
            "test data".toByteArray()
        )

        // When/Then
        assertThrows<StorageException> {
            storageService.store(testId, file)
        }
    }

    /**
     * Tests successful file retrieval.
     */
    @Test
    fun `retrieve should load existing file`() {
        // Given
        val content = "test image data".toByteArray()
        Files.write(tempDir.resolve(testId.toString()), content)

        // When
        val retrieved = storageService.retrieve(testId)

        // Then
        assertContentEquals(content, retrieved)
    }

    /**
     * Tests file retrieval for non-existent file.
     */
    @Test
    fun `retrieve should throw exception for missing file`() {
        // When/Then
        assertThrows<StorageException> {
            storageService.retrieve(testId)
        }
    }

    /**
     * Tests successful file deletion.
     */
    @Test
    fun `delete should remove existing file`() {
        // Given
        val filePath = tempDir.resolve(testId.toString())
        Files.write(filePath, "test data".toByteArray())

        // When
        storageService.delete(testId)

        // Then
        assertTrue(!Files.exists(filePath))
    }

    /**
     * Tests IOException handling during file deletion.
     */
    @Test
    fun `delete should handle IOException during file deletion`() {
        // Given
        val readOnlyDir = tempDir.resolve("readonly")
        Files.createDirectory(readOnlyDir)
        val filePath = readOnlyDir.resolve(testId.toString())
        Files.write(filePath, "test data".toByteArray())
        readOnlyDir.toFile().setReadOnly()
        
        val storageService = LocalDiskImageStorageService(readOnlyDir.toString())
        storageService.init()

        // When/Then
        assertThrows<StorageException> {
            storageService.delete(testId)
        }
    }
} 