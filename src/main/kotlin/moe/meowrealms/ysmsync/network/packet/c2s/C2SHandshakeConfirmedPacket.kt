package moe.meowrealms.ysmsync.network.packet.c2s

import moe.meowrealms.ysmsync.network.ClientConnectionManager
import moe.meowrealms.ysmsync.network.EnumDirection
import moe.meowrealms.ysmsync.network.packet.Packet
import moe.meowrealms.ysmsync.network.packet.PacketHandler
import moe.meowrealms.ysmsync.utils.SimpleFriendlyByteBuf

class C2SHandshakeConfirmedPacket(
    var version: String = ClientConnectionManager.VERSION
): Packet {
    override fun direction(): EnumDirection {
        return EnumDirection.C_T_S
    }

    override fun handle(handler: PacketHandler) {
        handler.handleHandshakeConfirmed(this)
    }

    override fun read(buffer: SimpleFriendlyByteBuf) {
        this.version = buffer.readUtf()
    }

    override fun write(buffer: SimpleFriendlyByteBuf) {
        buffer.writeUtf(this.version)
    }
}