package com.quera.imageservice.repository

import com.quera.imageservice.model.ImageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import java.time.Instant
import java.util.UUID
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [ImageMetadataRepository].
 * Verifies thread-safe storage and retrieval of image metadata.
 */
class ImageMetadataRepositoryTest {

    private lateinit var repository: ImageMetadataRepository
    private val testId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")

    @BeforeEach
    fun setup() {
        repository = ImageMetadataRepository()
    }

    @Nested
    inner class SaveAndRetrieve {
        /**
         * Verifies basic save and retrieve operations with field matching.
         */
        @Test
        fun `save metadata and retrieve by ID`() {
            // Given
            val metadata = createTestMetadata(testId)

            // When
            repository.save(metadata)
            val retrieved = repository.findById(testId)

            // Then
            assertEquals(metadata, retrieved)
            assertEquals(1, repository.count())
        }

        /**
         * Verifies that saving with same ID overwrites previous entry.
         */
        @Test
        fun `save with existing ID overwrites previous entry`() {
            // Given
            val original = createTestMetadata(testId, name = "original.jpg")
            val updated = createTestMetadata(testId, name = "updated.jpg")

            // When
            repository.save(original)
            repository.save(updated)
            val retrieved = repository.findById(testId)

            // Then
            assertEquals(updated, retrieved)
            assertEquals(1, repository.count())
            assertEquals("updated.jpg", retrieved?.name)
        }
    }

    @Nested
    inner class ListOperations {
        /**
         * Verifies that findAll returns metadata in correct insertion order.
         */
        @Test
        fun `findAll returns metadata in insertion order`() {
            // Given
            val first = createTestMetadata(UUID.randomUUID(), name = "first.jpg")
            val second = createTestMetadata(UUID.randomUUID(), name = "second.jpg")
            val third = createTestMetadata(UUID.randomUUID(), name = "third.jpg")

            // When
            repository.save(first)
            repository.save(second)
            repository.save(third)
            val all = repository.findAll()

            // Then
            assertEquals(3, all.size)
            assertEquals(listOf(first, second, third), all)
        }
    }

    @Nested
    inner class DeleteOperations {
        /**
         * Verifies successful deletion of existing metadata.
         */
        @Test
        fun `delete existing metadata`() {
            // Given
            val metadata = createTestMetadata(testId)
            repository.save(metadata)

            // When
            repository.delete(testId)

            // Then
            assertNull(repository.findById(testId))
            assertEquals(0, repository.count())
            assertTrue(repository.findAll().isEmpty())
        }

        /**
         * Verifies that deleting non-existent metadata is handled gracefully.
         */
        @Test
        fun `delete non-existent metadata causes no error`() {
            // When/Then - should not throw
            repository.delete(testId)
            assertEquals(0, repository.count())
        }
    }

    @Nested
    inner class ConcurrencyTests {
        /**
         * Stress tests concurrent access to the repository.
         * Simulates real-world parallel operations by:
         * - Inserting 1000 entries concurrently
         * - Deleting ~20% of entries concurrently
         * - Verifying repository consistency
         */
        @Test
        fun `concurrent operations should maintain repository consistency`() = runBlocking {
            // Given
            val totalEntries = 1000
            val deletionPercentage = 0.2
            val trackedMetadata = ConcurrentHashMap<UUID, ImageMetadata>()

            // When - Parallel inserts
            val insertJobs = (1..totalEntries).map { index ->
                async(Dispatchers.Default) {
                    val metadata = createTestMetadata(
                        id = UUID.randomUUID(),
                        name = "test-$index.jpg"
                    )
                    repository.save(metadata)
                    trackedMetadata[metadata.id] = metadata
                    metadata
                }
            }
            val savedMetadata = insertJobs.awaitAll()

            // Randomly select ~20% of entries for deletion
            val entriesToDelete = savedMetadata
                .shuffled()
                .take((totalEntries * deletionPercentage).toInt())

            // Parallel deletes
            val deleteJobs = entriesToDelete.map { metadata ->
                async(Dispatchers.Default) {
                    repository.delete(metadata.id)
                    trackedMetadata.remove(metadata.id)
                }
            }
            deleteJobs.awaitAll()

            // Then
            // 1. Verify repository size matches our tracking
            assertEquals(trackedMetadata.size, repository.count())

            // 2. Verify all remaining entries are retrievable and match
            trackedMetadata.forEach { (id, expectedMetadata) ->
                val actualMetadata = repository.findById(id)
                assertEquals(expectedMetadata, actualMetadata)
            }

            // 3. Verify deleted entries are actually gone
            entriesToDelete.forEach { metadata ->
                assertNull(repository.findById(metadata.id))
            }

            // 4. Verify findAll returns all remaining entries in some order
            val allMetadata = repository.findAll()
            assertEquals(trackedMetadata.size, allMetadata.size)
            assertTrue(allMetadata.all { metadata -> trackedMetadata.containsKey(metadata.id) })
        }
    }

    private fun createTestMetadata(
        id: UUID,
        name: String = "test.jpg",
        size: Long = 1024L,
        mimeType: String = "image/jpeg",
        path: String = "uploads/$id"
    ) = ImageMetadata(
        id = id,
        name = name,
        size = size,
        mimeType = mimeType,
        uploadedAt = Instant.parse("2024-03-14T12:00:00Z"),
        path = path
    )
} 