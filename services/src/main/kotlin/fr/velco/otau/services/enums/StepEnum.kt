package fr.velco.otau.services.enums

enum class StepEnum(val value: Int) {
    IDLE(0),
    ERASE_FLASH(1),
    DFU_RX(2),
    VERIFICATION(3),
    WAIT_VALIDATION(4),
    WAIT_PARK(5),
    RESET(6),
    ;

    companion object {
        fun from(value: Int): StepEnum? {
            return values().find { it.value == value }
        }
    }
}
