# Gossip

Gossip protocol implementation in Kotlin.

I do not recommend using this code in production environments, since its only for my own learning purpose (and trying out weird ideas)

If you wish to use it, feel free to distribute it / change it / whatever you please (I'll be glad this was useful for someone)

## Usage

Create the nodes
```kotlin
val members by lazy { ArrayList<GossipMember>() }
settings.forEach { memberData ->
  members.add(GossipMember(memberData.host, memberData.port, 0, memberData.clusterName))
}
```

Create a service for each of them
```kotlin
val services by lazy { ArrayList<GossipService>() }
members.forEach { services.add(GossipService(it)) }
```

Start services
```kotlin
services.forEach { it.start() }
```

Of course, you can inline most of these steps, I've simply split them so its understandable what im doing