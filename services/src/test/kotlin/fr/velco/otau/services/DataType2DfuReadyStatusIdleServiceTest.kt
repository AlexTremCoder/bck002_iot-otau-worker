package fr.velco.otau.services

import fr.velco.otau.persistences.velco.dao.ProductDao
import fr.velco.otau.persistences.velco.table.Product
import fr.velco.otau.services.config.Properties
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.service.*
import fr.velco.otau.services.service.cache.FirmwareCacheService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class DataType2DfuReadyStatusIdleServiceTest : KotlinMockitoHelper() {
    @InjectMocks
    private lateinit var dataType2DfuPacketDataIdService: DataType2DfuPacketDataIdService

    @Mock
    private lateinit var properties: Properties

    @Mock
    private lateinit var productDao: ProductDao

    @Mock
    private lateinit var dataTypeService: DataTypeService

    @Mock
    private lateinit var firmwareCacheService: FirmwareCacheService

    @Mock
    private lateinit var dfuDataTopicService: DfuDataTopicService

    @Mock
    private lateinit var otauTrackingService: OtauTrackingService

    @Test
    fun `threat() with invalid payload should do nothing`() {
        //Act
        dataType2DfuPacketDataIdService.treat(getProductDto(), byteArrayOf())

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(0)).sendEndOfTransmissionIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(0)).sendDfuCancelIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
    }

    @Test
    fun `threat() IDLE #0 should send 'Start of Transmission'`() {
        //Arrange
        val payload = byteArrayOf(
            0x02, //DFU Packet Data ID
            0x00, //Last Packet Number ID
            0x00,
            0x00, //Process step IDLE
        )
        val productDto = getProductDto(idFirmware = 1)
        Mockito.`when`(firmwareCacheService.getFirmware(productDto.idFirmware ?: 0)).thenReturn(getFirmware())
        Mockito.`when`(productDao.getReferenceById(0)).thenReturn(getProduct(idFirmware = 1, batteryLevel = 75))
        Mockito.`when`(otauTrackingService.isOtauSlotAvailable(anyMap())).thenReturn(true)
        Mockito.`when`(dataTypeService.isBatteryLevelSufficient(any(Short::class.javaObjectType), anyMap())).thenReturn(true)

        //Act
        dataType2DfuPacketDataIdService.treat(productDto, payload)

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(1)).sendEndOfTransmissionIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
        Mockito.verify(dfuDataTopicService, Mockito.times(1)).sendStartOfTransmission(any(ProductDto::class.java), eq(65535), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(1)).sendPacket(any(ProductDto::class.java), eq(1), anyMap(), eq(true))
        Mockito.verify(otauTrackingService, Mockito.times(1)).start(any(Product::class.java), anyMap())
        Mockito.verify(otauTrackingService, Mockito.times(1)).isOtauSlotAvailable(anyMap())
    }

    @Test
    fun `threat() IDLE #0 without enough battery should not send 'Start of Transmission'`() {
        //Arrange
        val payload = byteArrayOf(
            0x02, //DFU Packet Data ID
            0x00, //Last Packet Number ID
            0x00,
            0x00, //Process step IDLE
        )
        Mockito.`when`(productDao.getReferenceById(0)).thenReturn(getProduct(batteryLevel = 10))
        val product = getProductDto()
        Mockito.`when`(dataTypeService.isBatteryLevelSufficient(any(Short::class.javaObjectType), anyMap())).thenReturn(false)

        //Act
        dataType2DfuPacketDataIdService.treat(product, payload)

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(1)).sendEndOfTransmissionIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
        Mockito.verify(firmwareCacheService, Mockito.times(0)).getFirmware(anyLong())
        Mockito.verify(dfuDataTopicService, Mockito.times(0)).sendStartOfTransmission(any(ProductDto::class.java), eq(65535), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(0)).sendPacket(any(ProductDto::class.java), eq(1), anyMap(), eq(true))
        Mockito.verify(otauTrackingService, Mockito.times(0)).start(any(Product::class.java), anyMap())
    }

    @Test
    fun `threat() IDLE #0 without slot should not send 'Start of Transmission'`() {
        //Arrange
        val payload = byteArrayOf(
            0x02,
            0x00,
            0x00,
            0x00,
        )
        val productDto = getProductDto(idFirmware = 1)
        Mockito.`when`(firmwareCacheService.getFirmware(productDto.idFirmware ?: 0)).thenReturn(getFirmware())
        Mockito.`when`(productDao.getReferenceById(0)).thenReturn(getProduct(idFirmware = 1, batteryLevel = 75))
        Mockito.`when`(otauTrackingService.isOtauSlotAvailable(anyMap())).thenReturn(false)
        Mockito.`when`(dataTypeService.isBatteryLevelSufficient(any(Short::class.javaObjectType), anyMap())).thenReturn(true)

        //Act
        dataType2DfuPacketDataIdService.treat(productDto, payload)

        //Assert
        Mockito.verify(dfuDataTopicService, Mockito.times(0)).sendStartOfTransmission(any(ProductDto::class.java), eq(65535), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(0)).sendPacket(any(ProductDto::class.java), eq(1), anyMap(), eq(true))
        Mockito.verify(otauTrackingService, Mockito.times(0)).start(any(Product::class.java), anyMap())
    }

    @Test
    fun `threat() IDLE #1 should not send 'Start of Transmission'`() {
        //Arrange
        val payload = byteArrayOf(
            0x02, //DFU Packet Data ID
            0x00, //Last Packet Number ID
            0x01,
            0x00, //Process step IDLE
        )

        //Act
        dataType2DfuPacketDataIdService.treat(getProductDto(), payload)

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(1)).sendEndOfTransmissionIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
        Mockito.verify(firmwareCacheService, Mockito.times(0)).getFirmware(anyLong())
        Mockito.verify(dfuDataTopicService, Mockito.times(0)).sendStartOfTransmission(any(ProductDto::class.java), eq(65535), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(0)).sendPacket(any(ProductDto::class.java), eq(1), anyMap(), eq(true))
        Mockito.verify(otauTrackingService, Mockito.times(0)).start(any(Product::class.java), anyMap())
    }
}
