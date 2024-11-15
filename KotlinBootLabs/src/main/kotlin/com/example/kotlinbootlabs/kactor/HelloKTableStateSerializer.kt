package com.example.kotlinbootlabs.kactor

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer

class HelloKTableStateSerializer : Serializer<HelloKTableState> {
    private val objectMapper = ObjectMapper()

    override fun serialize(topic: String?, data: HelloKTableState?): ByteArray? {
        return objectMapper.writeValueAsBytes(data)
    }
}

class HelloKTableStateDeserializer : Deserializer<HelloKTableState> {
    private val objectMapper = ObjectMapper()

    override fun deserialize(topic: String?, data: ByteArray?): HelloKTableState? {
        return objectMapper.readValue(data, HelloKTableState::class.java)
    }
}