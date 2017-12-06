package com.saantiaguilera.gossip

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Simple timer to broadcast when a member should be considered dead
 */
typealias OnMemberDead = (GossipMember) -> Unit
class GossipTimer(private val gossipMember: GossipMember, private val onTimeout: OnMemberDead) {

    private val timeout = 10_000L

    private val timer by lazy { Timer() }
    private val running = AtomicBoolean(false)

    fun start() {
        if (!running.get()) {
            running.set(true)
            timer.schedule(TimerRunnable(gossipMember, onTimeout), timeout)
        }
    }

    fun reset() {
        if (running.get()) {
            stop()
            start()
        }
    }

    fun stop() {
        if (running.get()) {
            running.set(false)
            timer.cancel()
            timer.purge()
        }
    }

    companion object {
        private class TimerRunnable(val gossipMember: GossipMember, val onTimeout: OnMemberDead) : TimerTask() {
            override fun run() {
                onTimeout(gossipMember)
            }
        }
    }

}