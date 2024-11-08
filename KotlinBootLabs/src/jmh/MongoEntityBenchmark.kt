package com.example.kotlinbootlabs.repositories

import org.openjdk.jmh.annotations.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Scope
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.util.concurrent.TimeUnit

@ExtendWith(SpringExtension::class)
@SpringBootTest
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class MongoEntityBenchmark {

    @Autowired
    lateinit var repository: TestEntityRepository

    @Setup(Level.Trial)
    fun setup() {
        // 초기 데이터 설정
        val testCount = 10000
        val saveMonos = (1..testCount).map {
            val entity = TestEntity(name = "test$it", value = it)
            repository.save(entity)
        }
        val combinedMono = Mono.zip(saveMonos) { it.toList() }
        StepVerifier.create(combinedMono).expectNextMatches { it.size == testCount }.verifyComplete()
    }

    @Benchmark
    fun testReadMultipleTimes() {
        val testCount = 10000
        val readMonos = (1..testCount).map {
            repository.findByName("test$it").next()
        }
        val combinedFlux = Flux.merge(readMonos)
        StepVerifier.create(combinedFlux.collectList())
            .expectNextMatches { it.size == testCount }
            .verifyComplete()
    }
}

fun main() {
    org.openjdk.jmh.Main.main(arrayOf())
}