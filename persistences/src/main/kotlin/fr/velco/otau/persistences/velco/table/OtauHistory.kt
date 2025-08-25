package fr.velco.otau.persistences.velco.table

import java.time.LocalDateTime
import jakarta.persistence.*

/**
 * Finished OTAU (SUCCESS or FAILED)
 */
@Entity
@Table(name = "otau_history")
class OtauHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Primary key auto generated
    @Column(name = "id_otau_history", nullable = false)
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
    var startDate: LocalDateTime,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "duration_in_minutes", nullable = false)
    var durationInMinutes: Int,

    @Column(name = "total_packets_to_send", nullable = false)
    val totalPacketsToSend: Int,

    @Column(name = "last_packet_acked")
    var lastPacketAcked: Int? = null,

    @Column(name = "nack_packet_counter_consecutive", nullable = false)
    val nackPacketCounterConsecutive: Int,

    @Column(name = "nack_packet_counter_total", nullable = false)
    val nackPacketCounterTotal: Int,

    @Column(name = "progress_percentage", columnDefinition = "TINYINT", nullable = false)
    var progressPercentage: Short,

    @Column(name = "result", nullable = false)
    var result: String, //SUCCESS or FAILURE

    @Column(name = "failure_reason")
    var failureReason: String? = null, //Cf. FailureReasonEnum
)
