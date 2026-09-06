package org.mass.connection

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HeartbeatClientTest {
    @Test
    fun heartbeatAcknowledgementConfirmsSocketLiveness() = runTest {
        val socket = FakeRealtimeSocket("""{"type":"heartbeat_ack"}""")

        assertEquals(HeartbeatResult.Acknowledged, HeartbeatClient().send(socket))
        assertEquals(listOf("""{"type":"heartbeat"}"""), socket.sentPayloads)
    }

    @Test
    fun heartbeatRejectionPreservesServerCode() = runTest {
        val socket = FakeRealtimeSocket("""{"type":"heartbeat_rejected","code":"invalid_heartbeat"}""")

        assertEquals(
            HeartbeatResult.Rejected("invalid_heartbeat"),
            HeartbeatClient().send(socket)
        )
    }

    @Test
    fun malformedHeartbeatResponseIsNotTreatedAsAcknowledgement() = runTest {
        val socket = FakeRealtimeSocket("""{"type":"heartbeat_ack","code":"unexpected"}""")

        assertEquals(HeartbeatResult.InvalidResponse, HeartbeatClient().send(socket))
    }

    @Test
    fun socketCancellationPropagatesToConnectionLifecycle() = runTest {
        val socket = object : RealtimeSocket {
            override suspend fun send(payload: String) = Unit

            override suspend fun receive(): String = throw CancellationException("connection screen closed")

            override suspend fun close() = Unit
        }

        assertFailsWith<CancellationException> {
            HeartbeatClient().send(socket)
        }
    }

    private class FakeRealtimeSocket(vararg responses: String) : RealtimeSocket {
        val sentPayloads = mutableListOf<String>()
        private val responses = responses.toMutableList()

        override suspend fun send(payload: String) {
            sentPayloads += payload
        }

        override suspend fun receive(): String = responses.removeFirst()

        override suspend fun close() = Unit
    }
}
