package sinc2.util;

/**
 * Static methods in this class converts between integers and the corresponding little endian byte arrays.
 *
 * @since 2.0
 */
public class LittleEndianIntIO {
    public static int byteArray2LeInt(byte[] encodedValue) {
        int value = (encodedValue[3] << (Byte.SIZE * 3));
        value |= (encodedValue[2] & 0xFF) << (Byte.SIZE * 2);
        value |= (encodedValue[1] & 0xFF) << (Byte.SIZE);
        value |= (encodedValue[0] & 0xFF);
        return value;
    }

    public static byte[] leInt2ByteArray(int value) {
        byte[] encodedValue = new byte[Integer.BYTES];
        encodedValue[3] = (byte) (value >> Byte.SIZE * 3);
        encodedValue[2] = (byte) (value >> Byte.SIZE * 2);
        encodedValue[1] = (byte) (value >> Byte.SIZE);
        encodedValue[0] = (byte) value;
        return encodedValue;
    }
}
