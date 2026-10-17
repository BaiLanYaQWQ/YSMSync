package moe.meowrealms.ysmsync.network.packet.c2s

import it.unimi.dsi.fastutil.floats.FloatArrayList
import moe.meowrealms.ysmsync.network.EnumDirection
import moe.meowrealms.ysmsync.network.packet.Packet
import moe.meowrealms.ysmsync.network.packet.PacketHandler
import moe.meowrealms.ysmsync.utils.SimpleFriendlyByteBuf

class C2SMolangExpressionValueSyncPacket (
    var expressionValues: FloatArrayList
) : Packet{
    override fun direction(): EnumDirection {
        return EnumDirection.C_T_S
    }

    override fun handle(handler: PacketHandler) {
        handler.handleMolangExpressionValueSyncPacket(this)
    }

    override fun read(buffer: SimpleFriendlyByteBuf) {
        this.expressionValues = FloatArrayList()

        val size = buffer.readByte().toInt()
        for (i in 0 until size) {
            this.expressionValues.add(buffer.readFloat())
        }
    }

    override fun write(buffer: SimpleFriendlyByteBuf) {
        buffer.writeByte(this.expressionValues.size)

        for (i in this.expressionValues) {
            buffer.writeFloat(i)
        }
    }
}