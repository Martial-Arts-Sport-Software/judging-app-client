package org.mass.connection

import kotlinx.coroutines.delay

class PairingStatusPolling(
    private val fetch: suspend (requestId: String) -> PairingStatusResult,
    private val intervalMillis: Long = 1_000
) {
    suspend fun awaitTerminalStatus(requestId: String): PairingStatusResult {
        while (true) {
            when (val result = fetch(requestId)) {
                is PairingStatusResult.Pending -> delay(intervalMillis)
                else -> return result
            }
        }
    }
}
