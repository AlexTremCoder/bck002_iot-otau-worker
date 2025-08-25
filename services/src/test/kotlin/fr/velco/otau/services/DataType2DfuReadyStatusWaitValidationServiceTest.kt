package fr.velco.otau.services

import fr.velco.otau.services.config.Properties
import fr.velco.otau.persistences.velco.dao.ProductDao
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.service.*
import fr.velco.otau.services.service.cache.FirmwareCacheService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class DataType2DfuReadyStatusWaitValidationServiceTest : KotlinMockitoHelper() {
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
    fun `threat() WAIT_VALIDATION #0 should send 'Update Validation-DFU Validation'`() {
        //Arrange
        val payload = byteArrayOf(
            0x02, //DFU Packet Data ID
            0x00, //Last Packet Number ID
            0x00,
            0x04, //Process step WAIT_VALIDATION
        )

        //Act
        dataType2DfuPacketDataIdService.treat(getProductDto(), payload)

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(1)).sendDfuCancelIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(1)).sendDfuCancelIfNoTracking(any(ProductDto::class.java), anyMap())
        Mockito.verify(dfuDataTopicService, Mockito.times(1)).sendUpdateValidation(any(ProductDto::class.java), eq(true), anyMap())
    }
}
