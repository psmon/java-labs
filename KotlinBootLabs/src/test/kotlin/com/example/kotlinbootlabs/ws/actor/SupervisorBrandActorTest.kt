package com.example.kotlinbootlabs.ws.actor

import akka.actor.testkit.typed.javadsl.ActorTestKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SupervisorBrandActorTest {

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
    fun testCreateCounselorManager() {
        val probe = testKit.createTestProbe<SupervisorBrandResponse>()
        val brandSupervisorActor = testKit.spawn(SupervisorBrandActor.create())

        brandSupervisorActor.tell(CreateCounselorManager("Brand1", probe.ref))
        probe.expectMessage(CounselorManagerCreated("Brand1"))
    }

    @Test
    fun testGetCounselorManager() {
        val probe = testKit.createTestProbe<SupervisorBrandResponse>()
        val supervisorBrandActor = testKit.spawn(SupervisorBrandActor.create())

        supervisorBrandActor.tell(CreateCounselorManager("Brand1", probe.ref))
        probe.expectMessage(CounselorManagerCreated("Brand1"))

        supervisorBrandActor.tell(GetCounselorManager("Brand1", probe.ref))
        probe.expectMessageClass(CounselorManagerFound::class.java)

    }

    @Test
    fun testCreateCounselorManagerAlreadyExists() {
        val probe = testKit.createTestProbe<SupervisorBrandResponse>()
        val supervisorBrandActor = testKit.spawn(SupervisorBrandActor.create())

        supervisorBrandActor.tell(CreateCounselorManager("Brand1", probe.ref))
        probe.expectMessage(CounselorManagerCreated("Brand1"))

        supervisorBrandActor.tell(CreateCounselorManager("Brand1", probe.ref))
        probe.expectMessage(SupervisorErrorBrandResponse("CounselorManager for brand Brand1 already exists."))
    }

    @Test
    fun testGetCounselorManagerNotFound() {
        val probe = testKit.createTestProbe<SupervisorBrandResponse>()
        val supervisorBrandActor = testKit.spawn(SupervisorBrandActor.create())

        supervisorBrandActor.tell(GetCounselorManager("NonExistentBrand", probe.ref))
        probe.expectMessage(SupervisorErrorBrandResponse("CounselorManager for brand NonExistentBrand not found."))
    }
}