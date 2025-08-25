package fr.velco.otau

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = ["fr.velco.otau","fr.velco.back.framework.amqp"],
)
class Application

fun main() {
    runApplication<Application>() // Start SpringBoot
}
