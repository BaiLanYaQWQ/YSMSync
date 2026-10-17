package moe.meowrealms.ysmsync.network.data

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool
import it.unimi.dsi.fastutil.ints.Int2FloatArrayMap
import it.unimi.dsi.fastutil.ints.Int2FloatMap
import it.unimi.dsi.fastutil.ints.Int2FloatMaps
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap
import it.unimi.dsi.fastutil.objects.*
import moe.meowrealms.ysmsync.utils.SimpleFriendlyByteBuf
import org.bukkit.potion.PotionEffectType

@SuppressWarnings("removal")
class DispatchServerDrivenProperty(var entityId: Int) {
    var variant: Short = 0
    var flying: Boolean = false
    var effects: Object2ByteMap<PotionEffectType>? = null
    var expLevel: Int = 0
    var foodLevel: Int = 0
    var health: Int = 0
    var maxHealth: Int = 0
    var xxa: Byte = 0
    var yya: Byte = 0
    var zza: Byte = 0
    var inShieldBlockCooldown: Boolean = false
    var extraAnimation: String = ""
    private var modelHashShort: Int = 0
    var molangVarsServerBound: Object2FloatMap<String>? = null
    var molangVarsClientBound: Int2FloatMap? = null

    var tickPacketSyncRequired: Boolean = false

    fun isEmpty() = variant == 0.toShort()

    fun isFull() = (variant.toInt() and 1) != 0

    fun setFull() {
        variant = (variant.toInt() or 1).toShort()
    }

    fun clear(entityId: Int) {
        this.entityId = entityId
        variant = 0
        effects = null
        molangVarsServerBound = null
    }

    fun flying(flying: Boolean) = apply {
        variant = (variant.toInt() or (1 shl 1)).toShort()
        if (this.flying != flying) {
            this.tickPacketSyncRequired = true
        }
        this.flying = flying
    }

    fun addEffect(effect: PotionEffectType, level: Int) = apply {
        variant = (variant.toInt() or (1 shl 2)).toShort()
        effects = when {
            effects == null -> Object2ByteMaps.singleton(effect, level.toByte())
            effects!!.size == 1 -> {
                val newMap = Object2ByteOpenHashMap<PotionEffectType>(effects)
                newMap.put(effect, level.toByte())
                newMap
            }
            else -> {
                effects!!.put(effect, level.toByte())
                effects
            }
        }
    }

    fun allEffect(effects: Object2ByteMap<PotionEffectType>) = apply {
        variant = (variant.toInt() or (1 shl 2)).toShort()
        this.effects = effects
    }

    fun removeEffect(effect: PotionEffectType) = addEffect(effect, 0)

    fun expLevel(expLevel: Int) = apply {
        variant = (variant.toInt() or (1 shl 3)).toShort()
        if (this.expLevel != expLevel) {
            this.tickPacketSyncRequired = true
        }
        this.expLevel = expLevel
    }

    fun foodLevel(foodLevel: Int) = apply {
        variant = (variant.toInt() or (1 shl 4)).toShort()
        if (this.foodLevel != foodLevel) {
            this.tickPacketSyncRequired = true
        }
        this.foodLevel = foodLevel
    }

    fun health(health: Int) = apply {
        variant = (variant.toInt() or (1 shl 5)).toShort()
        if (this.health != health) {
            this.tickPacketSyncRequired = true
        }
        this.health = health
    }

    fun maxHealth(maxHealth: Int) = apply {
        variant = (variant.toInt() or (1 shl 6)).toShort()
        this.maxHealth = maxHealth
    }

    fun xxa(xxa: Float) = apply {
        variant = (variant.toInt() or (1 shl 7)).toShort()
        this.xxa = (xxa * 127).toInt().toByte()
    }

    fun yya(yya: Float) = apply {
        variant = (variant.toInt() or (1 shl 8)).toShort()
        this.yya = (yya * 127).toInt().toByte()
    }

    fun zza(zza: Float) = apply {
        variant = (variant.toInt() or (1 shl 9)).toShort()
        this.zza = (zza * 127).toInt().toByte()
    }

    fun inShieldBlockCooldown(inShieldBlockCooldown: Boolean) = apply {
        variant = (variant.toInt() or (1 shl 10)).toShort()
        this.inShieldBlockCooldown = inShieldBlockCooldown
    }

    fun extraAnimation(animation: String) = apply {
        variant = (variant.toInt() or (1 shl 11)).toShort()
        this.extraAnimation = animation
    }

    fun molangVars(modelHashShort: Int, molangVars: Object2FloatMap<String>) = apply {
        if (molangVarsServerBound == null || this.modelHashShort != modelHashShort) {
            variant = (variant.toInt() or (1 shl 12)).toShort()
            this.modelHashShort = modelHashShort
            this.molangVarsServerBound = Object2FloatOpenHashMap(molangVars)
        } else {
            this.molangVarsServerBound!!.putAll(molangVars)
        }
    }

    companion object {
        @JvmStatic
        fun encode(msg: DispatchServerDrivenProperty, buf: SimpleFriendlyByteBuf) {
            buf.writeVarInt(msg.entityId)
            buf.writeShort(msg.variant.toInt())

            val variant = msg.variant
            if ((variant.toInt() and (1 shl 1)) != 0) {
                buf.writeBoolean(msg.flying)
            }
            if ((variant.toInt() and (1 shl 2)) != 0) {
                val effects: Object2ByteMap<PotionEffectType> = msg.effects!!
                buf.writeVarInt(effects.size)
                Object2ByteMaps.fastForEach(effects) { entry ->
                    buf.writeVarInt(entry.key.id)
                    buf.writeByte(entry.byteValue.toInt())
                }
            }
            if ((variant.toInt() and (1 shl 3)) != 0) {
                buf.writeVarInt(msg.expLevel)
            }
            if ((variant.toInt() and (1 shl 4)) != 0) {
                buf.writeVarInt(msg.foodLevel)
            }
            if ((variant.toInt() and (1 shl 5)) != 0) {
                buf.writeVarInt(msg.health)
            }
            if ((variant.toInt() and (1 shl 6)) != 0) {
                buf.writeVarInt(msg.maxHealth)
            }
            if ((variant.toInt() and (1 shl 7)) != 0) {
                buf.writeByte(msg.xxa.toInt())
            }
            if ((variant.toInt() and (1 shl 8)) != 0) {
                buf.writeByte(msg.yya.toInt())
            }
            if ((variant.toInt() and (1 shl 9)) != 0) {
                buf.writeByte(msg.zza.toInt())
            }
            if ((variant.toInt() and (1 shl 10)) != 0) {
                buf.writeBoolean(msg.inShieldBlockCooldown)
            }
            if ((variant.toInt() and (1 shl 11)) != 0) {
                buf.writeUtf(msg.extraAnimation)
            }
            if ((variant.toInt() and (1 shl 12)) != 0) {
                buf.writeInt(msg.modelHashShort)
                val molangVars = msg.molangVarsServerBound!!
                buf.writeVarInt(molangVars.size)
                Object2FloatMaps.fastForEach(molangVars) { entry ->
                    buf.writeUtf(entry.key)
                    buf.writeFloat(entry.floatValue)
                }
            }
        }

        @JvmStatic
        fun decode(buf: SimpleFriendlyByteBuf): DispatchServerDrivenProperty {
            val entityId = buf.readVarInt()
            val variant = buf.readShort()
            val msg = DispatchServerDrivenProperty(entityId)
            msg.variant = variant

            if ((variant.toInt() and (1 shl 1)) != 0) {
                msg.flying = buf.readBoolean()
            }
            if ((variant.toInt() and (1 shl 2)) != 0) {
                val effectSize = buf.readVarInt()
                msg.effects = when (effectSize) {
                    0 -> Object2ByteMaps.emptyMap()
                    1 -> {
                        val effect = PotionEffectType.values()[buf.readVarInt()] // TODO : Check
                        val level = buf.readByte()
                        Object2ByteMaps.singleton(effect, level)
                    }
                    else -> {
                        val effectArray = arrayOfNulls<PotionEffectType>(effectSize)
                        val levelArray = ByteArray(effectSize)
                        for (i in 0 until effectSize) {
                            effectArray[i] = PotionEffectType.values()[buf.readVarInt()] // TODO : Check
                            levelArray[i] = buf.readByte()
                        }
                        Object2ByteArrayMap(effectArray, levelArray)
                    }
                }
            }
            if ((variant.toInt() and (1 shl 3)) != 0) {
                msg.expLevel = buf.readVarInt()
            }
            if ((variant.toInt() and (1 shl 4)) != 0) {
                msg.foodLevel = buf.readVarInt()
            }
            if ((variant.toInt() and (1 shl 5)) != 0) {
                msg.health = buf.readVarInt()
            }
            if ((variant.toInt() and (1 shl 6)) != 0) {
                msg.maxHealth = buf.readVarInt()
            }
            if ((variant.toInt() and (1 shl 7)) != 0) {
                msg.xxa = buf.readByte()
            }
            if ((variant.toInt() and (1 shl 8)) != 0) {
                msg.yya = buf.readByte()
            }
            if ((variant.toInt() and (1 shl 9)) != 0) {
                msg.zza = buf.readByte()
            }
            if ((variant.toInt() and (1 shl 10)) != 0) {
                msg.inShieldBlockCooldown = buf.readBoolean()
            }
            if ((variant.toInt() and (1 shl 11)) != 0) {
                msg.extraAnimation = buf.readUtf()
            }
            if ((variant.toInt() and (1 shl 12)) != 0) {
                msg.modelHashShort = buf.readInt()
                val size = buf.readVarInt()
                msg.molangVarsClientBound = if (msg.isFull()) {
                    val vars = Int2FloatOpenHashMap(size)
                    for (i in 0 until size) {
                        val name = StringPool.computeIfAbsent(buf.readUtf())
                        val value = buf.readFloat()
                        vars.put(name, value)
                    }
                    vars
                } else {
                    when (size) {
                        0 -> Int2FloatMaps.EMPTY_MAP
                        1 -> {
                            val name = StringPool.computeIfAbsent(buf.readUtf())
                            val value = buf.readFloat()
                            Int2FloatMaps.singleton(name, value)
                        }
                        else -> {
                            val nameArray = IntArray(size)
                            val valueArray = FloatArray(size)
                            for (i in 0 until size) {
                                nameArray[i] = StringPool.computeIfAbsent(buf.readUtf())
                                valueArray[i] = buf.readFloat()
                            }
                            Int2FloatArrayMap(nameArray, valueArray)
                        }
                    }
                }
            }

            return msg
        }
    }
}