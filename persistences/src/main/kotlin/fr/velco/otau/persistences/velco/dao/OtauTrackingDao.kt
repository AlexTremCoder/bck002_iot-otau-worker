package fr.velco.otau.persistences.velco.dao

import fr.velco.otau.persistences.velco.table.OtauTracking
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface OtauTrackingDao : JpaRepository<OtauTracking, Long> {
    fun findFirstByIdProduct(idProduct: Long): OtauTracking?

    fun countByLastUpdateAfter(lastUpdate: LocalDateTime): Int

    fun findByLastUpdateBefore(lastUpdate: LocalDateTime): List<OtauTracking>
}
