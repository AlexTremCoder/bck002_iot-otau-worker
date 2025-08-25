package fr.velco.otau.persistences.velco.table

import java.time.LocalDateTime
import jakarta.persistence.*

/**
 * In progress OTAU
 */
@Entity
@Table(name = "otau_tracking")
class OtauTracking(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Primary key auto generated
    @Column(name = "id_otau_tracking", nullable = false)
    var id: Long = 0,

    @Column(name = "id_product", nullable = false)
    val idProduct: Long,

    @Column(name = "current_firmware_version", nullable = false)
    val currentFirmwareVersion: String,

    @Column(name = "current_bootloader_version", nullable = false)
    val currentBootloaderVersion: String,

    @Column(name = "target_firmware_version", nullable = false)
    val targetFirmwareVersion: String,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_update", nullable = false)
    var lastUpdate: LocalDateTime = LocalDateTime.now(), //Allow to know which OTAU are stucked

    @Column(name = "duration_in_minutes", nullable = false)
    var durationInMinutes: Int,

    @Column(name = "total_packets_to_send", nullable = false)
    val totalPacketsToSend: Int,

    @Column(name = "last_packet_acked")
    var lastPacketAcked: Int? = null,

    @Column(name = "nack_packet_counter_consecutive", nullable = false)
    var nackPacketCounterConsecutive: Int,

    @Column(name = "nack_packet_counter_total", nullable = false)
    var nackPacketCounterTotal: Int,

    @Column(name = "progress_percentage", columnDefinition = "TINYINT", nullable = false)
    var progressPercentage: Short,
)
