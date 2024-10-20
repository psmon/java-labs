package com.example.kotlinbootlabs.ws.actor

import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class CounselorActorTest {

    companion object {
        private lateinit var testKit: ActorTestKit

        @BeforeAll
        @JvmStatic
        fun setup() {
            testKit = ActorTestKit.create()
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun testGoOnline() {
        val probe = testKit.createTestProbe<CounselorResponse>()
        val counselorActor = testKit.spawn(CounselorActor.create("Counselor1"))

        counselorActor.tell(GoOnline(probe.ref))
        probe.expectMessage(StatusChanged(CounselorStatus.ONLINE))
    }

    @Test
    fun testGoOffline() {
        val probe = testKit.createTestProbe<CounselorResponse>()
        val counselorActor = testKit.spawn(CounselorActor.create("Counselor1"))

        counselorActor.tell(GoOffline(AwayStatus.BREAK, probe.ref))
        probe.expectMessage(StatusChanged(CounselorStatus.OFFLINE, AwayStatus.BREAK))
    }

    @Test
    fun testAssignTask() {
        val probe = testKit.createTestProbe<CounselorResponse>()
        val counselorActor = testKit.spawn(CounselorActor.create("Counselor1"))

        counselorActor.tell(AssignTask("Task1", probe.ref))
        probe.expectMessage(TaskAssigned("Task1"))
    }
}