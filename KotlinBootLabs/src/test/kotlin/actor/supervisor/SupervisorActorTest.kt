package actor.supervisor

import com.example.kotlinbootlabs.actor.HelloActorResponse
import com.example.kotlinbootlabs.actor.HelloResponse
import akka.actor.testkit.typed.javadsl.ActorTestKit
import com.example.kotlinbootlabs.actor.supervisor.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SupervisorActorTest {

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
    fun testSupervisorCreatesChildAndResponds() {
        val responseProbe = testKit.createTestProbe<HelloActorResponse>()
        val supervisor = testKit.spawn(SupervisorActor.create())

        // 자식 액터 생성 요청
        supervisor.tell(CreateChild("child1"))

        // 자식 액터에게 메시지 전송 요청
        supervisor.tell(SendHello("child1", "Hello", responseProbe.ref()))

        // 응답 확인
        responseProbe.expectMessage(HelloResponse("Kotlin"))

        // 자식 액터 수 확인
        val countProbe = testKit.createTestProbe<Int>()
        supervisor.tell(GetChildCount(countProbe.ref()))
        countProbe.expectMessage(1)
    }

    @Test
    fun testSupervisorHandlesUnknownChild() {
        val responseProbe = testKit.createTestProbe<HelloActorResponse>()
        val supervisor = testKit.spawn(SupervisorActor.create())

        // 존재하지 않는 자식 액터에게 메시지 전송
        supervisor.tell(SendHello("unknownChild", "Hello", responseProbe.ref()))

        // 응답이 없음을 확인
        responseProbe.expectNoMessage()

        // 자식 액터 수는 0이어야 함
        val countProbe = testKit.createTestProbe<Int>()
        supervisor.tell(GetChildCount(countProbe.ref()))
        countProbe.expectMessage(0)
    }

    @Test
    fun testSupervisorRestartsChildOnFailure() {
        val responseProbe = testKit.createTestProbe<HelloActorResponse>()
        val supervisor = testKit.spawn(SupervisorActor.create())

        // 자식 액터 생성 및 의도적인 예외 발생
        supervisor.tell(CreateChild("child2"))

        // 자식 액터에게 잘못된 메시지 전송하여 예외 유발
        supervisor.tell(SendHello("child2", "InvalidMessage", responseProbe.ref()))

        // 응답이 없음을 확인 (자식 액터가 예외로 인해 재시작됨)
        responseProbe.expectNoMessage()

        // 올바른 메시지 재전송
        supervisor.tell(SendHello("child2", "Hello", responseProbe.ref()))

        // 응답 확인 (자식 액터가 재시작되어 정상 동작함)
        responseProbe.expectMessage(HelloResponse("Kotlin"))
    }

    @Test
    fun testSupervisorDetectsChildTermination() {

        val supervisor = testKit.spawn(SupervisorActor.create())

        // 자식 액터 생성
        supervisor.tell(CreateChild("child4"))

        // 자식 액터 수 확인 (1이어야 함)
        val countProbe = testKit.createTestProbe<Int>()
        supervisor.tell(GetChildCount(countProbe.ref()))
        countProbe.expectMessage(1)

        // 자식 액터 종료 요청
        supervisor.tell(TerminateChild("child4"))

        // 잠시 대기하여 종료 처리를 기다림
        Thread.sleep(500)

        // 자식 액터 수 확인 (0이어야 함)
        supervisor.tell(GetChildCount(countProbe.ref()))
        countProbe.expectMessage(0)
    }

}