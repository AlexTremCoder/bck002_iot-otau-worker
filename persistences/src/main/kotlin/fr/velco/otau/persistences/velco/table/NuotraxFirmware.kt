package fr.velco.otau.persistences.velco.table;

import jakarta.persistence.*

@Entity
@Table(name = "NuotraxFirmware")
class NuotraxFirmware(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idNuotraxFirmware", nullable = false)
    val id: Long = 0,

    @Column(name = "version", nullable = false)
    val version: String,

    @Column(name = "cpuFile", nullable = false)
    val cpuFile: ByteArray,
)
