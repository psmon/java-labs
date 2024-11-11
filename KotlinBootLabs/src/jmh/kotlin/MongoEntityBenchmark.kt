package com.example.kotlinbootlabs.repositories

import org.junit.jupiter.api.extension.ExtendWith
import org.openjdk.jmh.annotations.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.concurrent.TimeUnit


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@DataMongoTest
@SpringBootTest
@ExtendWith(SpringExtension::class)
@TestPropertySource(locations = ["classpath:application.properties"])
open class MongoEntityBenchmark {

    @Autowired
    lateinit var repository: TestEntityRepository

    @Setup(Level.Trial)
    fun setup() {
        // Repository 가져오기
        //repository = context.getBean(TestEntityRepository::class.java)

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
        val testCount = 100
        val readMonos = (1..testCount).map {
            repository.findByName("test$it").next()
        }
        val combinedFlux = Flux.merge(readMonos)
        StepVerifier.create(combinedFlux.collectList())
            .expectNextMatches { it.size == testCount }
            .verifyComplete()
    }
}
