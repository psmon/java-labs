package actor

import akka.actor.testkit.typed.javadsl.ActorTestKit
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
        val probe = testKit.createTestProbe<Any>()
        val counselorActor = testKit.spawn(CounselorActor.create("Counselor1"))

        counselorActor.tell(GoOnline(probe.ref))
        probe.expectMessage(StatusResponse("Counselor1 상담원이 온라인 상태입니다."))
    }

    @Test
    fun testGoOffline() {
        val probe = testKit.createTestProbe<Any>()
        val counselorActor = testKit.spawn(CounselorActor.create("Counselor1"))

        counselorActor.tell(GoOffline(probe.ref))
        probe.expectMessage(StatusResponse("Counselor1 상담원이 오프라인 상태입니다."))
    }


    @Test
    fun testStartTask() {
        val probe = testKit.createTestProbe<Any>()
        val counselorActor = testKit.spawn(CounselorActor.create("Counselor1"))

        counselorActor.tell(GoOnline(probe.ref))
        probe.expectMessage(StatusResponse("Counselor1 상담원이 온라인 상태입니다."))

        val task = CounselorTask("1", "Client1")
        counselorActor.tell(StartTask(task, probe.ref))
        probe.expectMessage(TaskStartedResponse(task.copy(status = "상담진행중")))
    }

    @Test
    fun testEndTask() {
        val probe = testKit.createTestProbe<Any>()
        val counselorActor = testKit.spawn(CounselorActor.create("Counselor1"))

        counselorActor.tell(GoOnline(probe.ref))
        probe.expectMessage(StatusResponse("Counselor1 상담원이 온라인 상태입니다."))

        val task = CounselorTask("1", "Client1")
        counselorActor.tell(StartTask(task, probe.ref))
        probe.expectMessage(TaskStartedResponse(task.copy(status = "상담진행중")))

        counselorActor.tell(EndTask("1", probe.ref))
        probe.expectMessage(TaskEndedResponse("1"))
    }

    @Test
    fun testGetStatus() {
        val probe = testKit.createTestProbe<Any>()
        val counselorActor = testKit.spawn(CounselorActor.create("Counselor1"))

        counselorActor.tell(GetStatus(probe.ref))
        probe.expectMessage(StatusResponse("Counselor1 상담원은 Offline 상태이며, 진행 중인 Task 수는 0개입니다."))

        counselorActor.tell(GoOnline(probe.ref))
        probe.expectMessage(StatusResponse("Counselor1 상담원이 온라인 상태입니다."))

        counselorActor.tell(GetStatus(probe.ref))
        probe.expectMessage(StatusResponse("Counselor1 상담원은 Online 상태이며, 진행 중인 Task 수는 0개입니다."))
    }

    @Test
    fun testTaskCount() {
        val probe = testKit.createTestProbe<Any>()
        val counselorActor = testKit.spawn(CounselorActor.create("Counselor1"))

        // Initial status check
        counselorActor.tell(GetStatus(probe.ref))
        probe.expectMessage(StatusResponse("Counselor1 상담원은 Offline 상태이며, 진행 중인 Task 수는 0개입니다."))

        // Go online
        counselorActor.tell(GoOnline(probe.ref))
        probe.expectMessage(StatusResponse("Counselor1 상담원이 온라인 상태입니다."))

        // Start a task
        val task1 = CounselorTask("1", "Client1")
        counselorActor.tell(StartTask(task1, probe.ref))
        probe.expectMessage(TaskStartedResponse(task1.copy(status = "상담진행중")))

        // Check status after starting a task
        counselorActor.tell(GetStatus(probe.ref))
        probe.expectMessage(StatusResponse("Counselor1 상담원은 Online 상태이며, 진행 중인 Task 수는 1개입니다."))

        // Start another task
        val task2 = CounselorTask("2", "Client2")
        counselorActor.tell(StartTask(task2, probe.ref))
        probe.expectMessage(TaskStartedResponse(task2.copy(status = "상담진행중")))

        // Check status after starting another task
        counselorActor.tell(GetStatus(probe.ref))
        probe.expectMessage(StatusResponse("Counselor1 상담원은 Online 상태이며, 진행 중인 Task 수는 2개입니다."))

        // End the first task
        counselorActor.tell(EndTask("1", probe.ref))
        probe.expectMessage(TaskEndedResponse("1"))

        // Check status after ending the first task
        counselorActor.tell(GetStatus(probe.ref))
        probe.expectMessage(StatusResponse("Counselor1 상담원은 Online 상태이며, 진행 중인 Task 수는 1개입니다."))

        // End the second task
        counselorActor.tell(EndTask("2", probe.ref))
        probe.expectMessage(TaskEndedResponse("2"))

        // Check status after ending the second task
        counselorActor.tell(GetStatus(probe.ref))
        probe.expectMessage(StatusResponse("Counselor1 상담원은 Online 상태이며, 진행 중인 Task 수는 0개입니다."))
    }

}