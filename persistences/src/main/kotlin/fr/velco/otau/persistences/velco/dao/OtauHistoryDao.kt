package fr.velco.otau.persistences.velco.dao

import fr.velco.otau.persistences.velco.table.OtauHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OtauHistoryDao : JpaRepository<OtauHistory, Long>
