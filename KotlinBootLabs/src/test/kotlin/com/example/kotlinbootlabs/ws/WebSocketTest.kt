package com.example.kotlinbootlabs.ws

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.web.client.RestTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WebSocketTest {

    companion object {
        private lateinit var client: OkHttpClient
        private val restTemplate = RestTemplate()

        @BeforeAll
        @JvmStatic
        fun setup() {
            client = OkHttpClient()
        }
    }

    @Test
    fun testWebSocketConnection() {
        val latch = CountDownLatch(1)
        val request = Request.Builder().url("ws://localhost:8080/ws").build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("hello")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                assertEquals("Echo: hello", text)
                latch.countDown()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
                latch.countDown()
            }
        }

        client.newWebSocket(request, listener)
        latch.await(10, TimeUnit.SECONDS)
    }

    @Test
    fun testSubscribeToTopic() {
        val latch = CountDownLatch(1)
        val request = Request.Builder().url("ws://localhost:8080/ws").build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("subscribe:test-topic")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                assertEquals("Subscribed to topic: test-topic", text)
                latch.countDown()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
                latch.countDown()
            }
        }

        client.newWebSocket(request, listener)
        latch.await(10, TimeUnit.SECONDS)
    }

    @Test
    fun testSendMessageToSubscribedUsers() {
        val latch = CountDownLatch(1)
        val request = Request.Builder().url("ws://localhost:8080/ws").build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("subscribe:test-topic")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text == "Hello Subscribers") {
                    assertEquals("Hello Subscribers", text)
                    latch.countDown()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
                latch.countDown()
            }
        }

        client.newWebSocket(request, listener)

        // Wait for subscription to complete
        Thread.sleep(1000)

        // Send message to topic using REST endpoint
        val url = "http://localhost:8080/send-to-topic?topic=test-topic"
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), "Hello Subscribers")
        val restRequest = Request.Builder().url(url).post(requestBody).build()
        client.newCall(restRequest).execute()

        latch.await(10, TimeUnit.SECONDS)
    }

    @Test
    fun testUnsubscribeFromTopic() {
        val latch = CountDownLatch(1)
        val request = Request.Builder().url("ws://localhost:8080/ws").build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("subscribe:test-topic")
                webSocket.send("unsubscribe:test-topic")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                assertEquals("Unsubscribed from topic: test-topic", text)
                latch.countDown()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
                latch.countDown()
            }
        }

        client.newWebSocket(request, listener)
        latch.await(10, TimeUnit.SECONDS)
    }
}