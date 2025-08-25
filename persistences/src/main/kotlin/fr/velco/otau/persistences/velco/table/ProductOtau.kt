package fr.velco.otau.persistences.velco.table

import java.time.LocalDateTime
import jakarta.persistence.*

@Entity
@Table(name = "product_otau")
class ProductOtau(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_product_otau", nullable = false)
    val id: Long = 0,

    @Column(name = "iot_fw_update_date")
    var iotFwUpdateDate: LocalDateTime? = null,

    @OneToOne
    @JoinColumn(name = "id_product", nullable = false)
    var product: Product,
)
