package org.mass.utils

import com.appstractive.dnssd.DiscoveryEvent
import com.appstractive.dnssd.discoverServices
import com.appstractive.dnssd.key
import org.mass.State.availableServers
import org.mass.State.removeServer
import org.mass.State.startDiscovery
import kotlinx.coroutines.CoroutineScope

object ServerConnectionUtil {
    private val scanLifecycle = DiscoveryScanLifecycle<DiscoveryEvent>()

    fun scan(scope: CoroutineScope) {
        availableServers.clear()
        startDiscovery()
        scanLifecycle.start(scope, discoverServices("_u-judge._tcp.local.")) {
            when (it) {
                is DiscoveryEvent.Discovered -> {
                    availableServers.discovered(it.service)
                    it.resolve()
                }
                is DiscoveryEvent.Removed -> {
                    availableServers.removed(it.service.key)
                    removeServer(it.service.key)
                }
                is DiscoveryEvent.Resolved -> {
                    availableServers.resolved(it.service)
                }
            }
        }
    }

    fun stopScan() {
        scanLifecycle.stop()
    }
}
