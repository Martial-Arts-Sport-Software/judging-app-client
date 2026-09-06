package org.mass.connection

class PairingStatusFlow(
    private val polling: PairingStatusPolling
) {
    suspend fun awaitStatus(requestId: String, store: ConnectionStateStore): PairingStatusResult {
        val result = polling.awaitTerminalStatus(requestId)
        when (result) {
            is PairingStatusResult.Accepted -> Unit
            is PairingStatusResult.Rejected -> store.dispatch(
                ConnectionEvent.RejectRealtime(ConnectionFailure.PairingStatusRejected(result.code))
            )
            PairingStatusResult.NotFound,
            PairingStatusResult.MalformedResponse -> store.dispatch(
                ConnectionEvent.RejectRealtime(ConnectionFailure.PairingStatusResponseInvalid)
            )
            PairingStatusResult.Unavailable -> store.dispatch(
                ConnectionEvent.RejectRealtime(ConnectionFailure.PairingStatusUnavailable)
            )
            is PairingStatusResult.Pending -> error("Polling must return a terminal pairing status")
        }
        return result
    }
}
