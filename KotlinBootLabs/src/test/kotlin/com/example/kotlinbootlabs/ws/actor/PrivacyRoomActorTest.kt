package com.example.kotlinbootlabs.ws.actor

import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.typed.ActorRef
import com.example.kotlinbootlabs.actor.HelloActorResponse
import com.example.kotlinbootlabs.actor.HelloResponse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean
import java.lang.management.MemoryMXBean
import java.lang.management.ThreadMXBean

class PrivacyRoomActorTest {

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

    @Test
    fun testSendMessage() {
        val probe = testKit.createTestProbe<HelloActorResponse>()
        val testCount:Int = 10000

        // Get memory and thread management beans
        val memoryMXBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
        val osMXBean: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean()

        // Measure memory and CPU usage before the test
        val beforeMemoryUsage = memoryMXBean.heapMemoryUsage.used
        val beforeCpuLoad = osMXBean.systemLoadAverage


        repeat(testCount) { i ->
            val identifier = "testIdentifier-$i"
            val privacyRoomActor: ActorRef<PrivacyRoomCommand> = testKit.spawn(PrivacyRoomActor.create(identifier))
            privacyRoomActor.tell(SetTestProbe(probe.ref))
        }

        repeat(testCount) {
            probe.expectMessage(Duration.ofSeconds(10), HelloResponse("Kotlin"))
        }

        // Measure memory and CPU usage after the test
        val afterMemoryUsage = memoryMXBean.heapMemoryUsage.used
        val afterCpuLoad = osMXBean.systemLoadAverage


        // Calculate the difference
        val memoryUsed  = (afterMemoryUsage - beforeMemoryUsage) / (1024 * 1024)
        val cpuLoadIncrease = afterCpuLoad - beforeCpuLoad
        val cpuLoadPercentage = afterCpuLoad * 100


        println("Memory used: $memoryUsed MB")
        println("CPU load increase: $cpuLoadIncrease")
        println("CPU load percentage: $cpuLoadPercentage%")

    }
}