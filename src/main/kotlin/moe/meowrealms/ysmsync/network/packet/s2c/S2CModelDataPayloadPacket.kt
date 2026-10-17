package moe.meowrealms.ysmsync.network.packet.s2c

import moe.meowrealms.ysmsync.network.EnumDirection
import moe.meowrealms.ysmsync.network.packet.Packet
import moe.meowrealms.ysmsync.network.packet.PacketHandler
import moe.meowrealms.ysmsync.utils.SimpleFriendlyByteBuf

class S2CModelDataPayloadPacket(
    var payload: ByteArray
) : Packet {
    override fun direction(): EnumDirection {
        return EnumDirection.S_T_C
    }

    override fun handle(handler: PacketHandler) {
        TODO("Not implemented on server side")
    }

    override fun read(buffer: SimpleFriendlyByteBuf) {
        this.payload = ByteArray(buffer.readableBytes())
        buffer.readBytes(payload)
    }

    override fun write(buffer: SimpleFriendlyByteBuf) {
        buffer.writeBytes(payload)
    }
}