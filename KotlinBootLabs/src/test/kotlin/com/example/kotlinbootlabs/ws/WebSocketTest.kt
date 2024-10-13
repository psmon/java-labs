package com.example.kotlinbootlabs.ws

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WebSocketTest {

    companion object {
        private lateinit var client: OkHttpClient

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
}