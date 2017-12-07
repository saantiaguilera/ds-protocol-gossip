package com.saantiaguilera.gossip

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.experimental.RestrictsSuspension

/**
 * Incoming gossiping runnable that will be waiting in a socket#receive mode. When a gossiping sends data to us
 * we will update our member lists and stuff.
 */
@RestrictsSuspension
class IncomingGossipRunnable(private val gossipService: GossipService) {

    private val keepRunning = AtomicBoolean(true)

    operator suspend fun invoke() {
        while (keepRunning.get()) {
            try {
                gossipService.receive()
            } catch (ignored: Exception) {
                keepRunning.set(false)
            }

        }
    }

}