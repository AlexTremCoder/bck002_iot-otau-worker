package fr.velco.otau.persistences.velco.dao

import fr.velco.otau.persistences.velco.table.NuotraxFirmware
import org.springframework.data.jpa.repository.JpaRepository

interface NuotraxFirmwareDao : JpaRepository<NuotraxFirmware, Long>
