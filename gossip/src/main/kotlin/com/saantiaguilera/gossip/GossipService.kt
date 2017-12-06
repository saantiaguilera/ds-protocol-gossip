package com.saantiaguilera.gossip

import java.io.IOException
import sun.plugin2.liveconnect.ArgumentHelper.writeObject
import java.io.ObjectOutputStream
import java.io.ByteArrayOutputStream
import java.net.*
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean




class GossipService {

    private val memberList by lazy { ArrayList<GossipMember>() }
    private val deadList by lazy { ArrayList<GossipMember>() }

    internal val gossipingPollInterval = 100L
    internal val gossipCleanupTime = 10_000L

    private var threadDelegate: Thread? = null

    private val me by lazy {
        val address = InetAddress.getLocalHost().hostAddress
        memberList.first { it.host == address }
    }

    fun add(gossipMember: GossipMember) = memberList.add(gossipMember)
    fun remove(gossipMember: GossipMember) = memberList.remove(gossipMember)

    internal fun gossip() {
        me.heartbeat = me.heartbeat++

        // If we are asked to gossip, make it sequentially. Else we might f**k our heartbeat
        synchronized(memberList) {
            val member = memberList[Random().nextInt(memberList.size + 1)]

            if (member != me) {
                val byteArrOS = ByteArrayOutputStream()
                ObjectOutputStream(byteArrOS).writeObject(memberList)
                val byteArr = byteArrOS.toByteArray()

                val socket = DatagramSocket()
                socket.send(DatagramPacket(byteArr, byteArr.size, InetSocketAddress(member.host, member.port)))
                socket.close()
            }
        }
    }

    /**
     * Watchout this function will create a daemon thread!
     */
    fun run() {
        shutdown()
        threadDelegate = Thread(GossipRunnable(this)).apply {
            isDaemon = true
            start()
        }
    }

    fun shutdown() {
        if (threadDelegate != null && threadDelegate!!.isAlive) {
            // Since its a daemon, we cant join. We interrupt it
            threadDelegate!!.interrupt()
        }
    }

}