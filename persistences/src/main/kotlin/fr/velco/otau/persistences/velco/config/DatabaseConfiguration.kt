package fr.velco.otau.persistences.velco.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EntityScan(
    basePackages = [
        "fr.velco.otau.persistences.velco.table",
    ]
)
@EnableJpaRepositories(
    basePackages = ["fr.velco.otau.persistences.velco.dao"],
)
class DatabaseConfiguration
