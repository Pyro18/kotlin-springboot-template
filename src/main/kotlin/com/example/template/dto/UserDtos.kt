package com.example.template.dto

import com.example.template.entity.UserRole
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.time.LocalDateTime

// Request DTOs
@Schema(description = "User creation request")
data class CreateUserDto(
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    @Schema(description = "User's first name", example = "John", required = true)
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    @Schema(description = "User's last name", example = "Doe", required = true)
    val lastName: String,

    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "Username can only contain letters, numbers, underscore and hyphen"
    )
    @Schema(description = "Unique username", example = "johndoe", required = true)
    val username: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "User's email address", example = "john.doe@example.com", required = true)
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character"
    )
    @Schema(description = "User's password", example = "SecurePass123!", required = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val password: String,

    @field:Size(max = 500, message = "Bio must not exceed 500 characters")
    @Schema(description = "User biography", example = "Software developer passionate about Kotlin", required = false)
    val bio: String? = null,

    @Schema(description = "User role", example = "USER", required = false)
    val role: UserRole? = UserRole.USER
)

@Schema(description = "User update request")
data class UpdateUserDto(
    @field:Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    @Schema(description = "User's first name", example = "John", required = false)
    val firstName: String? = null,

    @field:Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    @Schema(description = "User's last name", example = "Doe", required = false)
    val lastName: String? = null,

    @field:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "Username can only contain letters, numbers, underscore and hyphen"
    )
    @Schema(description = "Unique username", example = "johndoe", required = false)
    val username: String? = null,

    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "User's email address", example = "john.doe@example.com", required = false)
    val email: String? = null,

    @field:Size(max = 500, message = "Bio must not exceed 500 characters")
    @Schema(description = "User biography", example = "Software developer passionate about Kotlin", required = false)
    val bio: String? = null,

    @Schema(description = "User role", example = "USER", required = false)
    val role: UserRole? = null
)

@Schema(description = "Password change request")
data class ChangePasswordDto(
    @field:NotBlank(message = "Current password is required")
    @Schema(description = "Current password", required = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val currentPassword: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character"
    )
    @Schema(description = "New password", required = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val newPassword: String,

    @field:NotBlank(message = "Password confirmation is required")
    @Schema(description = "New password confirmation", required = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val confirmPassword: String
) {
    @AssertTrue(message = "Passwords do not match")
    @JsonIgnore
    fun isPasswordsMatching(): Boolean = newPassword == confirmPassword
}

// Response DTOs
@Schema(description = "User response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserResponseDto(
    @Schema(description = "User ID", example = "1")
    val id: Long,

    @Schema(description = "Username", example = "johndoe")
    val username: String,

    @Schema(description = "Email address", example = "john.doe@example.com")
    val email: String,

    @Schema(description = "First name", example = "John")
    val firstName: String,

    @Schema(description = "Last name", example = "Doe")
    val lastName: String,

    @Schema(description = "Full name", example = "John Doe")
    val fullName: String = "$firstName $lastName",

    @Schema(description = "User role", example = "USER")
    val role: UserRole,

    @Schema(description = "Account active status", example = "true")
    val active: Boolean,

    @Schema(description = "Email verification status", example = "false")
    val emailVerified: Boolean,

    @Schema(description = "User biography", example = "Software developer")
    val bio: String? = null,

    @Schema(description = "Profile picture URL", example = "https://example.com/avatar.jpg")
    val profilePictureUrl: String? = null,

    @Schema(description = "Last login timestamp", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val lastLoginAt: LocalDateTime? = null,

    @Schema(description = "Account creation timestamp", example = "2024-01-01T12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime,

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val updatedAt: LocalDateTime? = null
)

@Schema(description = "User statistics")
data class UserStatsDto(
    @Schema(description = "Total number of users", example = "1000")
    val totalUsers: Long,

    @Schema(description = "Number of active users", example = "950")
    val activeUsers: Long,

    @Schema(description = "Number of inactive users", example = "50")
    val inactiveUsers: Long,

    @Schema(description = "Distribution of users by role")
    val roleDistribution: Map<UserRole, Long>,

    @Schema(description = "Number of recent registrations (last 30 days)", example = "125")
    val recentRegistrations: Long,

    @Schema(description = "Percentage of users with verified emails", example = "85.5")
    val verifiedEmailPercentage: Double
)

// Generic API Response wrapper
@Schema(description = "Generic API response wrapper")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponseDto<T>(
    @Schema(description = "Response status", example = "success")
    val status: String,

    @Schema(description = "Response message", example = "Operation completed successfully")
    val message: String? = null,

    @Schema(description = "Response data")
    val data: T? = null,

    @Schema(description = "Error details")
    val error: ErrorDetails? = null,

    @Schema(description = "Response timestamp", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T? = null, message: String? = null): ApiResponseDto<T> =
            ApiResponseDto(
                status = "success",
                message = message,
                data = data
            )

        fun <T> error(message: String, error: ErrorDetails? = null): ApiResponseDto<T> =
            ApiResponseDto(
                status = "error",
                message = message,
                error = error
            )
    }
}

@Schema(description = "Error details")
data class ErrorDetails(
    @Schema(description = "Error code", example = "USER_NOT_FOUND")
    val code: String? = null,

    @Schema(description = "Field-specific errors")
    val fieldErrors: Map<String, List<String>>? = null,

    @Schema(description = "Stack trace (only in dev mode)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val stackTrace: String? = null
)

// Pagination metadata
@Schema(description = "Pagination metadata")
data class PageMetadata(
    @Schema(description = "Current page number", example = "0")
    val page: Int,

    @Schema(description = "Number of items per page", example = "20")
    val size: Int,

    @Schema(description = "Total number of elements", example = "100")
    val totalElements: Long,

    @Schema(description = "Total number of pages", example = "5")
    val totalPages: Int,

    @Schema(description = "Is first page", example = "true")
    val first: Boolean,

    @Schema(description = "Is last page", example = "false")
    val last: Boolean,

    @Schema(description = "Has next page", example = "true")
    val hasNext: Boolean,

    @Schema(description = "Has previous page", example = "false")
    val hasPrevious: Boolean
)

@Schema(description = "Paginated response")
data class PagedResponseDto<T>(
    @Schema(description = "Page content")
    val content: List<T>,

    @Schema(description = "Pagination metadata")
    val metadata: PageMetadata
)

// Authentication DTOs
@Schema(description = "Login request")
data class LoginRequestDto(
    @field:NotBlank(message = "Username is required")
    @Schema(description = "Username or email", example = "johndoe", required = true)
    val username: String,

    @field:NotBlank(message = "Password is required")
    @Schema(description = "Password", required = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val password: String
)

@Schema(description = "Login response")
data class LoginResponseDto(
    @Schema(description = "JWT access token")
    val accessToken: String,

    @Schema(description = "JWT refresh token")
    val refreshToken: String,

    @Schema(description = "Token type", example = "Bearer")
    val tokenType: String = "Bearer",

    @Schema(description = "Token expiry in seconds", example = "3600")
    val expiresIn: Long,

    @Schema(description = "User information")
    val user: UserResponseDto
)

@Schema(description = "Token refresh request")
data class RefreshTokenDto(
    @field:NotBlank(message = "Refresh token is required")
    @Schema(description = "JWT refresh token", required = true)
    val refreshToken: String
)