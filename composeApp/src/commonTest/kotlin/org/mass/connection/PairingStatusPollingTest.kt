package org.mass.connection

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class PairingStatusPollingTest {
    @Test
    fun pollsPendingStatusUntilTheDeviceIsAccepted() = runTest {
        var requests = 0
        val polling = PairingStatusPolling(
            fetch = {
                requests += 1
                if (requests == 1) PairingStatusResult.Pending("device-1")
                else PairingStatusResult.Accepted("device-1")
            },
            intervalMillis = 100
        )

        val result = async { polling.awaitTerminalStatus("request-1") }
        runCurrent()
        testScheduler.advanceTimeBy(100)
        runCurrent()

        assertEquals(PairingStatusResult.Accepted("device-1"), result.await())
        assertEquals(2, requests)
    }

    @Test
    fun stopsPollingWhenTheDeviceIsRejected() = runTest {
        var requests = 0
        val polling = PairingStatusPolling(
            fetch = {
                requests += 1
                PairingStatusResult.Rejected("device-1", "operator_rejected")
            }
        )

        assertEquals(
            PairingStatusResult.Rejected("device-1", "operator_rejected"),
            polling.awaitTerminalStatus("request-1")
        )
        assertEquals(1, requests)
    }

    @Test
    fun stopsPollingWhenTheStatusResponseIsTerminallyInvalid() = runTest {
        var requests = 0
        val polling = PairingStatusPolling(
            fetch = {
                requests += 1
                PairingStatusResult.MalformedResponse
            }
        )

        assertEquals(PairingStatusResult.MalformedResponse, polling.awaitTerminalStatus("request-1"))
        assertEquals(1, requests)
    }

    @Test
    fun cancellationStopsAPendingPollBeforeItsNextRequest() = runTest {
        var requests = 0
        val polling = PairingStatusPolling(
            fetch = {
                requests += 1
                PairingStatusResult.Pending("device-1")
            },
            intervalMillis = 100
        )

        val result = async { polling.awaitTerminalStatus("request-1") }
        runCurrent()
        result.cancelAndJoin()
        testScheduler.advanceTimeBy(100)
        runCurrent()

        assertFalse(result.isCompleted && !result.isCancelled)
        assertEquals(1, requests)
    }
}
