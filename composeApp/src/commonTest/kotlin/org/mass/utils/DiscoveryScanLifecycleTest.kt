package org.mass.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryScanLifecycleTest {
    @Test
    fun replacingAScanStopsThePreviousCollector() = runTest {
        val lifecycle = DiscoveryScanLifecycle<Int>()
        val firstScan = MutableSharedFlow<Int>()
        val secondScan = MutableSharedFlow<Int>()
        val received = mutableListOf<Int>()

        lifecycle.start(backgroundScope, firstScan) { received += it }
        runCurrent()
        firstScan.emit(1)
        runCurrent()

        lifecycle.start(backgroundScope, secondScan) { received += it }
        runCurrent()
        firstScan.emit(2)
        secondScan.emit(3)
        runCurrent()

        assertEquals(listOf(1, 3), received)
    }

    @Test
    fun stoppingAScanPreventsFurtherDiscoveryEvents() = runTest {
        val lifecycle = DiscoveryScanLifecycle<Int>()
        val scan = MutableSharedFlow<Int>()
        val received = mutableListOf<Int>()

        lifecycle.start(backgroundScope, scan) { received += it }
        runCurrent()
        scan.emit(1)
        runCurrent()
        lifecycle.stop()
        scan.emit(2)
        runCurrent()

        assertEquals(listOf(1), received)
    }
}
