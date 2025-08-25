package fr.velco.otau.services.service

import fr.velco.back.framework.logging.VelcoLogger

import fr.velco.otau.services.enums.FailureReasonEnum
import fr.velco.otau.persistences.velco.dao.OtauHistoryDao
import fr.velco.otau.persistences.velco.table.OtauHistory
import fr.velco.otau.persistences.velco.table.OtauTracking
import org.springframework.stereotype.Service

/**
 * Manage the velco.otau_history table
 */
@Service
class OtauHistoryService(
    private val otauHistoryDao: OtauHistoryDao,
) {
    private val log = VelcoLogger {}

    fun add(otauTracking: OtauTracking, failureReasonEnum: FailureReasonEnum? = null) {
        log.debug("add()")

        //Move record from otau_tracking -> otau_history
        this.otauHistoryDao.save(
            OtauHistory(
                idProduct = otauTracking.idProduct,
                currentFirmwareVersion = otauTracking.currentFirmwareVersion,
                currentBootloaderVersion = otauTracking.currentBootloaderVersion,
                targetFirmwareVersion = otauTracking.targetFirmwareVersion,
                startDate = otauTracking.startDate,
                durationInMinutes = otauTracking.durationInMinutes,
                totalPacketsToSend = otauTracking.totalPacketsToSend,
                lastPacketAcked = otauTracking.lastPacketAcked,
                nackPacketCounterConsecutive = otauTracking.nackPacketCounterConsecutive,
                nackPacketCounterTotal = otauTracking.nackPacketCounterTotal,
                progressPercentage = otauTracking.progressPercentage,
                result = if (failureReasonEnum == null) "SUCCESS" else "FAILURE",
                failureReason = failureReasonEnum?.name,
            )
        )
    }
}
