package com.saantiaguilera.gossip

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import java.io.ByteArrayInputStream
import java.io.ObjectOutputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.net.*
import java.util.*
import kotlin.collections.HashMap

/**
 * Gossip service class. This class manages a node (aka GossipMember) and is in charge of gossiping every some poll
 * interval and receiving gossip requests.
 */
class GossipService {

    /**
     * Lists of memberships this member holds
     */
    private val memberList by lazy { HashMap<GossipMember, GossipTimer>() }
    private val deadList by lazy { ArrayList<GossipMember>() }

    /**
     * Poll interval for sending gossiping events
     */
    internal val gossipingPollInterval = 100L

    /**
     * Coroutines for delegating incoming / outgoing gossiping functions
     */
    private var outgoingGossipsCoroutine: Job? = null
    private var incomingGossipsCoroutine: Job? = null

    /**
     * The member attached to this service
     */
    private val me by lazy {
        val address = InetAddress.getLocalHost().hostAddress
        memberList.keys.first { it.host == address }
    }

    /**
     * Lamba expression used as callback when a member has timedout and should be considered a dead node.
     */
    private val onMemberDead by lazy {{ gossipMember: GossipMember ->
        synchronized(memberList) {
            memberList.remove(gossipMember)
        }
        synchronized(deadList) {
            if (!deadList.contains(gossipMember)) {
                deadList.add(gossipMember)
            }
        }
    }}

    /**
     * Add a member
     */
    fun add(member: GossipMember) = memberList.put(member, GossipTimer(member, onMemberDead))

    /**
     * Add all gossip members
     */
    fun addAll(members: List<GossipMember>) = members.forEach { add(it) }

    /**
     * Remove a gossip member. This will also stop its attached timer.
     */
    fun remove(member: GossipMember) = memberList.remove(member)?.stop()

    /**
     * Do a gossiping operation. We will increment our heartbeat and broadcast our members to a
     * random gossiper
     */
    suspend internal fun send() {
        me.heartbeat = me.heartbeat++

        // If we are asked to gossip, make it sequentially. Else we might f**k our heartbeat
        synchronized(memberList) {
            val member = memberList.keys.elementAt(Random().nextInt(memberList.size + 1))

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
     * Incoming gossiping, update our member lists
     */
    suspend internal fun receive() {
        val buffer = ByteArray(Integer.MAX_VALUE)
        val packet = DatagramPacket(buffer, buffer.size)

        DatagramSocket(me.port).receive(packet)

        val inputStream = ByteArrayInputStream(packet.data)
        val readObject = ObjectInputStream(inputStream).readObject()

        if (readObject is ArrayList<*>) {
            @Suppress("UNCHECKED_CAST")
            (readObject as ArrayList<GossipMember>).parallelStream()
                    .forEach { remoteMember ->
                        val entry = memberList.entries.find { it.key == remoteMember }
                        if (entry == null) {
                            val deadMember = deadList.find { it == remoteMember }
                            if (deadMember == null) {
                                // New node, add it
                                val newMember = GossipMember(
                                        remoteMember.host,
                                        remoteMember.port,
                                        remoteMember.heartbeat,
                                        remoteMember.clusterName)
                                val timer = GossipTimer(newMember, onMemberDead)
                                synchronized(memberList) {
                                    memberList.put(newMember, timer)
                                }
                                timer.start()
                            } else {
                                // Its a dead node, check if it was brought up
                                if (remoteMember.heartbeat > deadMember.heartbeat) {
                                    // The node is back up, remove it from the dead list
                                    synchronized(deadList) {
                                        deadList.remove(deadMember)
                                    }

                                    // Add it.
                                    val newMember = GossipMember(
                                            remoteMember.host,
                                            remoteMember.port,
                                            remoteMember.heartbeat,
                                            remoteMember.clusterName)
                                    val timer = GossipTimer(newMember, onMemberDead)
                                    synchronized(memberList) {
                                        memberList.put(newMember, timer)
                                    }
                                    timer.start()
                                }
                            }
                        } else {
                            val member = entry.key
                            val timer = entry.value
                            // Check if its heartbeat was updated, and update it if so.
                            if (remoteMember.heartbeat > member.heartbeat) {
                                member.heartbeat = remoteMember.heartbeat
                                timer.reset()
                            }
                        }
                    }
        }
    }

    /**
     * Run the service.
     * Watch out this function will create daemon coroutines!
     */
    fun run() {
        // Shutdown last session if there is one.
        shutdown()

        // Turn timers on. Not this attached member tho.
        memberList
                .filter { (member, _) -> member != me }
                .forEach { (_, timer) -> timer.start() }

        // Start coroutines for outgoing and incoming gossipings
        outgoingGossipsCoroutine = launch { OutgoingGossipRunnable(this@GossipService)() }
        incomingGossipsCoroutine = launch { IncomingGossipRunnable(this@GossipService)() }
    }

    /**
     * Shutdown the service
     */
    fun shutdown() {
        // Turn timers off
        memberList.forEach { _, timer -> timer.stop() }

        // Stop coroutines
        if (outgoingGossipsCoroutine != null && outgoingGossipsCoroutine!!.isActive) {
            // Since its a daemon, we cant join. We interrupt it
            outgoingGossipsCoroutine!!.cancel()
        }
        if (incomingGossipsCoroutine != null && incomingGossipsCoroutine!!.isActive) {
            incomingGossipsCoroutine!!.cancel()
        }
    }

}