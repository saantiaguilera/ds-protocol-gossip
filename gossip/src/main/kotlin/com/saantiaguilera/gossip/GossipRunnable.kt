package com.saantiaguilera.gossip

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GossipRunnable(private val gossipService: GossipService) : Runnable {

    private val keepRunning = AtomicBoolean(true)

    override fun run() {
        while (keepRunning.get()) {
            try {
                TimeUnit.MILLISECONDS.sleep(gossipService.gossipingPollInterval)
                gossipService.gossip()
            } catch (ignored: InterruptedException) {
                keepRunning.set(false)
            }
        }
    }

}