package com.example.kotlinbootlabs.repositories

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TestEntityRepository : ReactiveMongoRepository<TestEntity, String> {
    fun findByName(name: String): Flux<TestEntity>
}