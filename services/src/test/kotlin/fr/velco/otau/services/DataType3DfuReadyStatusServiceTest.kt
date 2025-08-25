package fr.velco.otau.services

import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.enums.FailureReasonEnum
import fr.velco.otau.services.service.DataType3DfuReadyStatusService
import fr.velco.otau.services.service.DataTypeService
import fr.velco.otau.services.service.DfuDataTopicService
import fr.velco.otau.services.service.OtauTrackingService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class DataType3DfuReadyStatusServiceTest : KotlinMockitoHelper() {
    @InjectMocks
    private lateinit var dataType3DfuReadyStatusService: DataType3DfuReadyStatusService

    @Mock
    private lateinit var dfuDataTopicService: DfuDataTopicService

    @Mock
    private lateinit var dataTypeService: DataTypeService

    @Mock
    private lateinit var otauTrackingService: OtauTrackingService

    @Test
    fun `threat() with invalid payload should do nothing`() {
        //Act
        dataType3DfuReadyStatusService.treat(getProductDto(), byteArrayOf())

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(1)).sendDfuCancelIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(1)).sendDfuCancelIfNoTracking(any(ProductDto::class.java), anyMap())
        Mockito.verify(dfuDataTopicService, Mockito.times(0)).sendUpdateValidation(any(ProductDto::class.java), anyBoolean(), anyMap())
        Mockito.verify(otauTrackingService, Mockito.times(0)).stop(any(ProductDto::class.java), anyMap(), eq(null))
    }

    @Test
    fun `threat() with CRC incoherency should stop tracking`() {
        //Arrange
        val payload = byteArrayOf(
            0x03, //DFU Ready status
            0x01, //CRC Incoherency
        )

        //Act
        dataType3DfuReadyStatusService.treat(getProductDto(), payload)

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(1)).sendDfuCancelIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(1)).sendDfuCancelIfNoTracking(any(ProductDto::class.java), anyMap())
        Mockito.verify(dfuDataTopicService, Mockito.times(0)).sendUpdateValidation(any(ProductDto::class.java), anyBoolean(), anyMap())
        Mockito.verify(otauTrackingService, Mockito.times(1)).stop(any(ProductDto::class.java), anyMap(), eq(FailureReasonEnum.CRC_INCOHERENCY))
    }

    @Test
    fun `threat() with no error should send 'Update Validation-DFU Validation'`() {
        //Arrange
        val payload = byteArrayOf(
            0x03, //DFU Ready status
            0x00, //No error
        )

        //Act
        dataType3DfuReadyStatusService.treat(getProductDto(), payload)

        //Assert
        Mockito.verify(dataTypeService, Mockito.times(1)).sendDfuCancelIfNotEligibleToATargetVersion(any(ProductDto::class.java), anyMap())
        Mockito.verify(dataTypeService, Mockito.times(1)).sendDfuCancelIfNoTracking(any(ProductDto::class.java), anyMap())
        Mockito.verify(dfuDataTopicService, Mockito.times(1)).sendUpdateValidation(any(ProductDto::class.java), eq(true), anyMap())
        Mockito.verify(otauTrackingService, Mockito.times(0)).stop(any(ProductDto::class.java), anyMap(), eq(null))
    }
}
