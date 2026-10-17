package moe.meowrealms.ysmsync.utils

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2FloatMap
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

// TODO 这块让ai写的()
class FastutilMapAdapterFactory : TypeAdapterFactory {

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType

        if (Int2ObjectMap::class.java.isAssignableFrom(rawType)) {
            val valueType = getInt2ObjectValueType(type.type) ?: return null
            val valueAdapter = gson.getAdapter(TypeToken.get(valueType))
            return Int2ObjectMapAdapter(valueAdapter).nullSafe() as TypeAdapter<T>
        }

        if (Object2FloatMap::class.java.isAssignableFrom(rawType)) {
            val keyType = getObject2FloatKeyType(type.type) ?: return null
            val keyAdapter = gson.getAdapter(TypeToken.get(keyType))
            return Object2FloatMapAdapter(keyAdapter).nullSafe() as TypeAdapter<T>
        }

        return null
    }

    private fun getInt2ObjectValueType(type: Type): Type? {
        if (type is ParameterizedType) {
            val typeArgs = type.actualTypeArguments
            if (typeArgs.size == 1) {
                return typeArgs[0]
            }
        }
        return Any::class.java
    }

    private fun getObject2FloatKeyType(type: Type): Type? {
        if (type is ParameterizedType) {
            val typeArgs = type.actualTypeArguments
            if (typeArgs.size == 1) {
                return typeArgs[0]
            }
        }
        return Any::class.java
    }

    private class Int2ObjectMapAdapter<V>(private val valueAdapter: TypeAdapter<V>) :
        TypeAdapter<Int2ObjectMap<V>>() {

        override fun write(out: JsonWriter, map: Int2ObjectMap<V>?) {
            if (map == null) {
                out.nullValue()
                return
            }
            out.beginObject()
            for (entry in map.int2ObjectEntrySet()) {
                out.name(entry.intKey.toString())
                valueAdapter.write(out, entry.value)
            }
            out.endObject()
        }

        override fun read(`in`: JsonReader): Int2ObjectMap<V>? {
            if (`in`.peek() == com.google.gson.stream.JsonToken.NULL) {
                `in`.nextNull()
                return null
            }
            val map = Int2ObjectOpenHashMap<V>()
            `in`.beginObject()
            while (`in`.hasNext()) {
                val key = `in`.nextName().toIntOrNull() ?: continue
                val value = valueAdapter.read(`in`)
                map.put(key, value)
            }
            `in`.endObject()
            return map
        }
    }

    private class Object2FloatMapAdapter<K>(private val keyAdapter: TypeAdapter<K>) :
        TypeAdapter<Object2FloatMap<K>>() {

        override fun write(out: JsonWriter, map: Object2FloatMap<K>?) {
            if (map == null) {
                out.nullValue()
                return
            }
            out.beginObject()
            for (entry in map.object2FloatEntrySet()) {
                val keyStr = keyToString(keyAdapter, entry.key)
                out.name(keyStr)
                out.value(entry.floatValue.toDouble())
            }
            out.endObject()
        }

        override fun read(`in`: JsonReader): Object2FloatMap<K>? {
            if (`in`.peek() == com.google.gson.stream.JsonToken.NULL) {
                `in`.nextNull()
                return null
            }
            val map = Object2FloatOpenHashMap<K>()
            `in`.beginObject()
            while (`in`.hasNext()) {
                val keyStr = `in`.nextName()
                val value = `in`.nextDouble().toFloat()
                val key = stringToKey(keyAdapter, keyStr)
                map.put(key, value)
            }
            `in`.endObject()
            return map
        }

        private fun keyToString(keyAdapter: TypeAdapter<K>, key: K): String {
            return key.toString()
        }

        private fun stringToKey(keyAdapter: TypeAdapter<K>, str: String): K {
            return keyAdapter.fromJson("\"${str}\"")
        }
    }
}