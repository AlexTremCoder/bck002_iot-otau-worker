package fr.velco.otau.services.dto.logbook

class LogbookEventDto(
    var relativeEntityType: String,
    var relativeEntityId: Long,
    var eventType: String,
    var eventDetail: String,
    var eventAttributes: List<LogbookEventAttributeDto>,
    var idAuthUser: Long?,
) {
}
