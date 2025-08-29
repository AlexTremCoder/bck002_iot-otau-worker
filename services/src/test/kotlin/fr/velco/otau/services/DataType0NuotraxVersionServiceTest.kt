package fr.velco.otau.services

import fr.velco.otau.persistences.velco.dao.ProductDao
import fr.velco.otau.persistences.velco.table.Product
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.service.*
import fr.velco.otau.services.service.cache.FirmwareCacheService
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class DataType0NuotraxVersionServiceTest : KotlinMockitoHelper() {
    @InjectMocks
    private lateinit var dataType0NuotraxVersionService: DataType0NuotraxVersionService

    @Mock
    private lateinit var productDao: ProductDao

    @Mock
    private lateinit var firmwareCacheService: FirmwareCacheService

    @Mock
    private lateinit var dataTypeService: DataTypeService

    @Mock
    private lateinit var dfuDataTopicService: DfuDataTopicService

    @Mock
    private lateinit var otauTrackingService: OtauTrackingService

    @Mock
    private lateinit var productOtauService: ProductOtauService

    @Mock
    private lateinit var logbookService: LogbookService

    @Test
    fun `threat() with invalid payload should do nothing`() {
        //Act
        dataType0NuotraxVersionService.treat(getProductDto(), byteArrayOf())

        //Assert
        Mockito.verify(productDao, Mockito.times(0)).save(any(Product::class.java))
    }

    @Test
    fun `threat() with IoT not eligible should only update version in db`() {
        //Arrange
        val payload = byteArrayOf(
            0x00, //Nuotrax Version
            0x01, //Firmware Version
            0x02,
            0x03,
            0x04,
            0x04, //BootLoader Version
            0x03,
            0x02,
            0x01,
        )
        `when`(productDao.getReferenceById(0)).thenReturn(getProduct())

        //Act
        dataType0NuotraxVersionService.treat(getProductDto(), payload)

        //Assert
        val productCaptor: ArgumentCaptor<Product> = ArgumentCaptor.forClass(Product::class.java)
        Mockito.verify(productDao, Mockito.times(1)).save(capture(productCaptor))
        val product: Product = productCaptor.value
        assertEquals("1.2.3.4", product.nuotraxFirmwareVersion)
        assertEquals("4.3.2.1", product.bootloaderVersion)
        Mockito.verify(dfuDataTopicService, Mockito.times(0)).sendAskForLastPacketId(any(ProductDto::class.java), anyMap())
    }

    @Test
    fun `threat() with IoT eligible but up-to-date should not start an OTAU again`() {
        //Arrange
        val payload = byteArrayOf(
            0x00, //Nuotrax Version
            0x01, //Firmware Version
            0x02,
            0x03,
            0x04,
            0x04, //BootLoader Version
            0x03,
            0x02,
            0x01,
        )
        val productDtoBefore = getProductDto(idFirmware = 1)
        `when`(firmwareCacheService.getFirmware(productDtoBefore.idFirmware ?: 0)).thenReturn(getFirmware())
        `when`(dataTypeService.isEligibleToATargetVersion(any(ProductDto::class.java), anyMap())).thenReturn(true)
        `when`(productDao.getReferenceById(0)).thenReturn(getProduct(idFirmware = 1))

        //Act
        dataType0NuotraxVersionService.treat(productDtoBefore, payload)

        //Assert
        val productCaptor: ArgumentCaptor<Product> = ArgumentCaptor.forClass(Product::class.java)
        Mockito.verify(productDao, Mockito.times(2)).save(capture(productCaptor))

        Mockito.verify(logbookService, Mockito.times(1)).sendIotOtauPerformedEvent(any(ProductDto::class.java))
        Mockito.verify(productOtauService, Mockito.times(1)).updateProductOtau(any(Product::class.java))
        Mockito.verify(otauTrackingService, Mockito.times(1)).stop(any(ProductDto::class.java), anyMap(), eq(null))
        Mockito.verify(dfuDataTopicService, Mockito.times(0)).sendAskForLastPacketId(any(ProductDto::class.java), anyMap()) //principal assert of this test

        val productAfter: Product = productCaptor.value
        assertNull(productAfter.idNuotraxFirmwareAvailable)
        assertEquals("1.2.3.4", productAfter.nuotraxFirmwareVersion)
        assertEquals("4.3.2.1", productAfter.bootloaderVersion)
    }

    @Test
    fun `threat() with IoT eligible not up-to-date should start an OTAU`() {
        //Arrange
        val payload = byteArrayOf(
            0x00, //Nuotrax Version
            0x01, //Firmware Version
            0x02,
            0x03,
            0x03,
            0x04, //BootLoader Version
            0x03,
            0x02,
            0x01,
        )
        val productDtoBefore = getProductDto(idFirmware = 1)
        `when`(firmwareCacheService.getFirmware(productDtoBefore.idFirmware ?: 0)).thenReturn(getFirmware())
        `when`(dataTypeService.isEligibleToATargetVersion(any(ProductDto::class.java), anyMap())).thenReturn(true)
        `when`(productDao.getReferenceById(0)).thenReturn(getProduct(idFirmware = 1))
        `when`(otauTrackingService.isOtauSlotAvailableAfterCleanUp(anyMap())).thenReturn(true)

        //Act
        dataType0NuotraxVersionService.treat(productDtoBefore, payload)

        //Assert
        val productCaptor: ArgumentCaptor<Product> = ArgumentCaptor.forClass(Product::class.java)
        Mockito.verify(productDao, Mockito.times(1)).save(capture(productCaptor))
        Mockito.verify(dfuDataTopicService, Mockito.times(1)).sendAskForLastPacketId(any(ProductDto::class.java), anyMap()) //principal assert of this test

        val productAfter: Product = productCaptor.value
        assertEquals("1.2.3.3", productAfter.nuotraxFirmwareVersion)
        assertEquals("4.3.2.1", productAfter.bootloaderVersion)
    }

    @Test
    fun `Version with a number above 127 should work`() {
        val payload = byteArrayOf(0x01, 0x07, 0x05, 0xC8.toByte())
        val result = dataType0NuotraxVersionService.parseVersion(payload)
        assertEquals("1.7.5.200", result)
    }
}
