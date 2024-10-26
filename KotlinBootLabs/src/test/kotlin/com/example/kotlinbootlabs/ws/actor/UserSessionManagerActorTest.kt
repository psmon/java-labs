package com.example.kotlinbootlabs.ws.actor

import akka .actor.testkit.typed.javadsl.ActorTestKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.mockito.Mockito

class UserSessionManagerActorTest {

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
    fun testAddAndRemoveSession() {
        val session = Mockito.mock(WebSocketSession::class.java)
        Mockito.`when`(session.id).thenReturn("session1")

        val actor = testKit.spawn(UserSessionManagerActor.create())

        actor.tell(AddSession(session))
        // Verify session added (you can add more detailed checks if needed)

        actor.tell(RemoveSession(session))
        // Verify session removed (you can add more detailed checks if needed)
    }

    @Test
    fun testSubscribeAndUnsubscribeToTopic() {
        val session = Mockito.mock(WebSocketSession::class.java)
        Mockito.`when`(session.id).thenReturn("session1")

        val actor = testKit.spawn(UserSessionManagerActor.create())

        actor.tell(AddSession(session))
        actor.tell(SubscribeToTopic("session1", "topic1"))
        // Verify subscription (you can add more detailed checks if needed)

        actor.tell(UnsubscribeFromTopic("session1", "topic1"))
        // Verify unsubscription (you can add more detailed checks if needed)
    }

    @Test
    fun testSendMessageToSession() {
        val probe = testKit.createTestProbe<UserSessionResponse>()
        val session = Mockito.mock(WebSocketSession::class.java)
        Mockito.`when`(session.id).thenReturn("session1")

        val actor = testKit.spawn(UserSessionManagerActor.create())

        actor.tell(AddSession(session))
        actor.tell(SendMessageToSession("session1", "Hello"))

        // Test for DelayedMessage
        actor.tell(Ping(probe.ref()))
        probe.expectMessageClass(Pong::class.java)

        Mockito.verify(session).sendMessage(TextMessage("Hello"))

    }

    @Test
    fun testSendMessageToTopic() {
        val probe = testKit.createTestProbe<UserSessionResponse>()
        val session1 = Mockito.mock(WebSocketSession::class.java)
        val session2 = Mockito.mock(WebSocketSession::class.java)
        Mockito.`when`(session1.id).thenReturn("session1")
        Mockito.`when`(session2.id).thenReturn("session2")

        val actor = testKit.spawn(UserSessionManagerActor.create())

        actor.tell(AddSession(session1))
        actor.tell(AddSession(session2))
        actor.tell(SubscribeToTopic("session1", "topic1"))
        actor.tell(SubscribeToTopic("session2", "topic1"))
        actor.tell(SendMessageToTopic("topic1", "Hello Topic"))

        actor.tell(Ping(probe.ref()))
        probe.expectMessageClass(Pong::class.java)

        Mockito.verify(session1).sendMessage(TextMessage("Hello Topic"))
        Mockito.verify(session2).sendMessage(TextMessage("Hello Topic"))


    }

    @Test
    fun testGetSessions() {
        val session1 = Mockito.mock(WebSocketSession::class.java)
        val session2 = Mockito.mock(WebSocketSession::class.java)
        Mockito.`when`(session1.id).thenReturn("session1")
        Mockito.`when`(session2.id).thenReturn("session2")

        val actor = testKit.spawn(UserSessionManagerActor.create())
        val probe = testKit.createTestProbe<UserSessionResponse>()

        actor.tell(AddSession(session1))
        actor.tell(AddSession(session2))
        actor.tell(GetSessions(probe.ref()))

        val response = probe.expectMessageClass(SessionsResponse::class.java)
        assertEquals(2, response.sessions.size)
        assertTrue(response.sessions.containsKey("session1"))
        assertTrue(response.sessions.containsKey("session2"))
    }

    @Test
    fun testPingPong() {
        val actor = testKit.spawn(UserSessionManagerActor.create())
        val probe = testKit.createTestProbe<UserSessionResponse>()

        actor.tell(Ping(probe.ref()))

        val response = probe.expectMessageClass(Pong::class.java)
        assertEquals("Pong", response.message)
    }

}