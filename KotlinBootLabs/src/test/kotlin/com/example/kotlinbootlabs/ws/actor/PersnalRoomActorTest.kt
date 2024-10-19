package com.example.kotlinbootlabs.ws.actor

import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.typed.ActorRef
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean
import java.lang.management.MemoryMXBean

class PersnalRoomActorTest {

    companion object {
        private val testKit = ActorTestKit.create()

        @BeforeAll
        @JvmStatic
        fun setup() {
            // Setup code if needed
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            testKit.shutdownTestKit()
        }
    }

    private fun takeMemorySnapshot(): Long {
        val memoryMXBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
        return memoryMXBean.heapMemoryUsage.used
    }

    private fun measureCpuLoad(): Double {
        val osMXBean: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean()
        return osMXBean.systemLoadAverage
    }

    private fun reportResults(beforeMemoryUsage: Long, afterMemoryUsage: Long, beforeCpuLoad: Double, afterCpuLoad: Double) {
        val memoryUsed = (afterMemoryUsage - beforeMemoryUsage) / (1024 * 1024)
        val cpuLoadIncrease = afterCpuLoad - beforeCpuLoad
        val cpuLoadPercentage = afterCpuLoad * 100

        println("Memory used: $memoryUsed MB")
        println("CPU load increase: $cpuLoadIncrease")
        println("CPU load percentage: $cpuLoadPercentage%")
    }

    @Test
    fun testSendMessage() {
        val probe = testKit.createTestProbe<PersnalRoomResponse>()
        val testCount:Int = 10000

        // Measure memory and CPU usage before the test
        val beforeMemoryUsage = takeMemorySnapshot()
        val beforeCpuLoad = measureCpuLoad()


        repeat(testCount) { i ->
            val identifier = "testIdentifier-$i"
            val persnalRoomActor: ActorRef<PersnalRoomCommand> = testKit.spawn(PersnalRoomActor.create(identifier))
            persnalRoomActor.tell(SetTestProbe(probe.ref))
        }

        repeat(testCount) {
            probe.expectMessage(Duration.ofSeconds(10), PrivacyHelloResponse("Hello World"))
        }

        // Measure memory and CPU usage after the test
        val afterMemoryUsage = takeMemorySnapshot()
        val afterCpuLoad = measureCpuLoad()

        // Report results
        reportResults(beforeMemoryUsage, afterMemoryUsage, beforeCpuLoad, afterCpuLoad)

    }
}