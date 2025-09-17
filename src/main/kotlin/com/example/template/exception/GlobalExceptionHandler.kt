package com.example.template.exception

import com.example.template.dto.ApiResponseDto
import com.example.template.dto.ErrorDetails
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.NoHandlerFoundException
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler(
    @Value("\${spring.profiles.active:prod}")
    private val activeProfile: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Custom Exceptions
    @ExceptionHandler(ResourceNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleResourceNotFoundException(
        ex: ResourceNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Resource not found: ${ex.message}")

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiResponseDto.error(
                message = ex.message ?: "Resource not found",
                error = ErrorDetails(
                    code = "RESOURCE_NOT_FOUND",
                    stackTrace = if (isDevelopment()) getStackTrace(ex) else null
                )
            )
        )
    }

    @ExceptionHandler(DuplicateResourceException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleDuplicateResourceException(
        ex: DuplicateResourceException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Duplicate resource: ${ex.message}")

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiResponseDto.error(
                message = ex.message ?: "Resource already exists",
                error = ErrorDetails(
                    code = "DUPLICATE_RESOURCE",
                    stackTrace = if (isDevelopment()) getStackTrace(ex) else null
                )
            )
        )
    }

    @ExceptionHandler(BusinessException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBusinessException(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Business logic error: ${ex.message}")

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiResponseDto.error(
                message = ex.message ?: "Business logic error",
                error = ErrorDetails(
                    code = ex.errorCode,
                    stackTrace = if (isDevelopment()) getStackTrace(ex) else null
                )
            )
        )
    }

    // Validation Exceptions
    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Validation error: ${ex.message}")

        val fieldErrors = ex.bindingResult.allErrors
            .groupBy { (it as? FieldError)?.field ?: "general" }
            .mapValues { entry ->
                entry.value.mapNotNull { it.defaultMessage }
            }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiResponseDto.error(
                message = "Validation failed",
                error = ErrorDetails(
                    code = "VALIDATION_ERROR",
                    fieldErrors = fieldErrors,
                    stackTrace = if (isDevelopment()) getStackTrace(ex) else null
                )
            )
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleConstraintViolationException(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Constraint violation: ${ex.message}")

        val violations = ex.constraintViolations
            .groupBy { it.propertyPath.toString() }
            .mapValues { entry ->
                entry.value.map { it.message }
            }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiResponseDto.error(
                message = "Validation failed",
                error = ErrorDetails(
                    code = "CONSTRAINT_VIOLATION",
                    fieldErrors = violations,
                    stackTrace = if (isDevelopment()) getStackTrace(ex) else null
                )
            )
        )
    }

    // Security Exceptions
    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Access denied: ${ex.message}")

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiResponseDto.error(
                message = "Access denied: You don't have permission to access this resource",
                error = ErrorDetails(code = "ACCESS_DENIED")
            )
        )
    }

    @ExceptionHandler(BadCredentialsException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleBadCredentialsException(
        ex: BadCredentialsException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Bad credentials: ${ex.message}")

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiResponseDto.error(
                message = "Invalid username or password",
                error = ErrorDetails(code = "BAD_CREDENTIALS")
            )
        )
    }

    @ExceptionHandler(LockedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleLockedException(
        ex: LockedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Account locked: ${ex.message}")

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiResponseDto.error(
                message = "Account is locked. Please contact support",
                error = ErrorDetails(code = "ACCOUNT_LOCKED")
            )
        )
    }

    @ExceptionHandler(DisabledException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleDisabledException(
        ex: DisabledException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Account disabled: ${ex.message}")

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiResponseDto.error(
                message = "Account is disabled. Please contact support",
                error = ErrorDetails(code = "ACCOUNT_DISABLED")
            )
        )
    }

    @ExceptionHandler(AuthenticationException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Authentication failed: ${ex.message}")

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiResponseDto.error(
                message = ex.message ?: "Authentication failed",
                error = ErrorDetails(code = "AUTHENTICATION_FAILED")
            )
        )
    }

    // Database Exceptions
    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleDataIntegrityViolationException(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Data integrity violation: ${ex.message}")

        val message = when {
            ex.message?.contains("unique", ignoreCase = true) == true ->
                "A record with this value already exists"
            ex.message?.contains("foreign key", ignoreCase = true) == true ->
                "Cannot delete or update: this record is referenced by other data"
            else -> "Data integrity violation"
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiResponseDto.error(
                message = message,
                error = ErrorDetails(
                    code = "DATA_INTEGRITY_VIOLATION",
                    stackTrace = if (isDevelopment()) getStackTrace(ex) else null
                )
            )
        )
    }

    // File Upload Exceptions
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    fun handleMaxUploadSizeExceededException(
        ex: MaxUploadSizeExceededException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("File too large: ${ex.message}")

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            ApiResponseDto.error(
                message = "File size exceeds maximum allowed size",
                error = ErrorDetails(code = "FILE_TOO_LARGE")
            )
        )
    }

    // HTTP Exceptions
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    fun handleHttpRequestMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Method not allowed: ${ex.message}")

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
            ApiResponseDto.error(
                message = "Method ${ex.method} is not supported for this endpoint",
                error = ErrorDetails(code = "METHOD_NOT_ALLOWED")
            )
        )
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    fun handleHttpMediaTypeNotSupported(
        ex: HttpMediaTypeNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Media type not supported: ${ex.message}")

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
            ApiResponseDto.error(
                message = "Media type is not supported",
                error = ErrorDetails(code = "UNSUPPORTED_MEDIA_TYPE")
            )
        )
    }

    @ExceptionHandler(NoHandlerFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNoHandlerFoundException(
        ex: NoHandlerFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Handler not found: ${ex.message}")

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiResponseDto.error(
                message = "Endpoint not found: ${ex.requestURL}",
                error = ErrorDetails(code = "ENDPOINT_NOT_FOUND")
            )
        )
    }

    // Request Parameter Exceptions
    @ExceptionHandler(MissingServletRequestParameterException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Missing parameter: ${ex.message}")

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiResponseDto.error(
                message = "Required parameter '${ex.parameterName}' is missing",
                error = ErrorDetails(code = "MISSING_PARAMETER")
            )
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMethodArgumentTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Type mismatch: ${ex.message}")

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiResponseDto.error(
                message = "Invalid value for parameter '${ex.name}': expected ${ex.requiredType?.simpleName}",
                error = ErrorDetails(code = "TYPE_MISMATCH")
            )
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Message not readable: ${ex.message}")

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiResponseDto.error(
                message = "Malformed JSON request",
                error = ErrorDetails(
                    code = "MALFORMED_JSON",
                    stackTrace = if (isDevelopment()) getStackTrace(ex) else null
                )
            )
        )
    }

    // Generic Exception Handler
    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Unexpected error: ${ex.message}", ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiResponseDto.error(
                message = if (isDevelopment()) ex.message ?: "An unexpected error occurred"
                else "An unexpected error occurred",
                error = ErrorDetails(
                    code = "INTERNAL_SERVER_ERROR",
                    stackTrace = if (isDevelopment()) getStackTrace(ex) else null
                )
            )
        )
    }

    // Helper methods
    private fun isDevelopment(): Boolean = activeProfile == "dev"

    private fun getStackTrace(ex: Exception): String {
        val sw = StringWriter()
        ex.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }
}

// Custom Exception Classes
class ResourceNotFoundException(
    resourceName: String,
    fieldName: String,
    fieldValue: Any
) : RuntimeException("$resourceName not found with $fieldName: $fieldValue")

class DuplicateResourceException(message: String) : RuntimeException(message)

class BusinessException(
    message: String,
    val errorCode: String? = null
) : RuntimeException(message)

class InvalidTokenException(message: String) : RuntimeException(message)

class TokenExpiredException(message: String) : RuntimeException(message)