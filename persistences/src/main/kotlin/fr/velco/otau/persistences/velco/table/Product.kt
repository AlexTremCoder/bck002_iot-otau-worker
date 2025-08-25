package fr.velco.otau.persistences.velco.table

import java.time.LocalDateTime
import jakarta.persistence.*

/**
 * This table is the Achille heel of Velco SI
 * We can ruin all the worker performances when working with it
 *
 *                             USAGE    Hz  WHEN
 * id                              R  High  Each ACK          <- See ProductCacheService
 * aesKey                          R  High  Each interaction  <- See ProductCacheService
 * serialNumber                    R  High  Each interaction  <- See ProductCacheService
 * idNuotraxFirmwareAvailable    R/W  High  Each interaction  <- See ProductCacheService
 * batteryLevel                    R   Low  On start
 * nuotraxFirmwareVersion        R/W   Low  Version receiving
 * bootloaderVersion             R/W   Low  Version receiving
 * lastUpdate                      W   Low  Version receiving or on finish
 * productOtau                     W   Low  On finish (Cf ProductOtau)
 */
@Entity
@Table(name = "Product")
class Product(
    @Id
    @Column(name = "idProduct")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "lastUpdate")
    var lastUpdate: LocalDateTime? = null,

    @Column(name = "batteryLevel", columnDefinition = "TINYINT")
    val batteryLevel: Short? = null,

    @Column(name = "aesKey")
    val aesKey: ByteArray? = null,

    @Column(name = "serialNumber", nullable = false)
    val serialNumber: String,

    @Column(name = "idNuotraxFirmwareAvailable")
    var idNuotraxFirmwareAvailable: Long? = null,

    @Column(name = "nuotraxFirmwareVersion")
    var nuotraxFirmwareVersion: String? = null,

    @Column(name = "bootloaderVersion")
    var bootloaderVersion: String? = null,
)

