package fr.velco.otau.stream.adapter

import fr.velco.back.framework.byte.HexStringUtils
import fr.velco.back.framework.logging.VelcoLogger
import fr.velco.otau.services.port.MqttPort
import jakarta.annotation.PostConstruct
import org.apache.commons.lang3.RandomStringUtils
import org.eclipse.paho.client.mqttv3.IMqttClient
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.amqp.RabbitProperties
import org.springframework.stereotype.Service

@Service
class MqttAdapter(
    private val rabbitProperties: RabbitProperties, //Useful to read the whole spring.rabbitmq section in yml configuration file
) : MqttPort {
    private val log = VelcoLogger {}

    private lateinit var mqttClient: IMqttClient

    @Value("\${spring.application.name}")
    private val clientId: String = "iot-otau-worker"

    @Value("\${velco.maxInflight}")
    private var maxInflight: Int = 100

    @PostConstruct //@Value doesn't work with init {}
    private fun connection() {
        this.mqttClient = MqttClient(
            "tcp://${rabbitProperties.host}:1883",
            "${clientId}_${RandomStringUtils.randomAlphanumeric(10)}",
            MemoryPersistence()
        )

        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.userName = "n:${rabbitProperties.username}" //virtual host: n
        mqttConnectOptions.password = rabbitProperties.password.toCharArray()
        mqttConnectOptions.isCleanSession = true
        mqttConnectOptions.maxInflight = maxInflight
        mqttConnectOptions.isAutomaticReconnect = true

        mqttClient.connect(mqttConnectOptions)
        log.info("MqttAdapter connected. (maxInflight=$maxInflight)")
    }

    override fun send(topic: String, payload: ByteArray) {
        log.debug("MqttService.send(topic: $topic, payload: ${HexStringUtils.toHexString(payload)})")

        val message = MqttMessage()
        message.payload = payload
        message.qos = 1
        message.isRetained = false
        mqttClient.publish(topic, message)
    }
}
