package fr.velco.otau.services

import fr.velco.back.framework.byte.HexStringUtils
import fr.velco.otau.persistences.velco.table.OtauTracking
import fr.velco.otau.persistences.velco.table.Product
import fr.velco.otau.services.dto.FirmwareDto
import fr.velco.otau.services.dto.ProductDto
import org.json.JSONArray

open class DataSet {
    protected fun getProduct(idFirmware: Long? = null, batteryLevel: Short? = null) = Product(
        id = 0,
        serialNumber = "N12345",
        batteryLevel =  batteryLevel,
        idNuotraxFirmwareAvailable = idFirmware,
    )

    protected fun getProductDto(idFirmware: Long? = null, batteryLevel: Short? = 75) = ProductDto(
        id = 0,
        serialNumber = "N12345",
        aesKey = HexStringUtils.fromHexString("6960472942B621D29623EC7E06141556"),
        idFirmware = idFirmware,
        batteryLevel = batteryLevel,
    )

    protected fun getFirmware() = FirmwareDto(
        version = "1.2.3.4",
        jsonArray = JSONArray(),
        totalPackets = 65535,
    )

    protected fun getOtauTracking() = OtauTracking(
        idProduct = 0,
        currentFirmwareVersion = "",
        currentBootloaderVersion = "",
        targetFirmwareVersion = "",
        durationInMinutes = 0,
        totalPacketsToSend = 0,
        lastPacketAcked = 2,
        nackPacketCounterConsecutive = 0,
        nackPacketCounterTotal = 0,
        progressPercentage = 0,
    )
}


