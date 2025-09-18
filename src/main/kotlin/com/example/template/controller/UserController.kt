package com.example.template.controller

import com.example.template.dto.*
import com.example.template.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User CRUD operations")
@Validated
@SecurityRequirement(name = "Bearer Authentication")
class UserController(
    private val userService: UserService
) {

    @GetMapping
    @Operation(
        summary = "Get all users",
        description = "Retrieve a paginated list of all users with optional filtering"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content()]),
        ApiResponse(responseCode = "403", description = "Forbidden", content = [Content()])
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    fun getAllUsers(
        @ParameterObject
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
        @RequestParam(required = false)
        @Parameter(description = "Filter by active status")
        active: Boolean?,
        @RequestParam(required = false)
        @Parameter(description = "Filter by role")
        role: String?,
        @RequestParam(required = false)
        @Parameter(description = "Search by username or email")
        search: String?
    ): ResponseEntity<Page<UserResponseDto>> {
        val users = userService.findAll(pageable, active, role, search)
        return ResponseEntity.ok(users)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a single user by their ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "User found"),
        ApiResponse(responseCode = "404", description = "User not found", content = [Content()])
    )
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    fun getUserById(
        @PathVariable
        @Parameter(description = "User ID", required = true)
        id: Long
    ): ResponseEntity<UserResponseDto> {
        val user = userService.findById(id)
        return ResponseEntity.ok(user)
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Retrieve a user by their username")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "User found"),
        ApiResponse(responseCode = "404", description = "User not found", content = [Content()])
    )
    fun getUserByUsername(
        @PathVariable
        @Parameter(description = "Username", required = true)
        username: String
    ): ResponseEntity<UserResponseDto> {
        val user = userService.findByUsername(username)
        return ResponseEntity.ok(user)
    }

    @PostMapping
    @Operation(summary = "Create new user", description = "Register a new user in the system")
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = [Content(schema = Schema(implementation = UserResponseDto::class))]
        ),
        ApiResponse(responseCode = "400", description = "Invalid input", content = [Content()]),
        ApiResponse(responseCode = "409", description = "User already exists", content = [Content()])
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(
        @Valid @RequestBody
        @Parameter(description = "User creation data", required = true)
        createDto: CreateUserDto
    ): ResponseEntity<UserResponseDto> {
        val user = userService.create(createDto)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(user)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update an existing user's information")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "User updated successfully"),
        ApiResponse(responseCode = "400", description = "Invalid input", content = [Content()]),
        ApiResponse(responseCode = "404", description = "User not found", content = [Content()])
    )
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    fun updateUser(
        @PathVariable
        @Parameter(description = "User ID", required = true)
        id: Long,
        @Valid @RequestBody
        @Parameter(description = "User update data", required = true)
        updateDto: UpdateUserDto
    ): ResponseEntity<UserResponseDto> {
        val user = userService.update(id, updateDto)
        return ResponseEntity.ok(user)
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partial update user", description = "Partially update user fields")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "User updated successfully"),
        ApiResponse(responseCode = "400", description = "Invalid input", content = [Content()]),
        ApiResponse(responseCode = "404", description = "User not found", content = [Content()])
    )
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    fun patchUser(
        @PathVariable
        @Parameter(description = "User ID", required = true)
        id: Long,
        @RequestBody
        @Parameter(description = "Partial update data", required = true)
        patchDto: Map<String, Any>
    ): ResponseEntity<UserResponseDto> {
        val user = userService.partialUpdate(id, patchDto)
        return ResponseEntity.ok(user)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Remove a user from the system")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "User deleted successfully"),
        ApiResponse(responseCode = "404", description = "User not found", content = [Content()])
    )
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(
        @PathVariable
        @Parameter(description = "User ID", required = true)
        id: Long
    ): ResponseEntity<Void> {
        userService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate user", description = "Deactivate a user account")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "User deactivated successfully"),
        ApiResponse(responseCode = "404", description = "User not found", content = [Content()])
    )
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    fun deactivateUser(
        @PathVariable
        @Parameter(description = "User ID", required = true)
        id: Long
    ): ResponseEntity<UserResponseDto> {
        val user = userService.deactivate(id)
        return ResponseEntity.ok(user)
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate user", description = "Activate a user account")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "User activated successfully"),
        ApiResponse(responseCode = "404", description = "User not found", content = [Content()])
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun activateUser(
        @PathVariable
        @Parameter(description = "User ID", required = true)
        id: Long
    ): ResponseEntity<UserResponseDto> {
        val user = userService.activate(id)
        return ResponseEntity.ok(user)
    }

    @PostMapping("/{id}/upload-avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload user avatar", description = "Upload a profile picture for the user")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Avatar uploaded successfully"),
        ApiResponse(responseCode = "400", description = "Invalid file", content = [Content()]),
        ApiResponse(responseCode = "404", description = "User not found", content = [Content()])
    )
    @PreAuthorize("#id == authentication.principal.id")
    suspend fun uploadAvatar(
        @PathVariable
        @Parameter(description = "User ID", required = true)
        id: Long,
        @RequestParam("file")
        @Parameter(description = "Avatar image file", required = true)
        file: MultipartFile
    ): ResponseEntity<UserResponseDto> {
        val user = userService.updateAvatar(id, file)
        return ResponseEntity.ok(user)
    }

    @PostMapping("/{id}/change-password")
    @Operation(summary = "Change password", description = "Change user's password")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Password changed successfully"),
        ApiResponse(responseCode = "400", description = "Invalid password", content = [Content()]),
        ApiResponse(responseCode = "404", description = "User not found", content = [Content()])
    )
    @PreAuthorize("#id == authentication.principal.id")
    fun changePassword(
        @PathVariable
        @Parameter(description = "User ID", required = true)
        id: Long,
        @Valid @RequestBody
        @Parameter(description = "Password change data", required = true)
        passwordDto: ChangePasswordDto
    ): ResponseEntity<ApiResponseDto<String>> {
        userService.changePassword(id, passwordDto)
        return ResponseEntity.ok(
            ApiResponseDto.success("Password changed successfully")
        )
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users by various criteria")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Search results returned")
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    fun searchUsers(
        @RequestParam(required = false) firstName: String?,
        @RequestParam(required = false) lastName: String?,
        @RequestParam(required = false) email: String?,
        @ParameterObject pageable: Pageable
    ): ResponseEntity<Page<UserResponseDto>> {
        val users = userService.search(firstName, lastName, email, pageable)
        return ResponseEntity.ok(users)
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user statistics", description = "Get statistical data about users")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserStats(): ResponseEntity<UserStatsDto> {
        val stats = userService.getStatistics()
        return ResponseEntity.ok(stats)
    }

    @PostMapping("/bulk-delete")
    @Operation(summary = "Bulk delete users", description = "Delete multiple users at once")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Users deleted successfully"),
        ApiResponse(responseCode = "400", description = "Invalid IDs", content = [Content()])
    )
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun bulkDeleteUsers(
        @RequestBody
        @Parameter(description = "List of user IDs to delete", required = true)
        ids: List<Long>
    ): ResponseEntity<Void> {
        userService.bulkDelete(ids)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/export")
    @Operation(summary = "Export users", description = "Export user data in various formats")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Export generated successfully")
    )
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun exportUsers(
        @RequestParam(defaultValue = "CSV")
        @Parameter(description = "Export format", schema = Schema(allowableValues = ["CSV", "JSON", "EXCEL"]))
        format: String
    ): ResponseEntity<ByteArray> {
        val data = userService.export(format)
        val mediaType = when (format.uppercase()) {
            "JSON" -> MediaType.APPLICATION_JSON
            "EXCEL" -> MediaType.parseMediaType("application/vnd.ms-excel")
            else -> MediaType.parseMediaType("text/csv")
        }

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header("Content-Disposition", "attachment; filename=users.$format")
            .body(data)
    }
}