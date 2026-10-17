package moe.meowrealms.ysmsync.network.packet

import moe.meowrealms.ysmsync.YSMSyncMain
import java.util.function.Supplier
import kotlin.reflect.KClass

class PacketRegistry {
    private val registedId2Packets = mutableMapOf<Int, Supplier<Packet>>()
    private val registedPacket2Ids = mutableMapOf<KClass<out Packet>, Int>()

    fun register(id: Int, packetClazz: KClass<out Packet>, constructor: Supplier<Packet>) {
        this.registedId2Packets[id] = constructor
        this.registedPacket2Ids[packetClazz] = id

        YSMSyncMain.instance.slF4JLogger.info("Registered ysm packet : $packetClazz")
    }

    fun lookupAndConstruct(id: Int): Packet? {
        val constructor = this.registedId2Packets[id] ?: return null

        return constructor.get()
    }

    fun lookupForId(packet: Packet): Int {
        val lookup = this.registedPacket2Ids[packet::class] ?: return -1

        return lookup
    }
}