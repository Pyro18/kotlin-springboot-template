package com.example.template.mapper

import com.example.template.dto.*
import com.example.template.entity.User
import com.example.template.entity.UserRole
import org.mapstruct.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
abstract class UserMapper {

    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder

    // Entity to Response DTO
    abstract fun toDto(user: User): UserResponseDto

    // Create DTO to Entity
    @Mappings(
        Mapping(target = "id", ignore = true),
        Mapping(target = "password", ignore = true), // Will be handled in afterMapping
        Mapping(target = "active", constant = "true"),
        Mapping(target = "emailVerified", constant = "false"),
        Mapping(target = "lastLoginAt", ignore = true),
        Mapping(target = "failedLoginAttempts", constant = "0"),
        Mapping(target = "lockedUntil", ignore = true),
        Mapping(target = "permissions", ignore = true),
        Mapping(target = "sessions", ignore = true),
        Mapping(target = "version", constant = "0L"),
        Mapping(target = "createdAt", ignore = true),
        Mapping(target = "updatedAt", ignore = true),
        Mapping(target = "profilePictureUrl", ignore = true)
    )
    abstract fun toEntity(dto: CreateUserDto): User

    @AfterMapping
    protected fun encodePassword(dto: CreateUserDto, @MappingTarget user: User) {
        user.password = passwordEncoder.encode(dto.password)
        user.email = dto.email.lowercase()
    }

    // Update DTO to Entity (partial update)
    @Mappings(
        Mapping(target = "id", ignore = true),
        Mapping(target = "password", ignore = true),
        Mapping(target = "active", ignore = true),
        Mapping(target = "emailVerified", ignore = true),
        Mapping(target = "lastLoginAt", ignore = true),
        Mapping(target = "failedLoginAttempts", ignore = true),
        Mapping(target = "lockedUntil", ignore = true),
        Mapping(target = "permissions", ignore = true),
        Mapping(target = "sessions", ignore = true),
        Mapping(target = "version", ignore = true),
        Mapping(target = "createdAt", ignore = true),
        Mapping(target = "updatedAt", ignore = true),
        Mapping(target = "profilePictureUrl", ignore = true)
    )
    abstract fun updateEntityFromDto(dto: UpdateUserDto, @MappingTarget user: User)

    @AfterMapping
    protected fun normalizeEmailOnUpdate(dto: UpdateUserDto, @MappingTarget user: User) {
        dto.email?.let {
            user.email = it.lowercase()
        }
    }

    // List mapping
    abstract fun toDtoList(users: List<User>): List<UserResponseDto>

    abstract fun toEntityList(dtos: List<CreateUserDto>): List<User>

    // Custom mapping methods for complex transformations
    fun toSimpleDto(user: User): Map<String, Any?> {
        return mapOf(
            "id" to user.id,
            "username" to user.username,
            "email" to user.email,
            "fullName" to "${user.firstName} ${user.lastName}",
            "role" to user.role.name,
            "active" to user.active
        )
    }

    fun toDetailedDto(user: User, includePermissions: Boolean = false): Map<String, Any?> {
        val baseMap = mutableMapOf<String, Any?>(
            "id" to user.id,
            "username" to user.username,
            "email" to user.email,
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "fullName" to "${user.firstName} ${user.lastName}",
            "role" to user.role.name,
            "active" to user.active,
            "emailVerified" to user.emailVerified,
            "bio" to user.bio,
            "profilePictureUrl" to user.profilePictureUrl,
            "lastLoginAt" to user.lastLoginAt,
            "accountLocked" to user.isAccountLocked(),
            "createdAt" to user.createdAt,
            "updatedAt" to user.updatedAt
        )

        if (includePermissions) {
            baseMap["permissions"] = ArrayList(user.permissions)
        }

        return baseMap
    }

    // Specialized mappers for different contexts
    fun toPublicProfileDto(user: User): PublicProfileDto {
        return PublicProfileDto(
            username = user.username,
            firstName = user.firstName,
            lastName = user.lastName,
            bio = user.bio,
            profilePictureUrl = user.profilePictureUrl,
            role = user.role,
            createdAt = user.createdAt
        )
    }

    fun toAuthResponseDto(user: User): AuthUserDto {
        return AuthUserDto(
            id = user.id!!,
            username = user.username,
            email = user.email,
            role = user.role,
            permissions = user.permissions.toSet(),
            emailVerified = user.emailVerified,
            active = user.active
        )
    }
}

// Additional specialized DTOs for different contexts
data class PublicProfileDto(
    val username: String,
    val firstName: String,
    val lastName: String,
    val bio: String?,
    val profilePictureUrl: String?,
    val role: UserRole,
    val createdAt: LocalDateTime?
)

data class AuthUserDto(
    val id: Long,
    val username: String,
    val email: String,
    val role: UserRole,
    val permissions: Set<String>,
    val emailVerified: Boolean,
    val active: Boolean
)

// Custom MapStruct configuration for specific scenarios
@MapperConfig(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
interface UserMapperConfig

// Extension functions for convenient mapping
fun User.toResponseDto(mapper: UserMapper): UserResponseDto = mapper.toDto(this)
fun User.toPublicProfile(mapper: UserMapper): PublicProfileDto = mapper.toPublicProfileDto(this)
fun User.toAuthDto(mapper: UserMapper): AuthUserDto = mapper.toAuthResponseDto(this)
fun CreateUserDto.toEntity(mapper: UserMapper): User = mapper.toEntity(this)
fun UpdateUserDto.applyToEntity(user: User, mapper: UserMapper) = mapper.updateEntityFromDto(this, user)