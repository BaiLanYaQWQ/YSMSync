package moe.meowrealms.ysmsync.network.data

import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap
import it.unimi.dsi.fastutil.objects.Object2FloatMap
import moe.meowrealms.ysmsync.utils.SimpleFriendlyByteBuf
import java.util.function.Consumer

data class MolangSyncData(
    var modelHash: Int,
    var variables: Object2FloatMap<String> = Object2FloatArrayMap(),
    var entityId: Int
) {
    fun write(buf: SimpleFriendlyByteBuf) {
        buf.writeInt(this.modelHash)
        buf.writeVarInt(this.entityId)
        buf.writeByte(this.variables.size)

        this.variables.object2FloatEntrySet().forEach(Consumer { entry: Object2FloatMap.Entry<String> ->
            buf.writeUtf(entry.key!!)
            buf.writeFloat(entry.floatValue)
        })
    }

    companion object {
        fun readFromBuf(buf: SimpleFriendlyByteBuf): MolangSyncData {
            val variableTable: Object2FloatArrayMap<String>

            val modelHash: Int = buf.readInt()
            val entityId: Int = buf.readVarInt()
            val variableTableSize: Int = buf.readByte().toInt()

            val variables = arrayOfNulls<String>(variableTableSize)
            val variableValues = FloatArray(variableTableSize)

            for (i in 0..< variableTableSize) {
                variables[i] = buf.readUtf()
                variableValues[i] = buf.readFloat()
            }

            variableTable = Object2FloatArrayMap<String>(variables, variableValues)

            return MolangSyncData(modelHash, variableTable, entityId)
        }
    }
}