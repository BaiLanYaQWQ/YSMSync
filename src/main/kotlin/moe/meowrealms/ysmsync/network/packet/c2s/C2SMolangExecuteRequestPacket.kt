package moe.meowrealms.ysmsync.network.packet.c2s

import moe.meowrealms.ysmsync.network.EnumDirection
import moe.meowrealms.ysmsync.network.packet.Packet
import moe.meowrealms.ysmsync.network.packet.PacketHandler
import moe.meowrealms.ysmsync.utils.SimpleFriendlyByteBuf

class C2SMolangExecuteRequestPacket(
    var expression: String,
    var onEntityId: Int
) : Packet{
    override fun direction(): EnumDirection {
        return EnumDirection.C_T_S
    }

    override fun handle(handler: PacketHandler) {
        handler.handleMolangExecuteRequest(this)
    }

    override fun read(buffer: SimpleFriendlyByteBuf) {
        this.expression = buffer.readUtf()
        this.onEntityId = buffer.readVarInt()
    }

    override fun write(buffer: SimpleFriendlyByteBuf) {
        buffer.writeUtf(this.expression)
        buffer.writeVarInt(this.onEntityId)
    }
}