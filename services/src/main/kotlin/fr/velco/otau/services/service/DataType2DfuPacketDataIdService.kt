package fr.velco.otau.services.service

import fr.velco.back.framework.byte.ByteUtils
import fr.velco.back.framework.logging.VelcoLogger
import fr.velco.otau.persistences.velco.dao.ProductDao
import fr.velco.otau.services.config.Properties
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.enums.StepEnum
import fr.velco.otau.services.service.cache.FirmwareCacheService
import org.springframework.stereotype.Service

/**
 * Handle Dfu Status>DFU Packet Data ID
 * https://velco-tech.atlassian.net/wiki/spaces/05IT/pages/2515795982/iot-otau-worker+handling+messages#Data-Type-2-%3A-DFU_PACKET_DATA_ID
 * https://velco-tech.atlassian.net/wiki/spaces/RD/pages/1636990985/9.+Topic+-+DFU+STATUS
 *
 * This service resumes an OTAU when IoT lost contact with SI
 */
@Service
class DataType2DfuPacketDataIdService(
    private val properties: Properties,
    private val productDao: ProductDao,
    private val dataTypeService: DataTypeService,
    private val firmwareCacheService: FirmwareCacheService,
    private val dfuDataTopicService: DfuDataTopicService,
    private val otauTrackingService: OtauTrackingService,
) {
    private val log = VelcoLogger {}

    fun treat(productDto: ProductDto, payload: ByteArray) {
        val logCtx = mutableMapOf("context" to "DFU Status 2:DFU Packet Data ID", "iotSerialNumber" to productDto.serialNumber)
        log.debug("treat()", logCtx)

        if (payload.size != 4) {
            log.error("Invalid payload (Excepted: 4 but ${payload.size})", logCtx)
            return
        }

        val iotLastAcked: Int = ByteUtils.byteArrayToShort(
            payload.copyOfRange(1, 3) //Bytes 1-2 are the packet number
        ).toInt()
        val stepByte: Byte = payload[3]
        val step: StepEnum = StepEnum.from(stepByte.toInt()) ?: throw Exception("Unknown step ($stepByte)")

        logCtx += mapOf("iotLastAcked" to iotLastAcked.toString(), "step" to step.toString())

        log.info("Process step $step", logCtx)
        when (step) {
            StepEnum.IDLE -> {
                if (this.dataTypeService.sendEndOfTransmissionIfNotEligibleToATargetVersion(productDto, logCtx)) return

                if (iotLastAcked == 0) { //IoT ready for a new OTAU?
                    this.startOtau(productDto, logCtx)
                }
            }

            StepEnum.DFU_RX -> { //IoT in progress
                if (this.dataTypeService.sendEndOfTransmissionIfNotEligibleToATargetVersion(productDto, logCtx)) return
                if (!this.otauTrackingService.isOtauSlotAvailable(logCtx)) {
                    return
                }
                val otauTracking = this.dataTypeService.getOtauTrackingOrSendEndOfTransmission(productDto, logCtx) ?: return

                //Case of ACK/NACK #1
                //After sending first packet, otau_tracking.last_packet_acked is still NULL in database
                val otauTrackingLastPacketAcked = otauTracking.lastPacketAcked ?: 0

                //Allow only 2 cases:
                //- lastPacketAcked == otauTrackingLastPacketAcked -> The previous packet sent has been lost, IoT request it again
                //- lastPacketAcked > otauTrackingLastPacketAcked -> A ACK has not been received by the SI. Re-sync SI with IoT

                //All other cases are ignored
                if (iotLastAcked < otauTrackingLastPacketAcked) {
                    if (otauTracking.lastPacketAcked != null) logCtx += mapOf("lastPacketAcked" to otauTrackingLastPacketAcked.toString())
                    log.info("Receive DFU_RX with unexpected index. Stop OTAU", logCtx)
                    this.dfuDataTopicService.sendEndOfTransmission(productDto, logCtx)
                    return
                }

                //A DFU_RX is like an ACK
                this.otauTrackingService.setLastPacketAcked(productDto, iotLastAcked, logCtx)

                logCtx += mapOf("lastPacketAcked" to iotLastAcked.toString())

                if (this.dataTypeService.isLastPacket(productDto, iotLastAcked, logCtx)) { //All packets already transmitted?
                    log.info("Last packet ACK", logCtx)
                    this.dfuDataTopicService.sendEndOfTransmission(productDto, logCtx)
                    return
                }

                val packetToSend = iotLastAcked + 1
                logCtx += mapOf("packetToSend" to packetToSend.toString())

                this.dataTypeService.sendPacket(productDto, packetToSend, logCtx, logWithoutModulo = true)
            }

            StepEnum.WAIT_VALIDATION -> {
                if (this.dataTypeService.sendDfuCancelIfNotEligibleToATargetVersion(productDto, logCtx)) return
                if (this.dataTypeService.sendDfuCancelIfNoTracking(productDto, logCtx)) return

                this.dfuDataTopicService.sendUpdateValidation(productDto, true, logCtx) //Go (again) for flashing
            }

            else -> log.info("No action required for this type of DFU_PACKET_DATA_ID message", logCtx)
        }
    }

    /**
     * Starting OTAU involves these 2 steps:
     * - sendStartOfTransmission()
     * - sendPacket(#1)
     */
    private fun startOtau(productDto: ProductDto, logCtx: MutableMap<String, String>) {
        log.debug("startOtau()", logCtx)
        //Is the IoT Battery not too low?
        if (!this.dataTypeService.isBatteryLevelSufficient(productDto.batteryLevel, logCtx)) {
            return
        }

        if (!this.otauTrackingService.isOtauSlotAvailable(logCtx)) {
            return
        }

        val firmware = this.firmwareCacheService.getFirmware(productDto.idFirmware ?: throw Exception("Product.idNuotraxFirmwareAvailable cannot be null here"))

        val packetToSend = 1
        logCtx += mapOf("totalPacketsToSend" to firmware.totalPackets.toString(), "packetToSend" to packetToSend.toString())

        val product = productDao.getReferenceById(productDto.id)
        this.otauTrackingService.start(product, logCtx) //Insert a new record into otau_tracking table
        this.dfuDataTopicService.sendStartOfTransmission(productDto, packetCount = firmware.totalPackets, logCtx)
        this.dataTypeService.sendPacket(productDto, packetToSend, logCtx, logWithoutModulo = true)
    }
}
