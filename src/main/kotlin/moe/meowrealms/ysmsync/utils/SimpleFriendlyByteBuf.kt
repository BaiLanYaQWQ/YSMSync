package moe.meowrealms.ysmsync.utils
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.handler.codec.DecoderException
import io.netty.handler.codec.EncoderException
import io.netty.util.ByteProcessor
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.GatheringByteChannel
import java.nio.channels.ScatteringByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * A simplified FriendlyByteBuf reimplementation
 * Taken from minecraft-stress-test([...](https://github.com/PureGero/minecraft-stress-test))
 */
class SimpleFriendlyByteBuf(parent: ByteBuf) : ByteBuf() {
    private val source: ByteBuf = parent

    @JvmOverloads
    fun readVarIntArray(maxLength: Int = this.readableBytes()): IntArray {
        val varInt = this.readVarInt()
        if (varInt > maxLength) {
            throw DecoderException("VarIntArray with size $varInt is bigger than allowed $maxLength")
        } else {
            val ints = IntArray(varInt)

            for (i in ints.indices) {
                ints[i] = this.readVarInt()
            }

            return ints
        }
    }

    fun writeVarIntArray(array: IntArray): SimpleFriendlyByteBuf {
        this.writeVarInt(array.size)

        for (i in array) {
            this.writeVarInt(i)
        }

        return this
    }

    fun readVarInt(): Int {
        var i = 0
        var j = 0

        var b0: Byte

        do {
            b0 = this.readByte()
            i = i or ((b0.toInt() and 127) shl j++ * 7)
            if (j > 5) {
                throw RuntimeException("VarInt too big")
            }
        } while ((b0.toInt() and 128) == 128)

        return i
    }

    fun readVarLong(): Long {
        var i = 0L
        var j = 0

        var b0: Byte

        do {
            b0 = this.readByte()
            i = i or ((b0.toInt() and 127).toLong() shl j++ * 7)
            if (j > 10) {
                throw RuntimeException("VarLong too big")
            }
        } while ((b0.toInt() and 128) == 128)

        return i
    }

    fun writeUUID(uuid: UUID): SimpleFriendlyByteBuf {
        this.writeLong(uuid.mostSignificantBits)
        this.writeLong(uuid.leastSignificantBits)
        return this
    }

    fun readUUID(): UUID {
        return UUID(this.readLong(), this.readLong())
    }

    fun writeVarInt(value: Int): SimpleFriendlyByteBuf {
        var value = value
        while ((value and -128) != 0) {
            this.writeByte(value and 127 or 128)
            value = value ushr 7
        }

        this.writeByte(value)
        return this
    }

    fun writeVarLong(value: Long): SimpleFriendlyByteBuf {
        var value = value
        while ((value and -128L) != 0L) {
            this.writeByte((value and 127L).toInt() or 128)
            value = value ushr 7
        }

        this.writeByte(value.toInt())
        return this
    }

    @JvmOverloads
    fun readUtf(maxLength: Int = 32767): String {
        val j = this.readVarInt()

        if (j > maxLength * 4) {
            throw DecoderException("The received encoded string buffer length is longer than maximum allowed (" + j + " > " + maxLength * 4 + ")")
        } else if (j < 0) {
            throw DecoderException("The received encoded string buffer length is less than zero! Weird string!")
        } else {
            val s = this.toString(this.readerIndex(), j, StandardCharsets.UTF_8)

            this.readerIndex(this.readerIndex() + j)
            if (s.length > maxLength) {
                throw DecoderException("The received string length is longer than maximum allowed ($j > $maxLength)")
            } else {
                return s
            }
        }
    }

    @JvmOverloads
    fun writeUtf(string: String, maxLength: Int = 32767): SimpleFriendlyByteBuf {
        val abyte = string.toByteArray(StandardCharsets.UTF_8)

        if (abyte.size > maxLength) {
            throw EncoderException("String too big (was " + abyte.size + " bytes encoded, max " + maxLength + ")")
        } else {
            this.writeVarInt(abyte.size)
            this.writeBytes(abyte)
            return this
        }
    }

    override fun capacity(): Int {
        return this.source.capacity()
    }

    override fun capacity(i: Int): ByteBuf {
        return this.source.capacity(i)
    }

    override fun maxCapacity(): Int {
        return this.source.maxCapacity()
    }

    override fun alloc(): ByteBufAllocator {
        return this.source.alloc()
    }

    override fun order(): ByteOrder {
        return this.source.order()
    }

    override fun order(byteorder: ByteOrder?): ByteBuf {
        return this.source.order(byteorder)
    }

    override fun unwrap(): ByteBuf {
        return this.source.unwrap()
    }

    override fun isDirect(): Boolean {
        return this.source.isDirect
    }

    override fun isReadOnly(): Boolean {
        return this.source.isReadOnly
    }

    override fun asReadOnly(): ByteBuf {
        return this.source.asReadOnly()
    }

    override fun readerIndex(): Int {
        return this.source.readerIndex()
    }

    override fun readerIndex(i: Int): ByteBuf {
        return this.source.readerIndex(i)
    }

    override fun writerIndex(): Int {
        return this.source.writerIndex()
    }

    override fun writerIndex(i: Int): ByteBuf {
        return this.source.writerIndex(i)
    }

    override fun setIndex(i: Int, j: Int): ByteBuf {
        return this.source.setIndex(i, j)
    }

    override fun readableBytes(): Int {
        return this.source.readableBytes()
    }

    override fun writableBytes(): Int {
        return this.source.writableBytes()
    }

    override fun maxWritableBytes(): Int {
        return this.source.maxWritableBytes()
    }

    override fun isReadable(): Boolean {
        return this.source.isReadable
    }

    override fun isReadable(i: Int): Boolean {
        return this.source.isReadable(i)
    }

    override fun isWritable(): Boolean {
        return this.source.isWritable
    }

    override fun isWritable(i: Int): Boolean {
        return this.source.isWritable(i)
    }

    override fun clear(): ByteBuf {
        return this.source.clear()
    }

    override fun markReaderIndex(): ByteBuf {
        return this.source.markReaderIndex()
    }

    override fun resetReaderIndex(): ByteBuf {
        return this.source.resetReaderIndex()
    }

    override fun markWriterIndex(): ByteBuf {
        return this.source.markWriterIndex()
    }

    override fun resetWriterIndex(): ByteBuf {
        return this.source.resetWriterIndex()
    }

    override fun discardReadBytes(): ByteBuf {
        return this.source.discardReadBytes()
    }

    override fun discardSomeReadBytes(): ByteBuf {
        return this.source.discardSomeReadBytes()
    }

    override fun ensureWritable(i: Int): ByteBuf {
        return this.source.ensureWritable(i)
    }

    override fun ensureWritable(i: Int, flag: Boolean): Int {
        return this.source.ensureWritable(i, flag)
    }

    override fun getBoolean(i: Int): Boolean {
        return this.source.getBoolean(i)
    }

    override fun getByte(i: Int): Byte {
        return this.source.getByte(i)
    }

    override fun getUnsignedByte(i: Int): Short {
        return this.source.getUnsignedByte(i)
    }

    override fun getShort(i: Int): Short {
        return this.source.getShort(i)
    }

    override fun getShortLE(i: Int): Short {
        return this.source.getShortLE(i)
    }

    override fun getUnsignedShort(i: Int): Int {
        return this.source.getUnsignedShort(i)
    }

    override fun getUnsignedShortLE(i: Int): Int {
        return this.source.getUnsignedShortLE(i)
    }

    override fun getMedium(i: Int): Int {
        return this.source.getMedium(i)
    }

    override fun getMediumLE(i: Int): Int {
        return this.source.getMediumLE(i)
    }

    override fun getUnsignedMedium(i: Int): Int {
        return this.source.getUnsignedMedium(i)
    }

    override fun getUnsignedMediumLE(i: Int): Int {
        return this.source.getUnsignedMediumLE(i)
    }

    override fun getInt(i: Int): Int {
        return this.source.getInt(i)
    }

    override fun getIntLE(i: Int): Int {
        return this.source.getIntLE(i)
    }

    override fun getUnsignedInt(i: Int): Long {
        return this.source.getUnsignedInt(i)
    }

    override fun getUnsignedIntLE(i: Int): Long {
        return this.source.getUnsignedIntLE(i)
    }

    override fun getLong(i: Int): Long {
        return this.source.getLong(i)
    }

    override fun getLongLE(i: Int): Long {
        return this.source.getLongLE(i)
    }

    override fun getChar(i: Int): Char {
        return this.source.getChar(i)
    }

    override fun getFloat(i: Int): Float {
        return this.source.getFloat(i)
    }

    override fun getDouble(i: Int): Double {
        return this.source.getDouble(i)
    }

    override fun getBytes(i: Int, bytebuf: ByteBuf?): ByteBuf {
        return this.source.getBytes(i, bytebuf)
    }

    override fun getBytes(i: Int, bytebuf: ByteBuf?, j: Int): ByteBuf {
        return this.source.getBytes(i, bytebuf, j)
    }

    override fun getBytes(i: Int, bytebuf: ByteBuf?, j: Int, k: Int): ByteBuf {
        return this.source.getBytes(i, bytebuf, j, k)
    }

    override fun getBytes(i: Int, abyte: ByteArray?): ByteBuf {
        return this.source.getBytes(i, abyte)
    }

    override fun getBytes(i: Int, abyte: ByteArray?, j: Int, k: Int): ByteBuf {
        return this.source.getBytes(i, abyte, j, k)
    }

    override fun getBytes(i: Int, bytebuffer: ByteBuffer?): ByteBuf {
        return this.source.getBytes(i, bytebuffer)
    }

    @Throws(IOException::class)
    override fun getBytes(i: Int, outputstream: OutputStream?, j: Int): ByteBuf {
        return this.source.getBytes(i, outputstream, j)
    }

    @Throws(IOException::class)
    override fun getBytes(i: Int, gatheringbytechannel: GatheringByteChannel?, j: Int): Int {
        return this.source.getBytes(i, gatheringbytechannel, j)
    }

    @Throws(IOException::class)
    override fun getBytes(i: Int, filechannel: FileChannel?, j: Long, k: Int): Int {
        return this.source.getBytes(i, filechannel, j, k)
    }

    override fun getCharSequence(i: Int, j: Int, charset: Charset?): CharSequence {
        return this.source.getCharSequence(i, j, charset)
    }

    override fun setBoolean(i: Int, flag: Boolean): ByteBuf {
        return this.source.setBoolean(i, flag)
    }

    override fun setByte(i: Int, j: Int): ByteBuf {
        return this.source.setByte(i, j)
    }

    override fun setShort(i: Int, j: Int): ByteBuf {
        return this.source.setShort(i, j)
    }

    override fun setShortLE(i: Int, j: Int): ByteBuf {
        return this.source.setShortLE(i, j)
    }

    override fun setMedium(i: Int, j: Int): ByteBuf {
        return this.source.setMedium(i, j)
    }

    override fun setMediumLE(i: Int, j: Int): ByteBuf {
        return this.source.setMediumLE(i, j)
    }

    override fun setInt(i: Int, j: Int): ByteBuf {
        return this.source.setInt(i, j)
    }

    override fun setIntLE(i: Int, j: Int): ByteBuf {
        return this.source.setIntLE(i, j)
    }

    override fun setLong(i: Int, j: Long): ByteBuf {
        return this.source.setLong(i, j)
    }

    override fun setLongLE(i: Int, j: Long): ByteBuf {
        return this.source.setLongLE(i, j)
    }

    override fun setChar(i: Int, j: Int): ByteBuf {
        return this.source.setChar(i, j)
    }

    override fun setFloat(i: Int, f: Float): ByteBuf {
        return this.source.setFloat(i, f)
    }

    override fun setDouble(i: Int, d0: Double): ByteBuf {
        return this.source.setDouble(i, d0)
    }

    override fun setBytes(i: Int, bytebuf: ByteBuf?): ByteBuf {
        return this.source.setBytes(i, bytebuf)
    }

    override fun setBytes(i: Int, bytebuf: ByteBuf?, j: Int): ByteBuf {
        return this.source.setBytes(i, bytebuf, j)
    }

    override fun setBytes(i: Int, bytebuf: ByteBuf?, j: Int, k: Int): ByteBuf {
        return this.source.setBytes(i, bytebuf, j, k)
    }

    override fun setBytes(i: Int, abyte: ByteArray?): ByteBuf {
        return this.source.setBytes(i, abyte)
    }

    override fun setBytes(i: Int, abyte: ByteArray?, j: Int, k: Int): ByteBuf {
        return this.source.setBytes(i, abyte, j, k)
    }

    override fun setBytes(i: Int, bytebuffer: ByteBuffer?): ByteBuf {
        return this.source.setBytes(i, bytebuffer)
    }

    @Throws(IOException::class)
    override fun setBytes(i: Int, inputstream: InputStream?, j: Int): Int {
        return this.source.setBytes(i, inputstream, j)
    }

    @Throws(IOException::class)
    override fun setBytes(i: Int, scatteringbytechannel: ScatteringByteChannel?, j: Int): Int {
        return this.source.setBytes(i, scatteringbytechannel, j)
    }

    @Throws(IOException::class)
    override fun setBytes(i: Int, filechannel: FileChannel?, j: Long, k: Int): Int {
        return this.source.setBytes(i, filechannel, j, k)
    }

    override fun setZero(i: Int, j: Int): ByteBuf {
        return this.source.setZero(i, j)
    }

    override fun setCharSequence(i: Int, charsequence: CharSequence?, charset: Charset?): Int {
        return this.source.setCharSequence(i, charsequence, charset)
    }

    override fun readBoolean(): Boolean {
        return this.source.readBoolean()
    }

    override fun readByte(): Byte {
        return this.source.readByte()
    }

    override fun readUnsignedByte(): Short {
        return this.source.readUnsignedByte()
    }

    override fun readShort(): Short {
        return this.source.readShort()
    }

    override fun readShortLE(): Short {
        return this.source.readShortLE()
    }

    override fun readUnsignedShort(): Int {
        return this.source.readUnsignedShort()
    }

    override fun readUnsignedShortLE(): Int {
        return this.source.readUnsignedShortLE()
    }

    override fun readMedium(): Int {
        return this.source.readMedium()
    }

    override fun readMediumLE(): Int {
        return this.source.readMediumLE()
    }

    override fun readUnsignedMedium(): Int {
        return this.source.readUnsignedMedium()
    }

    override fun readUnsignedMediumLE(): Int {
        return this.source.readUnsignedMediumLE()
    }

    override fun readInt(): Int {
        return this.source.readInt()
    }

    override fun readIntLE(): Int {
        return this.source.readIntLE()
    }

    override fun readUnsignedInt(): Long {
        return this.source.readUnsignedInt()
    }

    override fun readUnsignedIntLE(): Long {
        return this.source.readUnsignedIntLE()
    }

    override fun readLong(): Long {
        return this.source.readLong()
    }

    override fun readLongLE(): Long {
        return this.source.readLongLE()
    }

    override fun readChar(): Char {
        return this.source.readChar()
    }

    override fun readFloat(): Float {
        return this.source.readFloat()
    }

    override fun readDouble(): Double {
        return this.source.readDouble()
    }

    override fun readBytes(i: Int): ByteBuf {
        return this.source.readBytes(i)
    }

    override fun readSlice(i: Int): ByteBuf {
        return this.source.readSlice(i)
    }

    override fun readRetainedSlice(i: Int): ByteBuf {
        return this.source.readRetainedSlice(i)
    }

    override fun readBytes(bytebuf: ByteBuf?): ByteBuf {
        return this.source.readBytes(bytebuf)
    }

    override fun readBytes(bytebuf: ByteBuf?, i: Int): ByteBuf {
        return this.source.readBytes(bytebuf, i)
    }

    override fun readBytes(bytebuf: ByteBuf?, i: Int, j: Int): ByteBuf {
        return this.source.readBytes(bytebuf, i, j)
    }

    override fun readBytes(abyte: ByteArray?): ByteBuf {
        return this.source.readBytes(abyte)
    }

    override fun readBytes(abyte: ByteArray?, i: Int, j: Int): ByteBuf {
        return this.source.readBytes(abyte, i, j)
    }

    override fun readBytes(bytebuffer: ByteBuffer?): ByteBuf {
        return this.source.readBytes(bytebuffer)
    }

    @Throws(IOException::class)
    override fun readBytes(outputstream: OutputStream?, i: Int): ByteBuf {
        return this.source.readBytes(outputstream, i)
    }

    @Throws(IOException::class)
    override fun readBytes(gatheringbytechannel: GatheringByteChannel?, i: Int): Int {
        return this.source.readBytes(gatheringbytechannel, i)
    }

    override fun readCharSequence(i: Int, charset: Charset?): CharSequence {
        return this.source.readCharSequence(i, charset)
    }

    @Throws(IOException::class)
    override fun readBytes(filechannel: FileChannel?, i: Long, j: Int): Int {
        return this.source.readBytes(filechannel, i, j)
    }

    override fun skipBytes(i: Int): ByteBuf {
        return this.source.skipBytes(i)
    }

    override fun writeBoolean(flag: Boolean): ByteBuf {
        return this.source.writeBoolean(flag)
    }

    override fun writeByte(i: Int): ByteBuf {
        return this.source.writeByte(i)
    }

    override fun writeShort(i: Int): ByteBuf {
        return this.source.writeShort(i)
    }

    override fun writeShortLE(i: Int): ByteBuf {
        return this.source.writeShortLE(i)
    }

    override fun writeMedium(i: Int): ByteBuf {
        return this.source.writeMedium(i)
    }

    override fun writeMediumLE(i: Int): ByteBuf {
        return this.source.writeMediumLE(i)
    }

    override fun writeInt(i: Int): ByteBuf {
        return this.source.writeInt(i)
    }

    override fun writeIntLE(i: Int): ByteBuf {
        return this.source.writeIntLE(i)
    }

    override fun writeLong(i: Long): ByteBuf {
        return this.source.writeLong(i)
    }

    override fun writeLongLE(i: Long): ByteBuf {
        return this.source.writeLongLE(i)
    }

    override fun writeChar(i: Int): ByteBuf {
        return this.source.writeChar(i)
    }

    override fun writeFloat(f: Float): ByteBuf {
        return this.source.writeFloat(f)
    }

    override fun writeDouble(d0: Double): ByteBuf {
        return this.source.writeDouble(d0)
    }

    override fun writeBytes(bytebuf: ByteBuf?): ByteBuf {
        return this.source.writeBytes(bytebuf)
    }

    override fun writeBytes(bytebuf: ByteBuf?, i: Int): ByteBuf {
        return this.source.writeBytes(bytebuf, i)
    }

    override fun writeBytes(bytebuf: ByteBuf?, i: Int, j: Int): ByteBuf {
        return this.source.writeBytes(bytebuf, i, j)
    }

    override fun writeBytes(abyte: ByteArray?): ByteBuf {
        return this.source.writeBytes(abyte!!)
    }

    override fun writeBytes(abyte: ByteArray?, i: Int, j: Int): ByteBuf {
        return this.source.writeBytes(abyte!!, i, j)
    }

    override fun writeBytes(bytebuffer: ByteBuffer?): ByteBuf {
        return this.source.writeBytes(bytebuffer)
    }

    @Throws(IOException::class)
    override fun writeBytes(inputstream: InputStream?, i: Int): Int {
        return this.source.writeBytes(inputstream, i)
    }

    @Throws(IOException::class)
    override fun writeBytes(scatteringbytechannel: ScatteringByteChannel?, i: Int): Int {
        return this.source.writeBytes(scatteringbytechannel, i)
    }

    @Throws(IOException::class)
    override fun writeBytes(filechannel: FileChannel?, i: Long, j: Int): Int {
        return this.source.writeBytes(filechannel, i, j)
    }

    override fun writeZero(i: Int): ByteBuf {
        return this.source.writeZero(i)
    }

    override fun writeCharSequence(charsequence: CharSequence?, charset: Charset?): Int {
        return this.source.writeCharSequence(charsequence, charset)
    }

    override fun indexOf(i: Int, j: Int, b0: Byte): Int {
        return this.source.indexOf(i, j, b0)
    }

    override fun bytesBefore(b0: Byte): Int {
        return this.source.bytesBefore(b0)
    }

    override fun bytesBefore(i: Int, b0: Byte): Int {
        return this.source.bytesBefore(i, b0)
    }

    override fun bytesBefore(i: Int, j: Int, b0: Byte): Int {
        return this.source.bytesBefore(i, j, b0)
    }

    override fun forEachByte(byteprocessor: ByteProcessor): Int {
        return this.source.forEachByte(byteprocessor)
    }

    override fun forEachByte(i: Int, j: Int, byteprocessor: ByteProcessor): Int {
        return this.source.forEachByte(i, j, byteprocessor)
    }

    override fun forEachByteDesc(byteprocessor: ByteProcessor): Int {
        return this.source.forEachByteDesc(byteprocessor)
    }

    override fun forEachByteDesc(i: Int, j: Int, byteprocessor: ByteProcessor): Int {
        return this.source.forEachByteDesc(i, j, byteprocessor)
    }

    override fun copy(): ByteBuf {
        return this.source.copy()
    }

    override fun copy(i: Int, j: Int): ByteBuf {
        return this.source.copy(i, j)
    }

    override fun slice(): ByteBuf {
        return this.source.slice()
    }

    override fun retainedSlice(): ByteBuf {
        return this.source.retainedSlice()
    }

    override fun slice(i: Int, j: Int): ByteBuf {
        return this.source.slice(i, j)
    }

    override fun retainedSlice(i: Int, j: Int): ByteBuf {
        return this.source.retainedSlice(i, j)
    }

    override fun duplicate(): ByteBuf {
        return this.source.duplicate()
    }

    override fun retainedDuplicate(): ByteBuf {
        return this.source.retainedDuplicate()
    }

    override fun nioBufferCount(): Int {
        return this.source.nioBufferCount()
    }

    override fun nioBuffer(): ByteBuffer {
        return this.source.nioBuffer()
    }

    override fun nioBuffer(i: Int, j: Int): ByteBuffer {
        return this.source.nioBuffer(i, j)
    }

    override fun internalNioBuffer(i: Int, j: Int): ByteBuffer {
        return this.source.internalNioBuffer(i, j)
    }

    override fun nioBuffers(): Array<ByteBuffer?> {
        return this.source.nioBuffers()
    }

    override fun nioBuffers(i: Int, j: Int): Array<ByteBuffer?> {
        return this.source.nioBuffers(i, j)
    }

    override fun hasArray(): Boolean {
        return this.source.hasArray()
    }

    override fun array(): ByteArray {
        return this.source.array()
    }

    override fun arrayOffset(): Int {
        return this.source.arrayOffset()
    }

    override fun hasMemoryAddress(): Boolean {
        return this.source.hasMemoryAddress()
    }

    override fun memoryAddress(): Long {
        return this.source.memoryAddress()
    }

    override fun toString(charset: Charset?): String {
        return this.source.toString(charset!!)
    }

    override fun toString(i: Int, j: Int, charset: Charset?): String {
        return this.source.toString(i, j, charset)
    }

    override fun hashCode(): Int {
        return this.source.hashCode()
    }

    override fun equals(`object`: Any?): Boolean {
        return this.source == `object`
    }

    override fun compareTo(bytebuf: ByteBuf?): Int {
        return this.source.compareTo(bytebuf)
    }

    override fun toString(): String {
        return this.source.toString()
    }

    override fun retain(i: Int): ByteBuf {
        return this.source.retain(i)
    }

    override fun retain(): ByteBuf {
        return this.source.retain()
    }

    override fun touch(): ByteBuf {
        return this.source.touch()
    }

    override fun touch(`object`: Any?): ByteBuf {
        return this.source.touch(`object`)
    }

    override fun refCnt(): Int {
        return this.source.refCnt()
    }

    override fun release(): Boolean {
        return this.source.release()
    }

    override fun release(i: Int): Boolean {
        return this.source.release(i)
    }

    val bytes: ByteArray
        get() {
            val bytes = ByteArray(this.source.readableBytes())
            this.source.readBytes(bytes)
            return bytes
        }

    companion object {
        fun getVarIntSize(value: Int): Int {
            for (j in 1..4) {
                if ((value and (-1 shl j * 7)) == 0) {
                    return j
                }
            }

            return 5
        }

        fun getVarLongSize(value: Long): Int {
            for (j in 1..9) {
                if ((value and (-1L shl j * 7)) == 0L) {
                    return j
                }
            }

            return 10
        }
    }
}