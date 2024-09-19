package org.team100.lib.telemetry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

/**
 * Log data protocol 2
 * 
 * Data packets are lists of tuples:
 * 
 * * key (2 bytes)
 * * type (1 byte)
 * * value (varies)
 * 
 * I previously had a more complicated, terse, stateful protocol, but I think
 * it's worth a few bytes to be simpler.
 * 
 * This protocol is very simple: input tuples, write them to a buffer.
 * 
 * The type is included here so that the parser knows how to parse the value.
 * 
 * <pre>
 * DDDDDDDDKKTIIIIKKTIIIIKKTB
 * ^^^^^^^^                   timestamp
 *         ^^                 key = 16
 *           ^                type = 5 (int)
 *            ^^^^            int value = 1234 (4 bytes)
 *                ^^          key = 17
 *                  ^         type = 5 (int)
 *                   ^^^^     int value = 5678 (4 bytes)
 *                       ^^   key = 18
 *                         ^  type = 3 (bool)
 *                          ^ boolean value = true (1 byte)
 * </pre>
 * 
 * Note this is a stateful protocol within the scope of a single message,
 * because of the "current type". This implies that the message itself is
 * part of the state, so it is contained here. And because it would be
 * confusing to reuse, each instance is intended to be used once.
 */
public class UdpPrimitiveProtocol2 {

    private final ByteBuffer m_buffer;

    public UdpPrimitiveProtocol2(int bufferSize) {
        // m_buffer = ByteBuffer.allocate(bufferSize);
        // direct buffer goes faster out the network
        // TODO: make sure we reuse this buffer!
        m_buffer = ByteBuffer.allocateDirect(bufferSize);
        // big-endian is the default, but just to make it clear...
        m_buffer.order(ByteOrder.BIG_ENDIAN);
        m_buffer.putLong(UdpMetadataProtocol.timestamp); // timetstamp = 8 bytes
    }

    public UdpPrimitiveProtocol2() {
        this(UdpSender.MTU);
    }

    /** Return a buffer view of length equal to current position. */
    ByteBuffer trim() {
        return m_buffer.slice(0, m_buffer.position());
    }

    /** for testing */
    ByteBuffer buffer() {
        return m_buffer;
    }

    /** Clear the underlying buffer and rewrite the timestamp into it. */
    void clear() {
        m_buffer.clear();
        m_buffer.putLong(UdpMetadataProtocol.timestamp);
    }

    /** @return true if written */
    public boolean putLong(int key, long val) {
        return encodeLong(m_buffer, key, val) != 0;
    }

    /** @return true if written */
    public boolean putString(int key, String val) {
        return encodeString(m_buffer, key, val) != 0;
    }

    /** @return true if written */
    public boolean putInt(int key, int val) {
        return encodeInt(m_buffer, key, val) != 0;
    }

    /** @return true if written */
    public boolean putDouble(int key, double val) {
        return encodeDouble(m_buffer, key, val) != 0;
    }

    /** @return true if written */
    public boolean putBoolean(int key, boolean val) {
        return encodeBoolean(m_buffer, key, val) != 0;
    }

    /** @return true if written */
    public boolean putDoubleArray(int key, double[] val) {
        return encodeDoubleArray(m_buffer, key, val) != 0;
    }

    /**
     * Encode a type
     * 
     * <pre>
     * TT
     * ^^ value (2 bytes)
     * </pre>
     * 
     * @return length (2) if written, 0 if not
     */
    static int encodeType(ByteBuffer buf, UdpType val) {
        final int totalLength = 2;
        if (buf.remaining() < totalLength)
            return 0;
        buf.putChar((char) val.id); // 2 bytes
        return totalLength;
    }

    /**
     * <pre>
     * KKTb
     * ^^   key (2 bytes)
     *   ^  type (1 byte)
     *    ^ boolean value (1 byte)
     * </pre>
     */
    static int encodeBoolean(ByteBuffer buf, int key, boolean val) {
        final int totalLength = 4;
        if (buf.remaining() < totalLength)
            return 0;
        buf.putChar((char) key); // 2 bytes
        buf.put(UdpType.BOOLEAN.id); // type = 1 byte
        buf.put(val ? (byte) 1 : (byte) 0); // 1 byte
        return totalLength;
    }

    /**
     * <pre>
     * KKTdddddddd
     * ^^          key (2 bytes)
     *   ^         type (1 byte)
     *    ^^^^^^^^ double value (8 bytes)
     * </pre>
     */
    static int encodeDouble(ByteBuffer buf, int key, double val) {
        final int totalLength = 11;
        if (buf.remaining() < totalLength)
            return 0;
        buf.putChar((char) key); // 2 bytes
        buf.put(UdpType.DOUBLE.id); // type = 1 byte
        buf.putDouble(val); // 8 bytes
        return totalLength;
    }

    static double decodeDouble(ByteBuffer buf, int offset) {
        return buf.getDouble(offset);
    }

    /**
     * Note the maximum array length is not very long (approximately packet length
     * divided by 8).
     * 
     * <pre>
     * KKTldddddddddddddddd
     * ^^                   key (2 bytes)
     *   ^                  type (1 byte)
     *    ^                 array length (1 byte)
     *     ^^^^^^^^         double value 0
     *             ^^^^^^^^ double value 1
     * </pre>
     */
    static int encodeDoubleArray(ByteBuffer buf, int key, double[] val) {
        if (val.length > 255)
            throw new IllegalArgumentException();
        final int totalLength = 4 + val.length * 8;
        if (buf.remaining() < totalLength)
            return 0;
        buf.putChar((char) key); // 2 bytes
        buf.put(UdpType.DOUBLE_ARRAY.id); // type = 1 byte
        buf.put((byte) val.length); // 1 byte
        for (int i = 0; i < val.length; ++i) {
            buf.putDouble(val[i]); // 8 bytes
        }
        return totalLength;
    }

    /**
     * <pre>
     * KKTiiii
     * ^^      key (2 bytes)
     *   ^     type (1 byte)
     *    ^^^^ int value (4 bytes)
     * </pre>
     */
    static int encodeInt(ByteBuffer buf, int key, int val) {
        final int totalLength = 7;
        if (buf.remaining() < totalLength)
            return 0;
        buf.putChar((char) key); // 2 bytes
        buf.put(UdpType.INT.id); // type = 1 byte
        buf.putInt(val); // 4 bytes
        return totalLength;
    }

    /**
     * Note: try to avoid logging strings.
     * 
     * <pre>
     * KKLTssssssssssss
     * ^^               key (2 bytes)
     *   ^              type (1 byte)
     *    ^             string length (1 byte)
     *     ^^^^^^^^^^^^ string value (255 bytes max)
     * </pre>
     */
    static int encodeString(ByteBuffer buf, int key, String val) {
        final byte[] bytes = val.getBytes(StandardCharsets.US_ASCII);
        final int bytesLength = bytes.length;
        if (bytesLength > 255)
            throw new IllegalArgumentException();
        final int totalLength = 4 + bytesLength;
        if (buf.remaining() < totalLength)
            return 0;
        buf.putChar((char) key); // 2 bytes
        buf.put(UdpType.STRING.id); // type = 1 byte
        buf.put((byte) bytesLength); // 1 byte
        buf.put(bytes);
        return totalLength;
    }

    static int decodeString(ByteBuffer buf, BiConsumer<Integer, String> consumer) {

    }

    /**
     * Note: try to avoid logging long ints, they're needlessly ... long.
     * 
     * <pre>
     * KKTllllllll
     * ^^          key (2 bytes)
     *   ^         type (1 byte)
     *    ^^^^^^^^ long value (8 bytes)
     * </pre>
     */
    static int encodeLong(ByteBuffer buf, int key, long val) {
        final int totalLength = 11;
        if (buf.remaining() < totalLength)
            return 0;
        buf.putChar((char) key); // 2 bytes
        buf.put(UdpType.LONG.id); // type = 1 byte
        buf.putLong(val); // 8 bytes
        return totalLength;
    }

    static int decodeLong(ByteBuffer buf, BiConsumer<Integer, Long> consumer) {

    }

}
