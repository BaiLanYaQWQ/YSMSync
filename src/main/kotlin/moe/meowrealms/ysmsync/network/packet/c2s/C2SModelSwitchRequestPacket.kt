package moe.meowrealms.ysmsync.network.packet.c2s

import moe.meowrealms.ysmsync.network.EnumDirection
import moe.meowrealms.ysmsync.network.packet.Packet
import moe.meowrealms.ysmsync.network.packet.PacketHandler
import moe.meowrealms.ysmsync.utils.SimpleFriendlyByteBuf

class C2SModelSwitchRequestPacket(
    var modelId: String,
    var textureId: String
) : Packet{
    override fun direction(): EnumDirection {
        return EnumDirection.C_T_S
    }

    override fun handle(handler: PacketHandler) {
        handler.handleModelSwitchRequest(this)
    }

    override fun read(buffer: SimpleFriendlyByteBuf) {
        this.modelId = buffer.readUtf()
        this.textureId = buffer.readUtf()
    }

    override fun write(buffer: SimpleFriendlyByteBuf) {
        buffer.writeUtf(this.modelId)
        buffer.writeUtf(this.textureId)
    }
}