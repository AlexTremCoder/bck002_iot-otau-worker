package fr.velco.otau.services

import fr.velco.otau.services.config.Properties
import fr.velco.otau.services.service.DataTypeService
import fr.velco.otau.services.service.DfuDataTopicService
import fr.velco.otau.services.service.cache.FirmwareCacheService
import fr.velco.otau.services.service.OtauTrackingService
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class DataTypeServiceTest : KotlinMockitoHelper() {
    @InjectMocks
    private lateinit var dataTypeService: DataTypeService

    @Mock
    private lateinit var properties: Properties

    @Mock
    private lateinit var firmwareCacheService: FirmwareCacheService

    @Mock
    private lateinit var dfuDataTopicService: DfuDataTopicService

    @Mock
    private lateinit var otauTrackingService: OtauTrackingService

    @Test
    fun `For IoT not eligible isEligibleToATargetVersion() should return false`() {
        //Act
        val result = dataTypeService.isEligibleToATargetVersion(getProductDto(), mutableMapOf())

        //Assert
        assertFalse(result)
    }

    @Test
    fun `For IoT eligible isEligibleToATargetVersion() should return true`() {
        //Arrange
        val product = getProductDto(idFirmware = 1)

        //Act
        val result = dataTypeService.isEligibleToATargetVersion(product, mutableMapOf())

        //Assert
        assertTrue(result)
    }
}
