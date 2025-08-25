package fr.velco.otau.services

import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.port.MqttPort
import fr.velco.otau.services.service.DfuDataTopicService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class DfuDataTopicServiceTest : KotlinMockitoHelper() {
    @InjectMocks
    private lateinit var dfuDataTopicService: DfuDataTopicService

    @Mock
    private lateinit var mqttPort: MqttPort

    @Test
    fun `sendAskNuotraxVersion() should work`() {
        //Arrange
        val dfuDataTopicServiceSpy = spy(this.dfuDataTopicService)

        //Act
        dfuDataTopicServiceSpy.sendAskNuotraxVersion(getProductDto(), mutableMapOf())

        //Assert
        val payload: ArgumentCaptor<ByteArray> = ArgumentCaptor.forClass(ByteArray::class.java)
        verify(dfuDataTopicServiceSpy, times(1)).cryptAndSendPayload(any(ProductDto::class.java), capture(payload), anyMap())

        val byteArray: ByteArray = payload.value
        assertEquals(1, byteArray.size)
        assertEquals(0x00, byteArray[0]) //Ask for Nuotrax Version
    }

    @Test
    fun `sendStartOfTransmission() should work`() {
        //Arrange
        val dfuDataTopicServiceSpy = spy(this.dfuDataTopicService)

        //Act
        dfuDataTopicServiceSpy.sendStartOfTransmission(getProductDto(), 258, mutableMapOf())

        //Assert
        val payload: ArgumentCaptor<ByteArray> = ArgumentCaptor.forClass(ByteArray::class.java)
        verify(dfuDataTopicServiceSpy, times(1)).cryptAndSendPayload(any(ProductDto::class.java), capture(payload), anyMap())

        val byteArray: ByteArray = payload.value
        assertEquals(4, byteArray.size)
        assertEquals(0x01, byteArray[0]) //Start of Transmission
        assertEquals(0x01, byteArray[1]) //The number of lines to transmit
        assertEquals(0x02, byteArray[2])
        assertEquals(0x00, byteArray[3]) //Mode (mqtt or http)
    }

    @Test
    fun `sendEndOfTransmission() should work`() {
        //Arrange
        val dfuDataTopicServiceSpy = spy(this.dfuDataTopicService)

        //Act
        dfuDataTopicServiceSpy.sendEndOfTransmission(getProductDto(), mutableMapOf())

        //Assert
        val payload: ArgumentCaptor<ByteArray> = ArgumentCaptor.forClass(ByteArray::class.java)
        verify(dfuDataTopicServiceSpy, times(1)).cryptAndSendPayload(any(ProductDto::class.java), capture(payload), anyMap())

        val byteArray: ByteArray = payload.value
        assertEquals(1, byteArray.size)
        assertEquals(0x03, byteArray[0]) //End of Transmission
    }

    @Test
    fun `sendPacket() should work`() {
        //Arrange
        val dfuDataTopicServiceSpy = spy(this.dfuDataTopicService)
        val packetb64 = "CABgACAAGiAIAaZ1CAGerQgBnq8AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAGetwAAAAAAAAAACAGeuQgBnrtYCjoc" //72 bytes

        //Act
        dfuDataTopicServiceSpy.sendPacket(getProductDto(), 258, packetb64, mutableMapOf())

        //Assert
        val payload: ArgumentCaptor<ByteArray> = ArgumentCaptor.forClass(ByteArray::class.java)
        verify(dfuDataTopicServiceSpy, times(1)).cryptAndSendPayload(any(ProductDto::class.java), capture(payload), anyMap())

        val byteArray: ByteArray = payload.value
        assertEquals(75, byteArray.size)
        assertEquals(0x02, byteArray[0]) //Packet Transmission
        assertEquals(0x01, byteArray[1]) //Packet number
        assertEquals(0x02, byteArray[2])
        assertEquals(0x08, byteArray[3]) //First byte of packet (Include address)
        assertEquals(0x1C, byteArray[74]) //Last byte of packet (Include checksum)
    }

    @Test
    fun `sendUpdateValidation(false) should work`() {
        //Arrange
        val dfuDataTopicServiceSpy = spy(this.dfuDataTopicService)

        //Act
        dfuDataTopicServiceSpy.sendUpdateValidation(getProductDto(), false, mutableMapOf())

        //Assert
        val payload: ArgumentCaptor<ByteArray> = ArgumentCaptor.forClass(ByteArray::class.java)
        verify(dfuDataTopicServiceSpy, times(1)).cryptAndSendPayload(any(ProductDto::class.java), capture(payload), anyMap())

        val byteArray: ByteArray = payload.value
        assertEquals(4, byteArray.size)
        assertEquals(0x05, byteArray[0]) //Update Validation
        assertEquals(0x02, byteArray[1]) //02: DFU Cancel
    }

    @Test
    fun `sendUpdateValidation(true) should work`() {
        //Arrange
        val dfuDataTopicServiceSpy = spy(this.dfuDataTopicService)

        //Act
        dfuDataTopicServiceSpy.sendUpdateValidation(getProductDto(), true, mutableMapOf())

        //Assert
        val payload: ArgumentCaptor<ByteArray> = ArgumentCaptor.forClass(ByteArray::class.java)
        verify(dfuDataTopicServiceSpy, times(1)).cryptAndSendPayload(any(ProductDto::class.java), capture(payload), anyMap())

        val byteArray: ByteArray = payload.value
        assertEquals(4, byteArray.size)
        assertEquals(0x05, byteArray[0]) //Update Validation
        assertEquals(0x01, byteArray[1]) //01: DFU Validation
    }

    @Test
    fun `sendAskForLastPacketId() should work`() {
        //Arrange
        val dfuDataTopicServiceSpy = spy(this.dfuDataTopicService)

        //Act
        dfuDataTopicServiceSpy.sendAskForLastPacketId(getProductDto(), mutableMapOf())

        //Assert
        val payload: ArgumentCaptor<ByteArray> = ArgumentCaptor.forClass(ByteArray::class.java)
        verify(dfuDataTopicServiceSpy, times(1)).cryptAndSendPayload(any(ProductDto::class.java), capture(payload), anyMap())

        val byteArray: ByteArray = payload.value
        assertEquals(1, byteArray.size)
        assertEquals(0x04, byteArray[0]) //Ask for Last Packet Transmit
    }
}
