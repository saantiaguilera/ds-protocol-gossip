package com.saantiaguilera.gossip

import kotlinx.coroutines.experimental.delay
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.experimental.RestrictsSuspension

/**
 * Runnable that will run according to the poll interval setted by the service, and ask the
 * service to do a gossiping operation
 */
@RestrictsSuspension
class OutgoingGossipRunnable(private val gossipService: GossipService) {

    private val keepRunning = AtomicBoolean(true)

    operator suspend fun invoke() {
        while (keepRunning.get()) {
            try {
                delay(gossipService.gossipingPollInterval)
                gossipService.send()
            } catch (ignored: InterruptedException) {
                keepRunning.set(false)
            }
        }
    }

}