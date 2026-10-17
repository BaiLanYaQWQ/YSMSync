package moe.meowrealms.ysmsync.network.packet.c2s

import moe.meowrealms.ysmsync.network.EnumDirection
import moe.meowrealms.ysmsync.network.packet.Packet
import moe.meowrealms.ysmsync.network.packet.PacketHandler
import moe.meowrealms.ysmsync.utils.SimpleFriendlyByteBuf

class C2SAnimationRequestPacket(
    var animationIndex: Int,
    var category: String,
    var entityId: Int
) : Packet{
    override fun direction(): EnumDirection {
        return EnumDirection.C_T_S
    }

    override fun handle(handler: PacketHandler) {
        handler.handleAnimationRequest(this)
    }

    override fun read(buffer: SimpleFriendlyByteBuf) {
        this.animationIndex = buffer.readVarInt()
        this.category = buffer.readUtf()
        this.entityId = buffer.readVarInt()
    }

    override fun write(buffer: SimpleFriendlyByteBuf) {
        buffer.writeVarInt(this.animationIndex)
        buffer.writeUtf(this.category)
        buffer.writeVarInt(this.entityId)
    }
}