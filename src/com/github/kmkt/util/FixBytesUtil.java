package com.github.kmkt.util;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * 固定長の多倍長 unsigned 整数の四則演算・論理演算や比較などを行うユーティリティクラス
 *
 * License : MIT License
 */
public class FixBytesUtil {
    /**
     * int の下位1byteを byte として取り出す
     * @param i
     * @return i & 0xff
     */
    public static byte int2byte(int i) {
        return (byte) (i & 0x000000ff);
    }

    /**
     * byte を unsigned として int に変換
     * @param b
     * @return ((int) b & 0xff)
     */
    public static int byte2int(byte b) {
        return ((int) b & 0x000000ff);
    }

    public static byte[] fill(byte[] val, byte b) {
        if (val == null)
            throw new IllegalArgumentException("val should not be null");

        Arrays.fill(val, b);
        return val;
    }

    /**
     * a & b
     * @param a
     * @param b
     * @return
     */
    public static byte[] and(byte[] a, byte[] b) {
        if (a == null)
            throw new IllegalArgumentException("a should not be null");
        if (b == null)
            throw new IllegalArgumentException("b should not be null");
        if (a.length != b.length)
            throw new IllegalArgumentException("byte array length should be same");

        int len = a.length;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = (byte) ((a[i] & b[i]) & 0x000000ff);
        }

        return result;
    }

    /**
     * a | b
     * @param a
     * @param b
     * @return
     */
    public static byte[] or(byte[] a, byte[] b) {
        if (a == null)
            throw new IllegalArgumentException("a should not be null");
        if (b == null)
            throw new IllegalArgumentException("b should not be null");
        if (a.length != b.length)
            throw new IllegalArgumentException("byte array length should be same");

        int len = a.length;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = (byte) ((a[i] | b[i]) & 0x000000ff);
        }

        return result;
    }

    /**
     * a ^ b
     * @param a
     * @param b
     * @return
     */
    public static byte[] xor(byte[] a, byte[] b) {
        if (a == null)
            throw new IllegalArgumentException("a should not be null");
        if (b == null)
            throw new IllegalArgumentException("b should not be null");
        if (a.length != b.length)
            throw new IllegalArgumentException("byte array length should be same");

        int len = a.length;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = (byte) ((a[i] ^ b[i]) & 0x000000ff);
        }

        return result;
    }

    /**
     * ~val
     * @param val
     * @return
     */
    public static byte[] not(byte[] val) {
        if (val == null)
            throw new IllegalArgumentException("val should not be null");

        int len = val.length;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = (byte) ((~val[i]) & 0x000000ff);
        }

        return result;
    }

    /**
     * a + b
     * @param a
     * @param b
     * @return
     */
    public static byte[] add(byte[] a, byte[] b) {
        if (a == null)
            throw new IllegalArgumentException("a should not be null");
        if (b == null)
            throw new IllegalArgumentException("b should not be null");
        if (a.length != b.length)
            throw new IllegalArgumentException("byte array length should be same");

        int len = a.length;
        byte[] result = new byte[len];
        int carry = 0;
        for (int i = len - 1; 0 <= i; i--) {
            int i1 = byte2int(a[i]);   // 0 <= i1 <= 255(0xff)
            int i2 = byte2int(b[i]);   // 0 <= i2 <= 255(0xff)
            int col = i1 + i2 + carry;
            carry = (col >> 8);
            result[i] = int2byte(col);
        }
        return result;
    }

    /**
     * a - b
     * @param a
     * @param b
     * @return
     */
    public static byte[] subtract(byte[] a, byte[] b) {
        if (a == null)
            throw new IllegalArgumentException("b1 should not be null");
        if (b == null)
            throw new IllegalArgumentException("b2 should not be null");
        if (a.length != b.length)
            throw new IllegalArgumentException("byte array length should be same");

        int len = a.length;
        byte[] result = new byte[len];
        int borrow = 0;
        for (int i = len - 1; 0 <= i; i--) {
            int i1 = byte2int(a[i]);   // 0 <= i1 <= 255(0xff)
            int i2 = byte2int(b[i]);   // 0 <= i2 <= 255(0xff)
            int col = i1 - i2 + borrow;
            borrow = (col >> 8);       
            result[i] = int2byte(col);
        }
        return result;
    }

    /**
     * a++
     * @param a
     * @return
     */
    public static byte[] inc(byte[] a) {
        if (a == null)
            throw new IllegalArgumentException("a should not be null");

        int len = a.length;
        byte[] result = Arrays.copyOf(a, len);
        int carry = 1;
        for (int i = len - 1; 0 <= i; i--) {
            int i1 = byte2int(a[i]);    // 0 <= i1 <= 255(0xff)
            int col = i1 + carry;
            carry = (col >> 8);
            result[i] = int2byte(col);

            if (carry == 0)             // carry が 0 になれば以降は省略できる
                break;
        }
        return result;
    }

    /**
     * a--
     * @param a
     * @return
     */
    public static byte[] dec(byte[] a) {
        if (a == null)
            throw new IllegalArgumentException("a should not be null");

        int len = a.length;
        byte[] result = Arrays.copyOf(a, len);
        int borrow = -1;
        for (int i = len - 1; 0 <= i; i--) {
            int i1 = byte2int(a[i]);    // 0 <= i1 <= 255(0xff)
            int col = i1 + borrow;
            borrow = (col >> 8);
            result[i] = int2byte(col);

            if (borrow == 0)    // 一度 borrow が 0 になれば以降は省略できる
                break;
        }
        return result;
    }

    /**
     * a * b
     * @param a
     * @param b
     * @return
     */
    public static byte[] multiply(byte[] a, byte b) {
        if (a == null)
            throw new IllegalArgumentException("a should not be null");

        int len = a.length;
        byte[] result = new byte[len];
        int carry = 0;
        int i2 = byte2int(b);
        for (int i = len - 1; 0 <= i; i--) {
            int i1 = byte2int(a[i]);   // 0 <= i1 <= 255(0xff)
            int col = i1 * i2 + carry;
            carry = (col >> 8);
            result[i] = int2byte(col);
        }
        return result;
    }


    /**
     * a * b
     * @param a
     * @param b
     * @return
     */
    public static byte[] multiply(byte[] a, byte[] b) {
        if (a == null)
            throw new IllegalArgumentException("a should not be null");
        if (b == null)
            throw new IllegalArgumentException("b should not be null");
        if (a.length != b.length)
            throw new IllegalArgumentException("byte array length should be same");

        int len = a.length;
        byte[] result = new byte[len];
        for (int offset = len - 1; 0 <= offset; offset--) {
            int i2 = byte2int(b[offset]);   // 0 <= i1 <= 255(0xff)
            int carry = 0;
            for (int i = len - 1; 0 <= i; i--) {
                int pos = offset - (len - 1 - i);
                if (pos < 0)
                    break;
                int i1 = byte2int(a[i]);   // 0 <= i1 <= 255(0xff)
                int r = byte2int(result[pos]);
                int col = r + i1 * i2 + carry;
                carry = (col >> 8);
                result[pos] = int2byte(col);
            }
        }
        return result;
    }

    /**
     * a / b
     * @param a
     * @param b
     * @return
     */
    public static byte[] divide(byte[] a, byte[] b) {
        if (a == null)
            throw new IllegalArgumentException("a should not be null");
        if (b == null)
            throw new IllegalArgumentException("b should not be null");
        if (isZero(b))
            throw new ArithmeticException("b should not be zero");

        byte[] result = new byte[a.length];
        if (isZero(a))
            return result;

        // XXX ひとまず BigInteger 使った実装
        BigInteger ba = new BigInteger(1, a);
        BigInteger bb = new BigInteger(1, b);
        BigInteger quotient = ba.divide(bb);

        byte[] val = quotient.toByteArray();
        for (int i = 0; i < result.length && i < val.length; i++) {
            int rpos = result.length - 1 - i;
            int vpos = val.length - 1 - i;
            result[rpos] = val[vpos];
        }

        return result;
    }

    /**
     * a % b
     * @param a
     * @param b
     * @return
     */
    public static byte[] remainder(byte[] a, byte[] b) {
        if (a == null)
            throw new IllegalArgumentException("a should not be null");
        if (b == null)
            throw new IllegalArgumentException("b should not be null");
        if (isZero(b))
            throw new ArithmeticException("b should not be zero");

        byte[] result = new byte[a.length];
        if (isZero(a))
            return result;

                    
        // XXX ひとまず BigInteger 使った実装
        BigInteger ba = new BigInteger(1, a);
        BigInteger bb = new BigInteger(1, b);
        BigInteger remainder = ba.remainder(bb);

        byte[] val = remainder.toByteArray();
        for (int i = 0; i < result.length && i < val.length; i++) {
            int rpos = result.length - 1 - i;
            int vpos = val.length - 1 - i;
            result[rpos] = val[vpos];
        }

        return result;
    }


    public static double percentageOf(byte[] a) {
        byte[] b = new byte[a.length+1];
        b[0] = 1;
        BigInteger v1 = new BigInteger(1, a);
        BigInteger v2 = new BigInteger(1, b);
        double vv1 = v1.doubleValue();
        double vv2 = v2.doubleValue();
        return vv1 / vv2;
    }

    /**
     * is zero
     * @param a
     * @return
     */
    public static boolean isZero(byte[] a) {
        if (a == null)
            throw new IllegalArgumentException("a should not be null");

        for (byte b : a) {
            if (b != 0)
                return false;
        }
        return true;
    }

    /**
     * Compare a and b
     * @param a
     * @param b
     * @return 0 a == b, negative a < b, positive a > b
     */
    public static int compare(byte[] a, byte[] b) {
        if (a == null)
            throw new IllegalArgumentException("a should not be null");
        if (b == null)
            throw new IllegalArgumentException("b should not be null");
        if (a.length != b.length)
            throw new IllegalArgumentException("byte array length should be same");

        if (a == b)
            return 0;

        for (int i = 0; i < a.length; i++) {
            int i1 = byte2int(a[i]);
            int i2 = byte2int(b[i]);

            if (i1 != i2)
                return i1 - i2;
        }
        return 0;
    }
}
