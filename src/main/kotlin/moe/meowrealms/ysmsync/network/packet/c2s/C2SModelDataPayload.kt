package moe.meowrealms.ysmsync.network.packet.c2s

import moe.meowrealms.ysmsync.network.EnumDirection
import moe.meowrealms.ysmsync.network.packet.Packet
import moe.meowrealms.ysmsync.network.packet.PacketHandler
import moe.meowrealms.ysmsync.utils.SimpleFriendlyByteBuf

class C2SModelDataPayload(
    var payload: ByteArray
) : Packet {
    override fun direction(): EnumDirection {
        return EnumDirection.C_T_S
    }

    override fun handle(handler: PacketHandler) {
        handler.handleClientModelSyncPayload(this)
    }

    override fun read(buffer: SimpleFriendlyByteBuf) {
        this.payload = ByteArray(buffer.readableBytes())
        buffer.readBytes(payload)
    }

    override fun write(buffer: SimpleFriendlyByteBuf) {
        buffer.writeBytes(payload)
    }
}