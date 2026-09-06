package org.mass.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class DiscoveryScanLifecycle<T> {
    private var job: Job? = null

    fun start(scope: CoroutineScope, events: Flow<T>, onEvent: suspend (T) -> Unit) {
        job?.cancel()
        job = scope.launch {
            events.collect(onEvent)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
