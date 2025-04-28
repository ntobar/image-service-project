package com.quera.imageservice.exception

/**
 * Custom exception for storage-related errors.
 */
class StorageException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
} 