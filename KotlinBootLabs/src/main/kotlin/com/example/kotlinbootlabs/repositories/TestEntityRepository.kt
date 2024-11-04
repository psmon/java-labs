package com.example.kotlinbootlabs.repositories

import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface TestEntityRepository : ReactiveMongoRepository<TestEntity, String>