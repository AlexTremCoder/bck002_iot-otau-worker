package fr.velco.otau.services

import fr.velco.otau.persistences.velco.dao.ProductDao
import fr.velco.otau.services.config.Properties
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.service.*
import fr.velco.otau.services.service.cache.FirmwareCacheService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class DataType2DfuReadyStatusDfuRxServiceTest : KotlinMockitoHelper() {
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
    fun `threat() DFU_RX #packet already acked should send 'End of transmission'`() {
        //Arrange
        val payload = byteArrayOf(
            0x02, //DFU Packet Data ID
            0x00, //Last Packet Number ID #1
            0x01,
            0x02, //Process step DFU_RX
        )
        val productDto = getProductDto()
        Mockito.`when`(otauTrackingService.isOtauSlotAvailable(anyMap())).thenReturn(true)
        Mockito.`when`(dataTypeService.getOtauTrackingOrSendEndOfTransmission(any(ProductDto::class.java), anyMap())).thenReturn(getOtauTracking())

        //Act
        dataType2DfuPacketDataIdService.treat(productDto, payload)

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(1)).sendEndOfTransmissionIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
        Mockito.verify(dfuDataTopicService, Mockito.times(1)).sendEndOfTransmission(any(ProductDto::class.java), anyMap())
        Mockito.verify(otauTrackingService, Mockito.times(0)).setLastPacketAcked(any(ProductDto::class.java), anyInt(), anyMap())
    }

    @Test
    fun `threat() DFU_RX #packet sent not received should send again the packet`() {
        //Arrange
        val payload = byteArrayOf(
            0x02, //DFU Packet Data ID
            0x00, //Last Packet Number ID #2
            0x02,
            0x02, //Process step DFU_RX
        )
        val productDto = getProductDto()
        Mockito.`when`(otauTrackingService.isOtauSlotAvailable(anyMap())).thenReturn(true)
        Mockito.`when`(dataTypeService.getOtauTrackingOrSendEndOfTransmission(any(ProductDto::class.java), anyMap())).thenReturn(getOtauTracking())
        Mockito.`when`(dataTypeService.isLastPacket(any(ProductDto::class.java), eq(2), anyMap())).thenReturn(false)

        //Act
        dataType2DfuPacketDataIdService.treat(productDto, payload)

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(1)).sendEndOfTransmissionIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
        Mockito.verify(dfuDataTopicService, Mockito.times(0)).sendEndOfTransmission(any(ProductDto::class.java), anyMap())
        Mockito.verify(otauTrackingService, Mockito.times(1)).setLastPacketAcked(any(ProductDto::class.java), eq(2), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(1)).sendPacket(any(ProductDto::class.java), eq(3), anyMap(), eq(true))
    }

    @Test
    fun `threat() DFU_RX #packet not ack should re-sync`() {
        //Arrange
        val payload = byteArrayOf(
            0x02, //DFU Packet Data ID
            0x00, //Last Packet Number ID #3
            0x03,
            0x02, //Process step DFU_RX
        )
        val productDto = getProductDto()
        Mockito.`when`(otauTrackingService.isOtauSlotAvailable(anyMap())).thenReturn(true)
        Mockito.`when`(dataTypeService.getOtauTrackingOrSendEndOfTransmission(any(ProductDto::class.java), anyMap())).thenReturn(getOtauTracking())
        Mockito.`when`(dataTypeService.isLastPacket(any(ProductDto::class.java), eq(3), anyMap())).thenReturn(false)

        //Act
        dataType2DfuPacketDataIdService.treat(productDto, payload)

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(1)).sendEndOfTransmissionIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
        Mockito.verify(dfuDataTopicService, Mockito.times(0)).sendEndOfTransmission(any(ProductDto::class.java), anyMap())
        Mockito.verify(otauTrackingService, Mockito.times(1)).setLastPacketAcked(any(ProductDto::class.java), eq(3), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(1)).sendPacket(any(ProductDto::class.java), eq(4), anyMap(), eq(true))
    }

    @Test
    fun `threat() DFU_RX #last packet should send 'End of transmission'`() {
        //Arrange
        val payload = byteArrayOf(
            0x02, //DFU Packet Data ID
            0x00, //Last Packet Number ID #2
            0x02,
            0x02, //Process step DFU_RX
        )
        val productDto = getProductDto()
        Mockito.`when`(otauTrackingService.isOtauSlotAvailable(anyMap())).thenReturn(true)
        Mockito.`when`(dataTypeService.getOtauTrackingOrSendEndOfTransmission(any(ProductDto::class.java), anyMap())).thenReturn(getOtauTracking())
        Mockito.`when`(dataTypeService.isLastPacket(any(ProductDto::class.java), eq(2), anyMap())).thenReturn(true)

        //Act
        dataType2DfuPacketDataIdService.treat(productDto, payload)

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(1)).sendEndOfTransmissionIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
        Mockito.verify(otauTrackingService, Mockito.times(1)).setLastPacketAcked(any(ProductDto::class.java), eq(2), anyMap())
        Mockito.verify(dfuDataTopicService, Mockito.times(1)).sendEndOfTransmission(any(ProductDto::class.java), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(0)).sendPacket(any(ProductDto::class.java), anyInt(), anyMap(), eq(true))
    }

    @Test
    fun `threat() DFU_RX without slot should not continue`() {
        //Arrange
        val payload = byteArrayOf(
            0x02, //DFU Packet Data ID
            0x00, //Last Packet Number ID #2
            0x02,
            0x02, //Process step DFU_RX
        )
        val productDto = getProductDto()
        Mockito.`when`(otauTrackingService.isOtauSlotAvailable(anyMap())).thenReturn(false)

        //Act
        dataType2DfuPacketDataIdService.treat(productDto, payload)

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(1)).sendEndOfTransmissionIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(0)).getOtauTrackingOrSendEndOfTransmission(any(ProductDto::class.java), anyMap())
        Mockito.verify(otauTrackingService, Mockito.times(1)).isOtauSlotAvailable(anyMap())
        Mockito.verify(otauTrackingService, Mockito.times(0)).setLastPacketAcked(any(ProductDto::class.java), anyInt(), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(0)).sendPacket(any(ProductDto::class.java), anyInt(), anyMap(), anyBoolean())
    }
}
