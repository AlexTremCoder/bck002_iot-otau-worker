package fr.velco.otau.services.dto

import org.json.JSONArray

class FirmwareDto(
    val version: String,
    val jsonArray: JSONArray,
    val totalPackets: Int,
)
