package org.mass.connection

import org.mass.transport.DurableEventOutbox
import org.mass.transport.EventOutboxStorage
import org.mass.transport.OutboxEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class RealtimeCommandFaultInjectionTest {
    @Test
    fun droppedSendSurvivesRecreationAndRetriesWithTheOriginalEventId() = runTest {
        val storage = SharedStorage()
        val event = event("event-1", 1)
        val initialOutbox = DurableEventOutbox(storage)

        assertFailsWith<IllegalStateException> {
            RealtimeCommandClient(initialOutbox).send(event, DroppedSocket(), nowMillis = 100)
        }

        val recoveredOutbox = DurableEventOutbox(storage)
        val recoveredSocket = ResponseSocket("""{"type":"command_ack","eventId":"event-1"}""")

        assertEquals(
            RealtimeCommandResult.Accepted("event-1"),
            RealtimeCommandClient(recoveredOutbox).send(event, recoveredSocket, nowMillis = 200)
        )
        assertEquals(1, "\"eventId\":\"event-1\"".toRegex().findAll(recoveredSocket.sent.single()).count())
        assertEquals(emptyList(), recoveredOutbox.pendingEvents())
    }

    @Test
    fun delayedAcknowledgementForAnotherEventKeepsBothEventsPending() = runTest {
        val storage = SharedStorage()
        val first = event("event-1", 1)
        val second = event("event-2", 2)
        val outbox = DurableEventOutbox(storage)
        outbox.enqueue(second)

        assertEquals(
            RealtimeCommandResult.InvalidResponse,
            RealtimeCommandClient(outbox).send(
                first,
                ResponseSocket("""{"type":"command_ack","eventId":"event-2"}"""),
                nowMillis = 100
            )
        )

        assertEquals(listOf(first, second), DurableEventOutbox(storage).pendingEvents())
    }

    private fun event(eventId: String, sequence: Long) = OutboxEvent(
        eventId = eventId,
        clientSequence = sequence,
        clientTimestampMillis = 0,
        clientTimestamp = "2026-09-01T10:00:00Z",
        sessionId = "session-1",
        payload = """{"type":"attention"}"""
    )

    private class DroppedSocket : RealtimeSocket {
        override suspend fun send(payload: String): Nothing = error("connection dropped")

        override suspend fun receive(): String = error("connection dropped")

        override suspend fun close() = Unit
    }

    private class ResponseSocket(private val response: String) : RealtimeSocket {
        val sent = mutableListOf<String>()

        override suspend fun send(payload: String) {
            sent += payload
        }

        override suspend fun receive(): String = response

        override suspend fun close() = Unit
    }

    private class SharedStorage : EventOutboxStorage {
        private var value: String? = null

        override fun load(): String? = value

        override fun save(value: String) {
            this.value = value
        }
    }
}
