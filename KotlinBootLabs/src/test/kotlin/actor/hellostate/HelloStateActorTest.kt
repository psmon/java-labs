package actor.hellostate

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.testkit.typed.javadsl.ManualTime
import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration

class HelloStateActorTest {

    companion object {
        private lateinit var testKit: ActorTestKit
        private lateinit var manualTime: ManualTime

        @BeforeAll
        @JvmStatic
        fun setup() {
            val config = ManualTime.config().withFallback(ConfigFactory.defaultApplication())
            testKit = ActorTestKit.create(config)
            manualTime = ManualTime.get(testKit.system())
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun testHelloStateActorRespondsBasedOnState() {
        val probe = testKit.createTestProbe<Any>()

        val helloStateActor = testKit.spawn(HelloStateActor.create(State.HAPPY))

        // Test in HAPPY state
        helloStateActor.tell(Hello("Hello", probe.ref()))
        probe.expectMessage(HelloResponse("Kotlin"))

        helloStateActor.tell(GetHelloTotalCount(probe.ref()))
        probe.expectMessage(HelloCountResponse(1))

        // Change state to ANGRY
        helloStateActor.tell(ChangeState(State.ANGRY))

        // Test in ANGRY state
        helloStateActor.tell(Hello("Hello", probe.ref()))
        probe.expectMessage(HelloResponse("Don't talk to me!"))

        helloStateActor.tell(GetHelloTotalCount(probe.ref()))
        probe.expectMessage(HelloCountResponse(1)) // Count should not change
    }

    @Test
    fun testHelloLimitCommand() {
        val probe = testKit.createTestProbe<Any>()
        val helloStateActor = testKit.spawn(HelloStateActor.create(State.HAPPY))

        // Send 100 HelloLimit messages
        val startTime = System.currentTimeMillis()
        for (i in 1..100) {
            helloStateActor.tell(HelloLimit("Hello", probe.ref()))
        }

        // Expect 100 responses
        for (i in 1..100) {
            probe.expectMessage(HelloResponse("Kotlin"))
        }
        val endTime = System.currentTimeMillis()

        // Calculate TPS
        val durationInSeconds = (endTime - startTime) / 1000.0
        val tps = 100 / durationInSeconds
        println("TPS: $tps")

        // Verify the hello count
        helloStateActor.tell(GetHelloTotalCount(probe.ref()))
        probe.expectMessage(HelloCountResponse(100))
    }

    @Test
    fun testResetHelloCount() {
        val probe = testKit.createTestProbe<Any>()
        val helloStateActor = testKit.spawn(HelloStateActor.create(State.HAPPY))

        // Send Hello messages
        helloStateActor.tell(Hello("Hello", probe.ref()))
        helloStateActor.tell(Hello("Hello", probe.ref()))

        probe.expectMessage(HelloResponse("Kotlin"))
        probe.expectMessage(HelloResponse("Kotlin"))

        // Verify the hello count
        helloStateActor.tell(GetHelloCount(probe.ref()))
        probe.expectMessage(HelloCountResponse(2))

        // Wait for the timer to reset the count
        //Thread.sleep(Duration.ofSeconds(11).toMillis())

        // Advance the time by 11 seconds
        manualTime.timePasses(Duration.ofSeconds(11))

        // Verify the hello count is reset
        helloStateActor.tell(GetHelloCount(probe.ref()))
        probe.expectMessage(HelloCountResponse(0))
    }
}