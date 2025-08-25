package fr.velco.otau.services.port

interface MqttPort {
    fun send(topic: String, payload: ByteArray)
}
