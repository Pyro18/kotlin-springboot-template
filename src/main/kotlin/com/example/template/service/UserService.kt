package com.example.template.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.template.dto.*
import com.example.template.entity.User
import com.example.template.entity.UserRole
import com.example.template.exception.ResourceNotFoundException
import com.example.template.exception.DuplicateResourceException
import com.example.template.exception.BusinessException
import com.example.template.mapper.UserMapper
import com.example.template.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
    private val passwordEncoder: PasswordEncoder,
    private val fileStorageService: FileStorageService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val CACHE_NAME = "users"
        const val CACHE_KEY = "#id"
    }

    @Transactional(readOnly = true)
    fun findAll(
        pageable: Pageable,
        active: Boolean? = null,
        role: String? = null,
        search: String? = null
    ): Page<UserResponseDto> {
        logger.debug("Finding users with filters - active: $active, role: $role, search: $search")

        val users = when {
            search != null && role != null && active != null ->
                userRepository.findByActiveAndRoleAndSearchTerm(active, UserRole.valueOf(role), search, pageable)
            search != null && active != null ->
                userRepository.findByActiveAndSearchTerm(active, search, pageable)
            search != null && role != null ->
                userRepository.findByRoleAndSearchTerm(UserRole.valueOf(role), search, pageable)
            role != null && active != null ->
                userRepository.findByActiveAndRole(active, UserRole.valueOf(role), pageable)
            search != null ->
                userRepository.findBySearchTerm(search, pageable)
            active != null ->
                userRepository.findByActive(active, pageable)
            role != null ->
                userRepository.findByRole(UserRole.valueOf(role), pageable)
            else ->
                userRepository.findAll(pageable)
        }

        return users.map(userMapper::toDto)
    }

    @Cacheable(value = [CACHE_NAME], key = CACHE_KEY)
    @Transactional(readOnly = true)
    fun findById(id: Long): UserResponseDto {
        logger.debug("Finding user by id: $id")
        val user = userRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("User", "id", id)
        return userMapper.toDto(user)
    }

    @Transactional(readOnly = true)
    fun findByUsername(username: String): UserResponseDto {
        logger.debug("Finding user by username: $username")
        val user = userRepository.findByUsername(username)
            ?: throw ResourceNotFoundException("User", "username", username)
        return userMapper.toDto(user)
    }

    @CacheEvict(value = [CACHE_NAME], allEntries = true)
    fun create(dto: CreateUserDto): UserResponseDto {
        logger.info("Creating new user with username: ${dto.username}")

        // Validate uniqueness
        validateUniqueFields(dto.username, dto.email)

        val user = User(
            firstName = dto.firstName,
            lastName = dto.lastName,
            username = dto.username,
            email = dto.email.lowercase(),
            password = passwordEncoder.encode(dto.password),
            bio = dto.bio,
            role = dto.role ?: UserRole.USER
        )

        val savedUser = userRepository.save(user)
        logger.info("User created successfully with id: ${savedUser.id}")

        return userMapper.toDto(savedUser)
    }

    @CachePut(value = [CACHE_NAME], key = CACHE_KEY)
    fun update(id: Long, dto: UpdateUserDto): UserResponseDto {
        logger.info("Updating user with id: $id")

        val user = userRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("User", "id", id)

        // Validate uniqueness if fields are being changed
        if (dto.username != null && dto.username != user.username) {
            validateUsernameUnique(dto.username)
            user.username = dto.username
        }

        if (dto.email != null && dto.email != user.email) {
            validateEmailUnique(dto.email)
            user.email = dto.email.lowercase()
        }

        dto.firstName?.let { user.firstName = it }
        dto.lastName?.let { user.lastName = it }
        dto.bio?.let { user.bio = it }
        dto.role?.let { user.role = it }

        val updatedUser = userRepository.save(user)
        logger.info("User updated successfully: $id")

        return userMapper.toDto(updatedUser)
    }

    @CachePut(value = [CACHE_NAME], key = CACHE_KEY)
    fun partialUpdate(id: Long, updates: Map<String, Any>): UserResponseDto {
        logger.info("Partially updating user with id: $id")

        val user = userRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("User", "id", id)

        updates.forEach { (key, value) ->
            when (key) {
                "firstName" -> user.firstName = value as String
                "lastName" -> user.lastName = value as String
                "username" -> {
                    val newUsername = value as String
                    if (newUsername != user.username) {
                        validateUsernameUnique(newUsername)
                        user.username = newUsername
                    }
                }
                "email" -> {
                    val newEmail = (value as String).lowercase()
                    if (newEmail != user.email) {
                        validateEmailUnique(newEmail)
                        user.email = newEmail
                    }
                }
                "bio" -> user.bio = value as? String
                "role" -> user.role = UserRole.valueOf(value as String)
                "active" -> user.active = value as Boolean
                else -> logger.warn("Unknown field in partial update: $key")
            }
        }

        val updatedUser = userRepository.save(user)
        logger.info("User partially updated successfully: $id")

        return userMapper.toDto(updatedUser)
    }

    @CacheEvict(value = [CACHE_NAME], key = CACHE_KEY)
    fun delete(id: Long) {
        logger.info("Deleting user with id: $id")

        if (!userRepository.existsById(id)) {
            throw ResourceNotFoundException("User", "id", id)
        }

        userRepository.deleteById(id)
        logger.info("User deleted successfully: $id")
    }

    @CacheEvict(value = [CACHE_NAME], allEntries = true)
    fun bulkDelete(ids: List<Long>) {
        logger.info("Bulk deleting users: $ids")

        val existingIds = userRepository.findAllById(ids).map { it.id }
        val nonExistentIds = ids - existingIds.toSet()

        if (nonExistentIds.isNotEmpty()) {
            logger.warn("Some IDs not found: $nonExistentIds")
        }

        userRepository.deleteAllByIdInBatch(existingIds)
        logger.info("Bulk delete completed. Deleted ${existingIds.size} users")
    }

    @CachePut(value = [CACHE_NAME], key = CACHE_KEY)
    fun deactivate(id: Long): UserResponseDto {
        logger.info("Deactivating user with id: $id")

        val user = userRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("User", "id", id)

        user.active = false
        val updatedUser = userRepository.save(user)

        logger.info("User deactivated successfully: $id")
        return userMapper.toDto(updatedUser)
    }

    @CachePut(value = [CACHE_NAME], key = CACHE_KEY)
    fun activate(id: Long): UserResponseDto {
        logger.info("Activating user with id: $id")

        val user = userRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("User", "id", id)

        user.active = true
        user.unlock() // Also unlock the account if it was locked
        val updatedUser = userRepository.save(user)

        logger.info("User activated successfully: $id")
        return userMapper.toDto(updatedUser)
    }

    suspend fun updateAvatar(id: Long, file: MultipartFile): UserResponseDto = withContext(Dispatchers.IO) {
        logger.info("Updating avatar for user: $id")

        validateImageFile(file)

        val user = userRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("User", "id", id)

        // Delete old avatar if exists
        user.profilePictureUrl?.let { oldUrl ->
            fileStorageService.deleteFile(oldUrl)
        }

        // Store new avatar
        val newUrl = fileStorageService.storeFile(file, "avatars/$id")
        user.profilePictureUrl = newUrl

        val updatedUser = userRepository.save(user)
        logger.info("Avatar updated successfully for user: $id")

        userMapper.toDto(updatedUser)
    }

    fun changePassword(id: Long, dto: ChangePasswordDto) {
        logger.info("Changing password for user: $id")

        val user = userRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("User", "id", id)

        // Verify current password
        if (!passwordEncoder.matches(dto.currentPassword, user.password)) {
            logger.warn("Invalid current password for user: $id")
            throw BusinessException("Current password is incorrect")
        }

        // Update password
        user.password = passwordEncoder.encode(dto.newPassword)
        userRepository.save(user)

        logger.info("Password changed successfully for user: $id")
    }

    @Transactional(readOnly = true)
    fun search(
        firstName: String?,
        lastName: String?,
        email: String?,
        pageable: Pageable
    ): Page<UserResponseDto> {
        logger.debug("Searching users with criteria - firstName: $firstName, lastName: $lastName, email: $email")

        val users = userRepository.searchUsers(firstName, lastName, email, pageable)
        return users.map(userMapper::toDto)
    }

    @Transactional(readOnly = true)
    fun getStatistics(): UserStatsDto {
        logger.debug("Generating user statistics")

        val totalUsers = userRepository.count()
        val activeUsers = userRepository.countByActive(true)
        val inactiveUsers = userRepository.countByActive(false)
        val roleDistribution = UserRole.values().associateWith { role ->
            userRepository.countByRole(role)
        }
        val recentRegistrations = userRepository.countRecentRegistrations(
            LocalDateTime.now().minusDays(30)
        )
        val verifiedEmails = userRepository.countByEmailVerified(true)

        return UserStatsDto(
            totalUsers = totalUsers,
            activeUsers = activeUsers,
            inactiveUsers = inactiveUsers,
            roleDistribution = roleDistribution,
            recentRegistrations = recentRegistrations,
            verifiedEmailPercentage = if (totalUsers > 0)
                (verifiedEmails * 100.0 / totalUsers) else 0.0
        )
    }

    suspend fun export(format: String): ByteArray = withContext(Dispatchers.IO) {
        logger.info("Exporting users in format: $format")

        val users = userRepository.findAll()
        val dtos = users.map(userMapper::toDto)

        when (format.uppercase()) {
            "JSON" -> objectMapper.writeValueAsBytes(dtos)
            "CSV" -> exportToCsv(dtos)
            "EXCEL" -> exportToExcel(dtos)
            else -> throw BusinessException("Unsupported export format: $format")
        }
    }

    // Validation helper methods
    private fun validateUniqueFields(username: String, email: String) {
        validateUsernameUnique(username)
        validateEmailUnique(email)
    }

    private fun validateUsernameUnique(username: String) {
        if (userRepository.existsByUsername(username)) {
            throw DuplicateResourceException("User with username '$username' already exists")
        }
    }

    private fun validateEmailUnique(email: String) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw DuplicateResourceException("User with email '$email' already exists")
        }
    }

    private fun validateImageFile(file: MultipartFile) {
        val allowedTypes = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
        if (file.contentType !in allowedTypes) {
            throw BusinessException("Invalid file type. Allowed types: ${allowedTypes.joinToString()}")
        }

        val maxSize = 5 * 1024 * 1024 // 5MB
        if (file.size > maxSize) {
            throw BusinessException("File size exceeds maximum allowed size of 5MB")
        }
    }

    // Export helper methods (simplified versions)
    private fun exportToCsv(users: List<UserResponseDto>): ByteArray {
        val csv = StringBuilder()
        csv.append("ID,Username,Email,First Name,Last Name,Role,Active,Created At\n")
        users.forEach { user ->
            csv.append("${user.id},${user.username},${user.email},")
            csv.append("${user.firstName},${user.lastName},${user.role},")
            csv.append("${user.active},${user.createdAt}\n")
        }
        return csv.toString().toByteArray()
    }

    private fun exportToExcel(users: List<UserResponseDto>): ByteArray {
        // Simplified - in real implementation use Apache POI
        return exportToCsv(users) // Fallback to CSV for now
    }

    // Either/Arrow example for functional error handling
    fun findUserEither(id: Long): Either<Throwable, UserResponseDto> {
        return try {
            val user = userRepository.findByIdOrNull(id)
                ?: return ResourceNotFoundException("User", "id", id).left()
            userMapper.toDto(user).right()
        } catch (e: Exception) {
            logger.error("Error finding user: $id", e)
            e.left()
        }
    }
}