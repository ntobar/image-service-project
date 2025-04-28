package com.quera.imageservice.service.storage

import com.quera.imageservice.exception.StorageException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID
import jakarta.annotation.PostConstruct

/**
 * Implementation of [ImageStorageService] that stores images on the local filesystem.
 * Provides basic file operations for storing, retrieving, and deleting images.
 *
 * @property storageLocation Base directory for storing images, configurable via application properties
 */
@Service
class LocalDiskImageStorageService(
    @Value("\${app.storage.location:uploads}") private val storageLocation: String
) : ImageStorageService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var rootLocation: Path

    /**
     * Initializes the storage system by creating the root storage directory.
     * Called automatically after bean construction.
     * @throws StorageException if directory creation fails
     */
    @PostConstruct
    override fun init() {
        rootLocation = Paths.get(storageLocation)
        try {
            Files.createDirectories(rootLocation)
            logger.info("Initialized storage directory at: {}", rootLocation.toAbsolutePath())
        } catch (e: Exception) {
            throw StorageException("Could not initialize storage", e)
        }
    }

    /**
     * Stores an image file on disk using its UUID as the filename.
     * @param imageId Unique identifier for the image
     * @param file The multipart file to store
     * @return Path where the file was stored
     * @throws StorageException if the file is empty or cannot be stored
     */
    override fun store(imageId: UUID, file: MultipartFile): Path {
        try {
            if (file.isEmpty) {
                throw StorageException("Failed to store empty file")
            }

            val destinationPath = rootLocation.resolve(imageId.toString())
            Files.copy(file.inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING)
            logger.info("Stored file {} at {}", file.originalFilename, destinationPath)
            return destinationPath
        } catch (e: Exception) {
            throw StorageException("Failed to store file", e)
        }
    }

    /**
     * Retrieves an image file from disk.
     * @param imageId Unique identifier of the image to retrieve
     * @return The image file contents as a byte array
     * @throws StorageException if the file cannot be read or doesn't exist
     */
    override fun retrieve(imageId: UUID): ByteArray {
        try {
            val path = rootLocation.resolve(imageId.toString())
            return Files.readAllBytes(path)
        } catch (e: Exception) {
            throw StorageException("Failed to read stored file", e)
        }
    }

    /**
     * Deletes an image file from disk.
     * @param imageId Unique identifier of the image to delete
     * @throws StorageException if the file cannot be deleted
     */
    override fun delete(imageId: UUID) {
        try {
            val path = rootLocation.resolve(imageId.toString())
            Files.deleteIfExists(path)
            logger.info("Deleted file at {}", path)
        } catch (e: Exception) {
            throw StorageException("Failed to delete file", e)
        }
    }
} 
