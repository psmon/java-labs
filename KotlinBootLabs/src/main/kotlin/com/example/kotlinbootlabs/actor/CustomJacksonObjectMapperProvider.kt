package com.example.kotlinbootlabs.actor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.pekko.serialization.JSerializer
import org.apache.pekko.serialization.Serializer
import org.apache.pekko.serialization.jackson.JacksonObjectMapperFactory
import org.apache.pekko.serialization.jackson.JacksonObjectMapperProvider
import java.nio.charset.StandardCharsets


abstract class CustomJacksonSerializer : JSerializer() {
    // Jackson ObjectMapper에 Kotlin 모듈 등록
    private val mapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
    }

}

abstract class CustomJacksonSerializer2 : Serializer {

    private val mapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
    }

    // "includeManifest"가 "fromBinary"에 "clazz"를 요구하는지 여부
    override fun includeManifest(): Boolean {
        return false
    }

    // 고유한 식별자 선택 (0 - 40은 Pekko 자체에서 예약)
    override fun identifier(): Int {
        return 1234567
    }

    // "toBinary"는 주어진 객체를 바이트 배열로 직렬화
    override fun toBinary(obj: Any): ByteArray {
        // 객체를 직렬화하는 코드를 여기에 작성합니다.
        // #...
        return ByteArray(0)
        // #...
    }

    // "fromBinary"는 주어진 배열을 역직렬화
    // 위에서 "includeManifest"가 true일 경우 type hint로 사용될 수 있습니다.
    override fun fromBinary(bytes: ByteArray, clazz: Class<*>?): Any? {
        // 객체를 역직렬화하는 코드를 여기에 작성합니다.
        // #...
        return null
        // #...
    }
}

