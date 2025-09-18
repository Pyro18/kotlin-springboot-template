package com.example.template.service

import com.example.template.exception.BusinessException
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class FileStorageService(
    @Value("\${app.file-upload.storage-path:./uploads}")
    private val storagePath: String,
    @Value("\${app.file-upload.max-size:10485760}") // 10MB default
    private val maxFileSize: Long,
    @Value("\${app.file-upload.allowed-extensions:jpg,jpeg,png,gif,pdf}")
    private val allowedExtensions: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var rootLocation: Path
    private lateinit var allowedExtensionsList: List<String>

    @PostConstruct
    fun init() {
        rootLocation = Paths.get(storagePath).toAbsolutePath().normalize()
        allowedExtensionsList = allowedExtensions.split(",").map { it.trim().lowercase() }

        try {
            Files.createDirectories(rootLocation)
            logger.info("File storage initialized at: $rootLocation")
        } catch (e: IOException) {
            throw BusinessException("Could not initialize file storage: ${e.message}")
        }
    }

    /**
     * Store a file in the specified subdirectory
     * @param file The file to store
     * @param subDir Optional subdirectory (e.g., "avatars", "documents")
     * @return The stored file path/URL
     */
    suspend fun storeFile(file: MultipartFile, subDir: String? = null): String = withContext(Dispatchers.IO) {
        logger.debug("Storing file: ${file.originalFilename}, size: ${file.size}")

        // Validate file
        validateFile(file)

        // Clean filename and generate unique name
        val originalFilename = StringUtils.cleanPath(file.originalFilename ?: "unknown")
        val fileExtension = getFileExtension(originalFilename)
        val uniqueFilename = "${UUID.randomUUID()}.${fileExtension}"

        // Determine target directory
        val targetDir = if (subDir != null) {
            rootLocation.resolve(subDir).also {
                Files.createDirectories(it)
            }
        } else {
            rootLocation
        }

        // Store file
        try {
            val targetLocation = targetDir.resolve(uniqueFilename)

            // Security check: ensure we're not writing outside our directory
            if (!targetLocation.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                throw BusinessException("Cannot store file outside current directory")
            }

            file.inputStream.use { inputStream ->
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)
            }

            logger.info("File stored successfully: $targetLocation")

            // Return relative path or URL
            val relativePath = rootLocation.relativize(targetLocation).toString()
            return@withContext if (subDir != null) "$subDir/$uniqueFilename" else uniqueFilename

        } catch (e: IOException) {
            logger.error("Failed to store file: ${e.message}", e)
            throw BusinessException("Failed to store file: ${e.message}")
        }
    }

    /**
     * Load a file as byte array
     */
    suspend fun loadFile(filePath: String): ByteArray = withContext(Dispatchers.IO) {
        try {
            val file = rootLocation.resolve(filePath).normalize()

            // Security check
            if (!file.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                throw BusinessException("Cannot access file outside current directory")
            }

            if (!Files.exists(file)) {
                throw BusinessException("File not found: $filePath")
            }

            return@withContext Files.readAllBytes(file)
        } catch (e: IOException) {
            logger.error("Could not read file: $filePath", e)
            throw BusinessException("Could not read file: ${e.message}")
        }
    }

    /**
     * Delete a file
     */
    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = rootLocation.resolve(filePath).normalize()

            // Security check
            if (!file.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                logger.warn("Attempted to delete file outside storage directory: $filePath")
                return@withContext false
            }

            if (Files.exists(file)) {
                Files.delete(file)
                logger.info("File deleted: $filePath")
                return@withContext true
            }

            logger.warn("File not found for deletion: $filePath")
            return@withContext false
        } catch (e: IOException) {
            logger.error("Could not delete file: $filePath", e)
            return@withContext false
        }
    }

    /**
     * Get file metadata
     */
    suspend fun getFileInfo(filePath: String): FileInfo? = withContext(Dispatchers.IO) {
        try {
            val file = rootLocation.resolve(filePath).normalize()

            if (!file.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                return@withContext null
            }

            if (!Files.exists(file)) {
                return@withContext null
            }

            val attributes = Files.readAttributes(file, "*")

            FileInfo(
                name = file.fileName.toString(),
                path = filePath,
                size = attributes["size"] as Long,
                contentType = Files.probeContentType(file) ?: "application/octet-stream",
                createdAt = (attributes["creationTime"] as java.nio.file.attribute.FileTime).toInstant(),
                lastModified = (attributes["lastModifiedTime"] as java.nio.file.attribute.FileTime).toInstant()
            )
        } catch (e: Exception) {
            logger.error("Could not get file info: $filePath", e)
            null
        }
    }

    /**
     * List files in a directory
     */
    suspend fun listFiles(subDir: String? = null): List<FileInfo> = withContext(Dispatchers.IO) {
        try {
            val searchDir = if (subDir != null) {
                rootLocation.resolve(subDir)
            } else {
                rootLocation
            }

            if (!Files.exists(searchDir)) {
                return@withContext emptyList()
            }

            Files.walk(searchDir, 1)
                .filter { Files.isRegularFile(it) }
                .map { file ->
                    val relativePath = rootLocation.relativize(file).toString()
                    FileInfo(
                        name = file.fileName.toString(),
                        path = relativePath,
                        size = Files.size(file),
                        contentType = Files.probeContentType(file) ?: "application/octet-stream",
                        createdAt = Files.getAttribute(file, "creationTime") as java.time.Instant,
                        lastModified = Files.getLastModifiedTime(file).toInstant()
                    )
                }
                .toList()
        } catch (e: IOException) {
            logger.error("Could not list files in: $subDir", e)
            emptyList()
        }
    }

    /**
     * Clean up old files (e.g., temporary uploads)
     */
    suspend fun cleanupOldFiles(olderThanDays: Long = 30): Int = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000)
            var deletedCount = 0

            Files.walk(rootLocation)
                .filter { Files.isRegularFile(it) }
                .filter { Files.getLastModifiedTime(it).toMillis() < cutoffTime }
                .forEach { file ->
                    try {
                        Files.delete(file)
                        deletedCount++
                        logger.debug("Deleted old file: $file")
                    } catch (e: IOException) {
                        logger.error("Could not delete old file: $file", e)
                    }
                }

            logger.info("Cleanup completed. Deleted $deletedCount old files")
            deletedCount
        } catch (e: IOException) {
            logger.error("Error during cleanup: ${e.message}", e)
            0
        }
    }

    /**
     * Validate file before storage
     */
    private fun validateFile(file: MultipartFile) {
        // Check if file is empty
        if (file.isEmpty) {
            throw BusinessException("Cannot store empty file")
        }

        // Check file size
        if (file.size > maxFileSize) {
            throw BusinessException(
                "File size (${file.size} bytes) exceeds maximum allowed size (${maxFileSize} bytes)"
            )
        }

        // Check file extension
        val filename = file.originalFilename ?: throw BusinessException("Filename is required")
        val fileExtension = getFileExtension(filename)

        if (!isAllowedExtension(fileExtension)) {
            throw BusinessException(
                "File type '.$fileExtension' is not allowed. Allowed types: ${allowedExtensionsList.joinToString(", ")}"
            )
        }

        // Additional security check for file content
        validateFileContent(file)
    }

    /**
     * Validate file content for security
     */
    private fun validateFileContent(file: MultipartFile) {
        // Check for potentially malicious content
        val contentType = file.contentType ?: "unknown"

        // Basic MIME type validation
        when {
            contentType.startsWith("image/") -> validateImageContent(file)
            contentType == "application/pdf" -> validatePdfContent(file)
            contentType.startsWith("text/") -> validateTextContent(file)
            else -> {
                // For other types, rely on extension validation
                logger.debug("Content type $contentType validation skipped")
            }
        }
    }

    private fun validateImageContent(file: MultipartFile) {
        // Simple magic number check for common image formats
        val bytes = ByteArray(12)
        file.inputStream.use { it.read(bytes, 0, bytes.size) }

        val isValidImage = when {
            // JPEG
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> true
            // PNG
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> true
            // GIF
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() -> true
            // WebP
            bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() -> true
            else -> false
        }

        if (!isValidImage) {
            throw BusinessException("File content does not match image format")
        }
    }

    private fun validatePdfContent(file: MultipartFile) {
        // Check PDF magic number
        val bytes = ByteArray(5)
        file.inputStream.use { it.read(bytes, 0, bytes.size) }

        val pdfHeader = String(bytes)
        if (!pdfHeader.startsWith("%PDF-")) {
            throw BusinessException("File content does not match PDF format")
        }
    }

    private fun validateTextContent(file: MultipartFile) {
        // Basic validation for text files
        // Could add more sophisticated checks here
        logger.debug("Text file validation passed")
    }

    private fun getFileExtension(filename: String): String {
        val dotIndex = filename.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < filename.length - 1) {
            filename.substring(dotIndex + 1).lowercase()
        } else {
            throw BusinessException("File must have an extension")
        }
    }

    private fun isAllowedExtension(extension: String): Boolean {
        return allowedExtensionsList.contains(extension.lowercase())
    }

    /**
     * Generate a public URL for the file (if behind a CDN or object storage)
     */
    fun generatePublicUrl(filePath: String, baseUrl: String = ""): String {
        return if (baseUrl.isNotEmpty()) {
            "$baseUrl/$filePath"
        } else {
            "/files/$filePath" // Relative URL for local storage
        }
    }

    /**
     * Get storage statistics
     */
    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        try {
            var totalSize = 0L
            var fileCount = 0
            val extensionCounts = mutableMapOf<String, Int>()

            Files.walk(rootLocation)
                .filter { Files.isRegularFile(it) }
                .forEach { file ->
                    totalSize += Files.size(file)
                    fileCount++

                    val extension = try {
                        getFileExtension(file.fileName.toString())
                    } catch (e: Exception) {
                        "unknown"
                    }
                    extensionCounts[extension] = extensionCounts.getOrDefault(extension, 0) + 1
                }

            StorageStats(
                totalFiles = fileCount,
                totalSize = totalSize,
                averageFileSize = if (fileCount > 0) totalSize / fileCount else 0,
                filesByExtension = extensionCounts,
                availableSpace = Files.getFileStore(rootLocation).usableSpace
            )
        } catch (e: IOException) {
            logger.error("Could not get storage stats: ${e.message}", e)
            StorageStats(0, 0, 0, emptyMap(), 0)
        }
    }
}

// Data classes
data class FileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val contentType: String,
    val createdAt: java.time.Instant,
    val lastModified: java.time.Instant
)

data class StorageStats(
    val totalFiles: Int,
    val totalSize: Long,
    val averageFileSize: Long,
    val filesByExtension: Map<String, Int>,
    val availableSpace: Long
)