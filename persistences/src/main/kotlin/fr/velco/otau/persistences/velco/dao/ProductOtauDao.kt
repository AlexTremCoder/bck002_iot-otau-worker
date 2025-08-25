package fr.velco.otau.persistences.velco.dao

import fr.velco.otau.persistences.velco.table.ProductOtau
import org.springframework.data.jpa.repository.JpaRepository

interface ProductOtauDao : JpaRepository<ProductOtau, Long> {
    fun findFirstByProductId(productId: Long): ProductOtau?
}
