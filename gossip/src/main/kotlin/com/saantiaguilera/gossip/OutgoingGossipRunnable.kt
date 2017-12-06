package com.saantiaguilera.gossip

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runnable that will run according to the poll interval setted by the service, and ask the
 * service to do a gossiping operation
 */
class OutgoingGossipRunnable(private val gossipService: GossipService) : Runnable {

    private val keepRunning = AtomicBoolean(true)

    override fun run() {
        while (keepRunning.get()) {
            try {
                TimeUnit.MILLISECONDS.sleep(gossipService.gossipingPollInterval)
                gossipService.send()
            } catch (ignored: InterruptedException) {
                keepRunning.set(false)
            }
        }
    }

}