package fr.velco.otau.services.enums

enum class DfuStatusDataTypeEnum(val value: Int) {
    NUOTRAX_VERSION(0),
    DFU_DATA_ACK_NACK(1),
    DFU_PACKET_DATA_ID(2),
    DFU_READY_STATUS(3),
    ;

    companion object {
        fun from(value: Int): DfuStatusDataTypeEnum? {
            return values().find { it.value == value }
        }
    }
}
