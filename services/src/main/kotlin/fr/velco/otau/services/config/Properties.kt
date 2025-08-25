package fr.velco.otau.services.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "velco")
class Properties(
    var maxSendPacketAttempts: Int = 5,
    var minBatteryLvlOtau: Int = 50,
    var activeDelayInMinutes: Long = 5,
    var obsoleteDelayInDays: Long = 30,
    var maxSimultaneousOtau: Int = 50,
    var logModulo: Int = 100,
)
