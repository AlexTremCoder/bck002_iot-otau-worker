package fr.velco.otau.services

import org.mockito.ArgumentCaptor
import org.mockito.Mockito

abstract class KotlinMockitoHelper: DataSet() {
    fun <T> eq(obj: T): T = Mockito.eq<T>(obj)

    fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

    fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
}
