package fr.velco.otau.services.service.cache

import fr.velco.back.framework.logging.VelcoLogger
import fr.velco.otau.persistences.velco.dao.NuotraxFirmwareDao
import fr.velco.otau.services.config.CacheConfig.Companion.FIRMWARE_CACHE
import fr.velco.otau.services.dto.FirmwareDto
import org.json.JSONArray
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets

/**
 * Provide a cache to avoid overloading unnecessarily database
 * (On velco.NuotraxFirmware table, records are never updated in db, only new record are added)
 */
@Service
class FirmwareCacheService(
    private val nuotraxFirmwareDao: NuotraxFirmwareDao,
) {
    private val log = VelcoLogger {}

    @Cacheable(FIRMWARE_CACHE)
    fun getFirmware(id: Long): FirmwareDto {
        log.debug("getFirmware(id: $id)")
        val nuotraxFirmware = this.nuotraxFirmwareDao.getReferenceById(id)
        val jsonArray = this.getJsonArrayFromCpuFile(nuotraxFirmware.cpuFile) //cpu cost? Not really (0.005s for 158401 chars)
        return FirmwareDto(
            version = nuotraxFirmware.version,
            jsonArray = jsonArray,
            totalPackets = jsonArray.length(),
        )
    }

    private fun getJsonArrayFromCpuFile(cpuFile: ByteArray) = JSONArray(
        String(cpuFile, StandardCharsets.UTF_8)
    )
}
