package com.azhari.tclremote.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Finds Android/Google TVs on the Wi‑Fi network through mDNS. */
class TvDiscovery(context: Context) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    fun discover(): Flow<List<Tv>> = callbackFlow {
        val lock = Any()
        val found = LinkedHashMap<String, Tv>()
        val pending = ArrayDeque<NsdServiceInfo>()
        var resolving = false

        // NsdManager resolves one service at a time on older Android versions.
        fun resolveNext() {
            synchronized(lock) {
                if (resolving) return
                val next = pending.removeFirstOrNull() ?: return
                resolving = true
                @Suppress("DEPRECATION")
                nsd.resolveService(next, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                        synchronized(lock) { resolving = false }
                        resolveNext()
                    }

                    override fun onServiceResolved(info: NsdServiceInfo) {
                        @Suppress("DEPRECATION")
                        val host = info.host?.hostAddress
                        synchronized(lock) {
                            if (host != null) found[info.serviceName] = Tv(host, info.serviceName)
                            resolving = false
                            trySend(found.values.toList())
                        }
                        resolveNext()
                    }
                })
            }
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onServiceFound(info: NsdServiceInfo) {
                synchronized(lock) { pending.addLast(info) }
                resolveNext()
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                synchronized(lock) {
                    found.remove(info.serviceName)
                    trySend(found.values.toList())
                }
            }

            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }

        trySend(emptyList())
        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose { runCatching { nsd.stopServiceDiscovery(listener) } }
    }

    private companion object {
        const val SERVICE_TYPE = "_androidtvremote2._tcp"
    }
}
