package fr.velco.otau.services

import fr.velco.otau.services.enums.FailureReasonEnum
import fr.velco.otau.services.config.Properties
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.service.DataType1DfuDataAckNackService
import fr.velco.otau.services.service.DataTypeService
import fr.velco.otau.services.service.DfuDataTopicService
import fr.velco.otau.services.service.OtauTrackingService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class DataType1DfuDataAckNackServiceTest : KotlinMockitoHelper() {
    @InjectMocks
    private lateinit var dataType1DfuDataAckNackService: DataType1DfuDataAckNackService

    @Mock
    private lateinit var properties: Properties

    @Mock
    private lateinit var dataTypeService: DataTypeService

    @Mock
    private lateinit var dfuDataTopicService: DfuDataTopicService

    @Mock
    private lateinit var otauTrackingService: OtauTrackingService

    @Test
    fun `threat() with invalid payload should do nothing`() {
        //Act
        dataType1DfuDataAckNackService.treat(getProductDto(), byteArrayOf())

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(0)).sendEndOfTransmissionIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
    }

    @Test
    fun `threat() ack old packet should be ignore`() {
        //Arrange
        val payload = byteArrayOf(
            0x01, //DFU Data Ack / Nack
            0x00, //ack
            0x00, //Last Packet Number ID #1 (Invalid message because IoT has already ack the #2)
            0x01,
        )
        Mockito.`when`(dataTypeService.getOtauTrackingOrSendEndOfTransmission(any(ProductDto::class.java), anyMap())).thenReturn(getOtauTracking())

        //Act
        dataType1DfuDataAckNackService.treat(getProductDto(), payload)

        //Assert
        Mockito.verify(otauTrackingService, Mockito.times(0)).setLastPacketAcked(any(ProductDto::class.java), anyInt(), anyMap())
    }

    @Test
    fun `threat() ack packet already acked should be ignore`() {
        //Arrange
        val payload = byteArrayOf(
            0x01, //DFU Data Ack / Nack
            0x00, //ack
            0x00, //Last Packet Number ID #2 (Invalid message because IoT has already ack the #2)
            0x02,
        )
        Mockito.`when`(dataTypeService.getOtauTrackingOrSendEndOfTransmission(any(ProductDto::class.java), anyMap())).thenReturn(getOtauTracking())

        //Act
        dataType1DfuDataAckNackService.treat(getProductDto(), payload)

        //Assert
        Mockito.verify(otauTrackingService, Mockito.times(0)).setLastPacketAcked(any(ProductDto::class.java), anyInt(), anyMap())
    }

    @Test
    fun `threat() ack packet from the future should be ignore`() {
        //Arrange
        val payload = byteArrayOf(
            0x01, //DFU Data Ack / Nack
            0x00, //ack
            0x00, //Last Packet Number ID #4 (Invalid message because IoT has acked the #2, we are waiting for the #3, not the #4)
            0x04,
        )
        Mockito.`when`(dataTypeService.getOtauTrackingOrSendEndOfTransmission(any(ProductDto::class.java), anyMap())).thenReturn(getOtauTracking())

        //Act
        dataType1DfuDataAckNackService.treat(getProductDto(), payload)

        //Assert
        Mockito.verify(otauTrackingService, Mockito.times(0)).setLastPacketAcked(any(ProductDto::class.java), anyInt(), anyMap())
    }

    @Test
    fun `threat() ack nominal case`() {
        //Arrange
        val payload = byteArrayOf(
            0x01, //DFU Data Ack / Nack
            0x00, //ack
            0x00, //Last Packet Number ID #3 (IoT has acked the #2, we are waiting for this #3)
            0x03,
        )
        Mockito.`when`(dataTypeService.getOtauTrackingOrSendEndOfTransmission(any(ProductDto::class.java), anyMap())).thenReturn(getOtauTracking())
        Mockito.`when`(properties.logModulo).thenReturn(100)

        //Act
        dataType1DfuDataAckNackService.treat(getProductDto(), payload)

        //Assert
        Mockito.verify(otauTrackingService, Mockito.times(1)).setLastPacketAcked(any(ProductDto::class.java), eq(3), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(1)).isLastPacket(any(ProductDto::class.java), eq(3), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(1)).sendPacket(any(ProductDto::class.java), eq(4), anyMap(), eq(false))
    }

    @Test
    fun `threat() ack nominal case last packet should send 'End of transmission'`() {
        //Arrange
        val payload = byteArrayOf(
            0x01, //DFU Data Ack / Nack
            0x00, //ack
            0x00, //Last Packet Number ID #3
            0x03,
        )
        Mockito.`when`(dataTypeService.getOtauTrackingOrSendEndOfTransmission(any(ProductDto::class.java), anyMap())).thenReturn(getOtauTracking())
        Mockito.`when`(dataTypeService.isLastPacket(any(ProductDto::class.java), eq(3), anyMap())).thenReturn(true)
        Mockito.`when`(properties.logModulo).thenReturn(100)

        //Act
        dataType1DfuDataAckNackService.treat(getProductDto(), payload)

        //Assert
        Mockito.verify(otauTrackingService, Mockito.times(1)).setLastPacketAcked(any(ProductDto::class.java), eq(3), anyMap())
        Mockito.verify(dfuDataTopicService, Mockito.times(1)).sendEndOfTransmission(any(ProductDto::class.java), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(0)).sendPacket(any(ProductDto::class.java), anyInt(), anyMap(), anyBoolean())
    }

    @Test
    fun `threat() nack nominal case`() {
        //Arrange
        val payload = byteArrayOf(
            0x01, //DFU Data Ack / Nack
            0x01, //nack
            0x00, //Last Packet Number ID #3
            0x02,
        )
        Mockito.`when`(dataTypeService.getOtauTrackingOrSendEndOfTransmission(any(ProductDto::class.java), anyMap())).thenReturn(getOtauTracking())
        Mockito.`when`(otauTrackingService.incrementNack(any(ProductDto::class.java), anyMap())).thenReturn(1)
        Mockito.`when`(properties.maxSendPacketAttempts).thenReturn(5)

        //Act
        dataType1DfuDataAckNackService.treat(getProductDto(), payload)

        //Assert
        Mockito.verify(dfuDataTopicService, Mockito.times(0)).sendEndOfTransmission(any(ProductDto::class.java), anyMap())
    }

    @Test
    fun `threat() nack too many`() {
        //Arrange
        val payload = byteArrayOf(
            0x01, //DFU Data Ack / Nack
            0x01, //nack
            0x00, //Last Packet Number ID #3
            0x02,
        )
        Mockito.`when`(dataTypeService.getOtauTrackingOrSendEndOfTransmission(any(ProductDto::class.java), anyMap())).thenReturn(getOtauTracking())
        Mockito.`when`(otauTrackingService.incrementNack(any(ProductDto::class.java), anyMap())).thenReturn(5)

        //Act
        dataType1DfuDataAckNackService.treat(getProductDto(), payload)

        //Assert
        Mockito.verify(dfuDataTopicService, Mockito.times(1)).sendEndOfTransmission(any(ProductDto::class.java), anyMap())
        Mockito.verify(otauTrackingService, Mockito.times(1)).stop(any(ProductDto::class.java), anyMap(), eq(FailureReasonEnum.TOO_MANY_NACK))
    }
}
