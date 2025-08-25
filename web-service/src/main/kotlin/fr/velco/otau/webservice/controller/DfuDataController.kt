package fr.velco.otau.webservice.controller

import fr.velco.otau.services.dto.rest.DfuDataDto
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import fr.velco.otau.services.service.DfuDataRestService
import fr.velco.otau.services.enums.rest.DfuDataEnum

@RestController
class DfuDataController(
    private val dfuDataRestService: DfuDataRestService,
) {
    @PostMapping("/iot/{serialNumber}/dfu-data")
    fun dfuData(
        @PathVariable("serialNumber") serialNumber: String,
        @RequestBody dfuDataDto: DfuDataDto,
    ) {
        when (dfuDataDto.dataType) {
            DfuDataEnum.ASK_NUOTRAX_VERSION -> this.dfuDataRestService.treatAskNuotraxVersion(serialNumber, dfuDataDto)
            DfuDataEnum.START_OF_TRANSMISSION -> this.dfuDataRestService.treatStartOfTransmission(serialNumber, dfuDataDto)
            DfuDataEnum.PACKET_TRANSMISSION -> this.dfuDataRestService.treatPacketTransmission(serialNumber, dfuDataDto)
            DfuDataEnum.END_OF_TRANSMISSION -> this.dfuDataRestService.treatEndOfTransmission(serialNumber, dfuDataDto)
            DfuDataEnum.ASK_FOR_LAST_PACKET -> this.dfuDataRestService.treatAskForLastPacket(serialNumber, dfuDataDto)
            DfuDataEnum.UPDATE_VALIDATION -> this.dfuDataRestService.treatUpdateValidation(serialNumber, dfuDataDto)
        }
    }
}
