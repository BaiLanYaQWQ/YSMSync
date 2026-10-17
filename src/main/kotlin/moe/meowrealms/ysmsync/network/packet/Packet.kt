package moe.meowrealms.ysmsync.network.packet

import moe.meowrealms.ysmsync.network.EnumDirection
import moe.meowrealms.ysmsync.utils.SimpleFriendlyByteBuf

interface Packet {
    fun direction() : EnumDirection

    fun handle(handler: PacketHandler)

    fun read(buffer: SimpleFriendlyByteBuf)

    fun write(buffer: SimpleFriendlyByteBuf)
}