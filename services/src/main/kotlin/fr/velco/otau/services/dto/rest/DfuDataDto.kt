package fr.velco.otau.services.dto.rest

import fr.velco.otau.services.enums.rest.DfuDataEnum

class DfuDataDto(
    val dataType: DfuDataEnum,
    val data: DfuDataDataDto? = null,
)
