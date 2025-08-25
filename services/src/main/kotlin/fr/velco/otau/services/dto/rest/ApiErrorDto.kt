package fr.velco.otau.services.dto.rest

import java.time.LocalDateTime

class ApiErrorDto(
    val status: Int,
    val error: String,
    val errorCode: String?,
    val timestamp: LocalDateTime,
)
