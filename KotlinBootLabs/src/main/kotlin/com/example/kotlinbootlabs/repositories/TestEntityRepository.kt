package com.example.kotlinbootlabs.repositories

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface TestEntityRepository : ReactiveMongoRepository<TestEntity, String> {
    fun findByName(name: String): Flux<TestEntity>
}