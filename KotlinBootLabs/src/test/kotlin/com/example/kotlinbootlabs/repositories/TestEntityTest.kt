package com.example.kotlinbootlabs.repositories

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@DataMongoTest
@ExtendWith(SpringExtension::class)
class TestEntityTest {


    @Autowired
    lateinit var repository: TestEntityRepository


    @Test
    fun testSave() {
        val entity = TestEntity(name = "test", value = 1)
        val saveMono: Mono<TestEntity> = repository.save(entity)

        StepVerifier.create(saveMono)
            .expectNextMatches {
                it.id != null && it.name == "test" && it.value == 1
            }
            .verifyComplete()
    }

    @Test
    fun testSaveMultipleTimes() {
        val startTime = System.currentTimeMillis()
        val testCount = 10000

        val saveMonos = (1..testCount).map {
            val entity = TestEntity(name = "test$it", value = it)
            repository.save(entity)
        }

        val combinedMono = Mono.zip(saveMonos) { it.toList() }

        StepVerifier.create(combinedMono)
            .expectNextMatches { it.size == testCount }
            .verifyComplete()

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val averageTime = totalTime / testCount.toFloat()

        println("Total time: $totalTime ms Total saves: $testCount")
        println("Average time per save: $averageTime ms")
    }


}