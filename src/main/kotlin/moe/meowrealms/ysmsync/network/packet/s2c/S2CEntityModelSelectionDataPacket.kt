package moe.meowrealms.ysmsync.network.packet.s2c

import moe.meowrealms.ysmsync.network.EnumDirection
import moe.meowrealms.ysmsync.network.data.DispatchServerDrivenProperty
import moe.meowrealms.ysmsync.network.packet.Packet
import moe.meowrealms.ysmsync.network.packet.PacketHandler
import moe.meowrealms.ysmsync.utils.SimpleFriendlyByteBuf

class S2CEntityModelSelectionDataPacket(
    var entityId: Int,
    var modelId: String,
    var textureId: String,
    var disabled: Boolean,
    var modelAnimationData: DispatchServerDrivenProperty
): Packet{
    override fun direction(): EnumDirection {
        return EnumDirection.S_T_C
    }

    override fun handle(handler: PacketHandler) {
        TODO("Not yet implemented on server side!")
    }

    override fun read(buffer: SimpleFriendlyByteBuf) {
        this.entityId = buffer.readVarInt()
        this.modelId = buffer.readUtf()
        this.textureId = buffer.readUtf()
        this.disabled = buffer.readBoolean()
        this.modelAnimationData = DispatchServerDrivenProperty.decode(buffer)
    }

    override fun write(buffer: SimpleFriendlyByteBuf) {
        buffer.writeVarInt(this.entityId)
        buffer.writeUtf(this.modelId)
        buffer.writeUtf(this.textureId)
        buffer.writeBoolean(this.disabled)
        DispatchServerDrivenProperty.encode(this.modelAnimationData, buffer)
    }
}