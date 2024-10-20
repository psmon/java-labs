package com.example.kotlinbootlabs.ws.actor

import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class CounselorRoomActorTest {

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
    fun testInvitePersnalRoomActor() {
        val probe = testKit.createTestProbe<CounselorRoomResponse>()
        val counselorRoomActor = testKit.spawn(CounselorRoomActor.create("Room1"))

        val persnalRoomActor = testKit.createTestProbe<PersnalRoomCommand>().ref
        counselorRoomActor.tell(InvitePersnalRoomActor(persnalRoomActor, probe.ref))
        probe.expectMessage(InvitationCompleted)
    }

    @Test
    fun testChangeStatus() {
        val probe = testKit.createTestProbe<CounselorRoomResponse>()
        val counselorRoomActor = testKit.spawn(CounselorRoomActor.create("Room1"))

        counselorRoomActor.tell(ChangeStatus(CounselorRoomStatus.IN_PROGRESS, probe.ref))
        probe.expectMessage(StatusChangeCompleted(CounselorRoomStatus.IN_PROGRESS))

        counselorRoomActor.tell(ChangeStatus(CounselorRoomStatus.COMPLETED, probe.ref))
        probe.expectMessage(StatusChangeCompleted(CounselorRoomStatus.COMPLETED))
    }
}