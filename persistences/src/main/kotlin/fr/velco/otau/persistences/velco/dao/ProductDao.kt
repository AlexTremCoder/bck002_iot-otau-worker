package fr.velco.otau.persistences.velco.dao

import fr.velco.otau.persistences.velco.table.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductDao : JpaRepository<Product, Long> {
    fun findFirstBySerialNumber(serialNumber: String): Product?
}
