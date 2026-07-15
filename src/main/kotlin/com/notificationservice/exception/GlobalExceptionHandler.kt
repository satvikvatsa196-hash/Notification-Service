package com.notificationservice.exception

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException

/**
 * Centralized exception handler for all REST endpoints.
 * Converts exceptions into a consistent [ErrorResponse] JSON envelope.
 *
 * Handler priority (top-to-bottom):
 *  1. Domain exceptions (ResourceNotFoundException, DuplicateResourceException)
 *  2. Validation exceptions (Bean Validation, request body parsing)
 *  3. Spring MVC exceptions (routing, type mismatch)
 *  4. Database exceptions (DataIntegrityViolation)
 *  5. Catch-all for unexpected server errors
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // ── Domain Exceptions ────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(
        ex: ResourceNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.debug("Resource not found: {}", ex.message)
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.message!!, request)
    }

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicateResource(
        ex: DuplicateResourceException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.debug("Duplicate resource: {}", ex.message)
        return buildResponse(HttpStatus.CONFLICT, "Conflict", ex.message!!, request)
    }

    // ── Validation Exceptions ────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.map { fe ->
            ErrorResponse.FieldError(
                field = fe.field,
                message = fe.defaultMessage ?: "Invalid value",
                rejectedValue = fe.rejectedValue
            )
        }
        val globalErrors = ex.bindingResult.globalErrors.map { ge ->
            ErrorResponse.FieldError(
                field = ge.objectName,
                message = ge.defaultMessage ?: "Invalid value"
            )
        }

        log.debug("Validation failed: {} field errors", fieldErrors.size)

        val response = ErrorResponse(
            status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
            error = "Validation Failed",
            message = "Request contains ${fieldErrors.size + globalErrors.size} validation error(s)",
            path = request.requestURI,
            details = fieldErrors + globalErrors
        )
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.constraintViolations.map { cv ->
            ErrorResponse.FieldError(
                field = cv.propertyPath.toString(),
                message = cv.message,
                rejectedValue = cv.invalidValue
            )
        }
        val response = ErrorResponse(
            status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
            error = "Constraint Violation",
            message = "One or more constraints were violated",
            path = request.requestURI,
            details = fieldErrors
        )
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.debug("Malformed request body: {}", ex.message)
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "Malformed Request",
            "Request body is malformed or contains invalid JSON",
            request
        )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingRequestParam(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "Missing Parameter",
            "Required request parameter '${ex.parameterName}' is missing",
            request
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val expected = ex.requiredType?.simpleName ?: "unknown"
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "Type Mismatch",
            "Parameter '${ex.name}' must be of type '$expected'",
            request
        )
    }

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFound(
        ex: NoHandlerFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return buildResponse(
            HttpStatus.NOT_FOUND,
            "Not Found",
            "No endpoint found for ${ex.httpMethod} ${ex.requestURL}",
            request
        )
    }

    // ── Database Exceptions ──────────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Data integrity violation at {}: {}", request.requestURI, ex.mostSpecificCause.message)
        return buildResponse(
            HttpStatus.CONFLICT,
            "Data Integrity Violation",
            "The operation would violate a data constraint. Please check for duplicate or invalid references.",
            request
        )
    }

    // ── Catch-All ────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error at {} {}: ", request.method, request.requestURI, ex)
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            request
        )
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private fun buildResponse(
        status: HttpStatus,
        error: String,
        message: String,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = status.value(),
            error = error,
            message = message,
            path = request.requestURI
        )
        return ResponseEntity.status(status).body(response)
    }
}
