package com.example.kotlinbootlabs.repositories

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
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

    @Test
    fun testReadMultipleTimes() {
        val startTime = System.currentTimeMillis()
        val testCount = 10000

        val readMonos = (1..testCount).map {
            repository.findByName("test$it").next()
        }

        val combinedFlux = Flux.merge(readMonos)

        StepVerifier.create(combinedFlux.collectList())
            .expectNextMatches { it.size == testCount }
            .verifyComplete()

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val averageTime = totalTime / testCount.toFloat()

        println("Total time: $totalTime ms Total reads: $testCount")
        println("Average time per read: $averageTime ms")
    }

    @Test
    fun testSaveAndReadMultipleTimes() {
        val testCount = 10000

        // Measure save time
        val saveStartTime = System.currentTimeMillis()

        val saveMonos = (1..testCount).map {
            val entity = TestEntity(name = "test$it", value = it)
            repository.save(entity)
        }

        val combinedSaveMono = Mono.zip(saveMonos) { it.toList() }

        StepVerifier.create(combinedSaveMono)
            .expectNextMatches { it.size == testCount }
            .verifyComplete()

        val saveEndTime = System.currentTimeMillis()
        val saveTotalTime = saveEndTime - saveStartTime
        val saveAverageTime = saveTotalTime / testCount.toFloat()

        println("Save total time: $saveTotalTime ms Total saves: $testCount")
        println("Save average time per save: $saveAverageTime ms")

        // Measure read time
        val readStartTime = System.currentTimeMillis()

        val readMonos = saveMonos.map { saveMono ->
            saveMono.flatMap { savedEntity ->
                repository.findById(savedEntity.id)
            }
        }

        val combinedReadFlux = Flux.merge(readMonos)

        StepVerifier.create(combinedReadFlux.collectList())
            .expectNextMatches { it.size == testCount }
            .verifyComplete()

        val readEndTime = System.currentTimeMillis()
        val readTotalTime = readEndTime - readStartTime
        val readAverageTime = readTotalTime / testCount.toFloat()

        println("Read total time: $readTotalTime ms Total reads: $testCount")
        println("Read average time per read: $readAverageTime ms")
    }

}