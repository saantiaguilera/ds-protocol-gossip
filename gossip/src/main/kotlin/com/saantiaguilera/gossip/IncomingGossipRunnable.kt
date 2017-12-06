package com.saantiaguilera.gossip

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Incoming gossiping runnable that will be waiting in a socket#receive mode. When a gossiping sends data to us
 * we will update our member lists and stuff.
 */
class IncomingGossipRunnable(private val gossipService: GossipService) : Runnable {

    private val keepRunning = AtomicBoolean(true)

    override fun run() {
        while (keepRunning.get()) {
            try {
                gossipService.receive()
            } catch (ignored: Exception) {
                keepRunning.set(false)
            }

        }
    }

}