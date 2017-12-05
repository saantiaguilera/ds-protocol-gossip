package com.saantiaguilera.gossip

import java.io.IOException
import sun.plugin2.liveconnect.ArgumentHelper.writeObject
import java.io.ObjectOutputStream
import java.io.ByteArrayOutputStream
import java.net.*
import java.util.*


class GossipService {

    private val memberList by lazy { ArrayList<GossipMember>() }
    private val deadList by lazy { ArrayList<GossipMember>() }

    private val gossipingPollInterval: Int = 100
    private val gossipCleanupTime: Int = 10_000

    private val server by lazy { DatagramSocket(me.port) }

    private val me by lazy {
        val address = InetAddress.getLocalHost().hostAddress
        memberList.first { it.host == address }
    }

    fun add(gossipMember: GossipMember) = memberList.add(gossipMember)
    fun remove(gossipMember: GossipMember) = memberList.remove(gossipMember)

    fun send() {
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

}