package com.example.kotlinbootlabs.repositories

import org.openjdk.jmh.annotations.*
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.util.concurrent.TimeUnit
import reactor.core.publisher.Mono


@DataMongoTest
@ExtendWith(SpringExtension::class)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class MongoEntityBenchmark {

    lateinit var repository: TestEntityRepository
    lateinit var context: AnnotationConfigApplicationContext

    @Setup(Level.Trial)
    fun setup() {
        // Spring 컨텍스트 초기화
        context = AnnotationConfigApplicationContext()
        context.scan("com.example.kotlinbootlabs.repositories")
        context.refresh()

        // Repository 가져오기
        repository = context.getBean(TestEntityRepository::class.java)

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
