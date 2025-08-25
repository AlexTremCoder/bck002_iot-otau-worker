package fr.velco.otau.stream.config

import fr.velco.back.framework.logging.VelcoLogger
import org.springframework.util.ErrorHandler

class AmqpExceptionHandler : ErrorHandler {
    private val log = VelcoLogger {}

    override fun handleError(exception: Throwable) {
        log.error(exception.cause?.message ?: "", null, exception.cause)
        throw exception
    }
}
