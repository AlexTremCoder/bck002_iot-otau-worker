package fr.velco.otau.webservice.config

import fr.velco.back.framework.logging.VelcoLogger
import fr.velco.otau.services.dto.rest.ApiErrorDto
import fr.velco.otau.services.exception.BadRequestException
import fr.velco.otau.services.exception.DataNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.LocalDateTime

/**
 * To return :
 * - 400 (HttpStatus.BAD_REQUEST), throw a BadRequestException
 * - 404 (HttpStatus.NOT_FOUND)  , throw a DataNotFoundException
 */
@ControllerAdvice
class ExceptionHandlerController : ResponseEntityExceptionHandler() {
    private val log = VelcoLogger {}

    private fun getApiError(status: Int, error: String, errorCode: String? = null): ApiErrorDto = ApiErrorDto(
        status = status,
        error = error,
        errorCode = errorCode,
        timestamp = LocalDateTime.now(),
    )

    /**
     * BadRequestException = Error 400 (HttpStatus.BAD_REQUEST)
     */
    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(e: BadRequestException): ResponseEntity<Any?>? {
        log.error("handleBadRequest : $e")
        return ResponseEntity(
            getApiError(status = 400, error = e.message ?: "Bad or missing query parameter"),
            HttpStatus.BAD_REQUEST,
        )
    }

    /**
     * DataNotFoundException = Error 404 (HttpStatus.NOT_FOUND)
     */
    @ExceptionHandler(DataNotFoundException::class)
    fun handleDataNotFound(e: DataNotFoundException): ResponseEntity<Any?>? {
        log.error("handleDataNotFound : $e")
        return ResponseEntity(
            getApiError(status = 404, error = e.message ?: "Resource not found"),
            HttpStatus.NOT_FOUND,
        )
    }
}
