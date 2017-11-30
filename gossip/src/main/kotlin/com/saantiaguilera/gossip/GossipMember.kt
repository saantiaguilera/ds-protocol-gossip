package com.saantiaguilera.gossip

/**
 * GossipMember is a data holder class that defines a member (node) that is able to gossip with others to check
 * availability / status / failures / etc.
 */
data class GossipMember(val host: String, val port: Int, @Volatile var heartbeat: Long, val clusterName: String) {

    val address get() = "$host:$port"

    /**
     * Equals should only rely on host/port/clusterName
     * Since the heartbeat will be modified when gossiping (either merged with a gossiped member to aquire a higher
     * heartbeat, or simply increment it by one because of the gossiping interval) we cant rely on the equals for it
     * We also make it volatile so it gets WB when it changes in a threads cache
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is GossipMember) {
            return false
        }

        return address == other.address &&
                clusterName == other.clusterName
    }

}