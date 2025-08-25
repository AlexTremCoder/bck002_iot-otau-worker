package fr.velco.otau.services.dto.rest

/**
 * Lot of nullable values because this dto handle many different REST bodies
 */
class DfuDataDataDto(
    val numberOfLine: Int? = null,                         //body for START_OF_TRANSMISSION
    val packetTransmission: PacketTransmissionDto? = null, //body for PACKET_TRANSMISSION
    val updateAction: Int? = null,                         //body for END_OF_TRANSMISSION
)
