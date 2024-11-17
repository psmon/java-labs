package com.example.kotlinbootlabs.service

import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class RedisService(private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>) {

    fun setValue(category: String, key: String, value: String): Mono<Boolean> {
        val compositeKey = "$category:$key"
        return reactiveRedisTemplate.opsForValue().set(compositeKey, value)
    }

    fun getValue(category: String, key: String): Mono<String?> {
        val compositeKey = "$category:$key"
        return reactiveRedisTemplate.opsForValue().get(compositeKey)
    }
}