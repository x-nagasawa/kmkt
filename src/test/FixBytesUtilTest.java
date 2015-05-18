package test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.github.kmkt.util.FixBytesUtil;

public class FixBytesUtilTest {
    @Test
    public void and() {
        byte[] b1 = new byte[]{0x00, 0x00};
        byte[] b2 = new byte[]{0x00, 0x00};
        byte[] except = new byte[]{(byte) 0x00, (byte) 0x00};

        assertArrayEquals(except, FixBytesUtil.and(b1, b2));

        b1 = new byte[]{0x55, 0x55};
        b2 = new byte[]{0x00, 0x00};
        except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.and(b1, b2));

        b1 = new byte[]{0x55, 0x55};
        b2 = new byte[]{0x05, 0x05};
        except = new byte[]{0x05, 0x05};

        assertArrayEquals(except, FixBytesUtil.and(b1, b2));

        b1 = new byte[]{0x55, 0x55};
        b2 = new byte[]{0x55, 0x55};
        except = new byte[]{0x55, 0x55};

        assertArrayEquals(except, FixBytesUtil.and(b1, b2));

        b1 = new byte[]{(byte) 0x55, (byte) 0x55};
        b2 = new byte[]{(byte) 0xaa, (byte) 0xaa};
        except = new byte[]{(byte) 0x00, (byte) 0x00};

        assertArrayEquals(except, FixBytesUtil.and(b1, b2));
    }

    @Test
    public void or() {
        byte[] b1 = new byte[]{0x00, 0x00};
        byte[] b2 = new byte[]{0x00, 0x00};
        byte[] except = new byte[]{(byte) 0x00, (byte) 0x00};

        assertArrayEquals(except, FixBytesUtil.or(b1, b2));

        b1 = new byte[]{0x55, 0x55};
        b2 = new byte[]{0x00, 0x00};
        except = new byte[]{0x55, 0x55};

        assertArrayEquals(except, FixBytesUtil.or(b1, b2));

        b1 = new byte[]{0x55, 0x55};
        b2 = new byte[]{0x05, 0x05};
        except = new byte[]{0x55, 0x55};

        assertArrayEquals(except, FixBytesUtil.or(b1, b2));

        b1 = new byte[]{0x55, 0x55};
        b2 = new byte[]{0x55, 0x55};
        except = new byte[]{0x55, 0x55};

        assertArrayEquals(except, FixBytesUtil.or(b1, b2));

        b1 = new byte[]{(byte) 0x55, (byte) 0x55};
        b2 = new byte[]{(byte) 0xaa, (byte) 0xaa};
        except = new byte[]{(byte) 0xff, (byte) 0xff};

        assertArrayEquals(except, FixBytesUtil.or(b1, b2));
    }

    @Test
    public void xor() {
        byte[] b1 = new byte[]{0x00, 0x00};
        byte[] b2 = new byte[]{0x00, 0x00};
        byte[] except = new byte[]{(byte) 0x00, (byte) 0x00};

        assertArrayEquals(except, FixBytesUtil.xor(b1, b2));

        b1 = new byte[]{0x55, 0x55};
        b2 = new byte[]{0x00, 0x00};
        except = new byte[]{0x55, 0x55};

        assertArrayEquals(except, FixBytesUtil.xor(b1, b2));

        b1 = new byte[]{0x55, 0x55};
        b2 = new byte[]{0x05, 0x05};
        except = new byte[]{0x50, 0x50};

        assertArrayEquals(except, FixBytesUtil.xor(b1, b2));

        b1 = new byte[]{0x55, 0x55};
        b2 = new byte[]{0x55, 0x55};
        except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.xor(b1, b2));

        b1 = new byte[]{(byte) 0x55, (byte) 0x55};
        b2 = new byte[]{(byte) 0xaa, (byte) 0xaa};
        except = new byte[]{(byte) 0xff, (byte) 0xff};

        assertArrayEquals(except, FixBytesUtil.xor(b1, b2));
    }

    @Test
    public void not() {
        byte[] val = new byte[]{0x00, 0x00};
        byte[] except = new byte[]{(byte) 0xff, (byte) 0xff};

        assertArrayEquals(except, FixBytesUtil.not(val));

        val = new byte[]{(byte) 0xff, (byte) 0xff};
        except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.not(val));

        val = new byte[]{(byte) 0x55, (byte) 0x55};
        except = new byte[]{(byte) 0xaa, (byte) 0xaa};

        assertArrayEquals(except, FixBytesUtil.not(val));
    }

    @Test
    public void add() {
        byte[] b1 = new byte[]{0x00, 0x00};
        byte[] b2 = new byte[]{0x00, 0x01};
        byte[] except = new byte[]{0x00, 0x01};

        assertArrayEquals(except, FixBytesUtil.add(b1, b2));

        b1 = new byte[]{0x00, 0x01};
        b2 = new byte[]{0x00, 0x01};
        except = new byte[]{0x00, 0x02};

        assertArrayEquals(except, FixBytesUtil.add(b1, b2));

        b1 = new byte[]{0x00, (byte) 0xff};
        b2 = new byte[]{0x00, 0x00};
        except = new byte[]{0x00, (byte) 0xff};

        assertArrayEquals(except, FixBytesUtil.add(b1, b2));

        b1 = new byte[]{0x00, (byte) 0xff};
        b2 = new byte[]{0x00, 0x01};
        except = new byte[]{0x01, 0x00};

        assertArrayEquals(except, FixBytesUtil.add(b1, b2));

        b1 = new byte[]{0x00, (byte) 0xff};
        b2 = new byte[]{0x00, (byte) 0xff};
        except = new byte[]{0x01, (byte) 0xfe};

        assertArrayEquals(except, FixBytesUtil.add(b1, b2));

        b1 = new byte[]{(byte) 0xff, (byte) 0xff};
        b2 = new byte[]{0x00, 0x01};
        except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.add(b1, b2));

        b1 = new byte[]{(byte) 0xff, (byte) 0xff};
        b2 = new byte[]{0x00, 0x01};
        except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.add(b1, b2));

        b1 = new byte[]{(byte) 0xff, (byte) 0xff};
        b2 = new byte[]{(byte) 0xff, (byte) 0xff};
        except = new byte[]{(byte) 0xff, (byte) 0xfe};

        assertArrayEquals(except, FixBytesUtil.add(b1, b2));

        b1 = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        b2 = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        except = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xfe};

        assertArrayEquals(except, FixBytesUtil.add(b1, b2));
    }

    @Test
    public void subtract() {
        byte[] b1 = new byte[]{0x00, 0x00};
        byte[] b2 = new byte[]{0x00, 0x00};
        byte[] except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.subtract(b1, b2));

        b1 = new byte[]{0x00, (byte) 0xff};
        b2 = new byte[]{0x00, 0x01};
        except = new byte[]{0x00, (byte) 0xfe};

        assertArrayEquals(except, FixBytesUtil.subtract(b1, b2));

        b1 = new byte[]{0x01, (byte) 0x00};
        b2 = new byte[]{0x00, 0x01};
        except = new byte[]{0x00, (byte) 0xff};

        assertArrayEquals(except, FixBytesUtil.subtract(b1, b2));

        b1 = new byte[]{0x00, 0x00};
        b2 = new byte[]{0x00, 0x01};
        except = new byte[]{(byte) 0xff, (byte) 0xff};

        assertArrayEquals(except, FixBytesUtil.subtract(b1, b2));

        b1 = new byte[]{(byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        b2 = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01};
        except = new byte[]{(byte) 0x0f, (byte) 0xff, (byte) 0xfe, (byte) 0xff};

        assertArrayEquals(except, FixBytesUtil.subtract(b1, b2));
    }

    @Test
    public void inc() {
        byte[] b = new byte[]{0x00, 0x00};
        byte[] except = new byte[]{0x00, 0x01};

        assertArrayEquals(except, FixBytesUtil.inc(b));

        b = new byte[]{0x00, (byte) 0xff};
        except = new byte[]{0x01, 0x00};

        assertArrayEquals(except, FixBytesUtil.inc(b));

        b = new byte[]{(byte) 0xff, (byte) 0xff};
        except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.inc(b));
    }

    @Test
    public void dec() {
        byte[] b = new byte[]{0x00, 0x01};
        byte[] except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.dec(b));

        b = new byte[]{0x01, 0x00};
        except = new byte[]{0x00, (byte) 0xff};

        assertArrayEquals(except, FixBytesUtil.dec(b));

        b = new byte[]{0x00, 0x00};
        except = new byte[]{(byte) 0xff, (byte) 0xff};

        assertArrayEquals(except, FixBytesUtil.dec(b));
    }


    @Test
    public void mul1() {
        byte[] a = new byte[]{0x00, 0x01};
        byte b = 0;
        byte[] except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{0x00, 0x01};
        b = 2;
        except = new byte[]{0x00, 0x02};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{0x00, 0x01};
        b = (byte) 0xff;
        except = new byte[]{0x00, (byte) 0xff};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{0x00, (byte) 0x80};
        b = 2;
        except = new byte[]{0x01, 0x00};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{0x00, (byte) 0xff};
        b = 2;
        except = new byte[]{0x01, (byte) 0xfe};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{(byte) 0xff, (byte) 0xff};
        b = 2;
        except = new byte[]{(byte) 0xff, (byte) 0xfe};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{(byte) 0x10, (byte) 0x00};
        b = (byte) 0xff;
        except = new byte[]{(byte) 0xf0, (byte) 0x00};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));
    }
    
    @Test
    public void mul2() {
        byte[] a = new byte[]{0x00, 0x01};
        byte[] b = new byte[]{0x00, 0x00};
        byte[] except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{0x00, 0x01};
        b = new byte[]{0x00, 0x01};
        except = new byte[]{0x00, 0x01};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{0x00, 0x01};
        b = new byte[]{0x10, 0x00};
        except = new byte[]{0x10, 0x00};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{0x00, 0x08};
        b = new byte[]{0x10, 0x00};
        except = new byte[]{(byte) 0x80, 0x00};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{0x00, 0x10};
        b = new byte[]{0x10, 0x00};
        except = new byte[]{(byte) 0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{(byte) 0x00, (byte) 0xff};
        b = new byte[]{(byte) 0x00, (byte) 0xff};
        except = new byte[]{(byte) (byte) 0xfe, (byte) 0x01};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{(byte) 0xff, (byte) 0xff};
        b = new byte[]{(byte) 0xff, (byte) 0xff};
        except = new byte[]{(byte) (byte) 0x00, (byte) 0x01};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{(byte) 0x00, (byte) 0x00};
        b = new byte[]{(byte) 0xff, (byte) 0xff};
        except = new byte[]{(byte) (byte) 0x00, (byte) 0x00};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));

        a = new byte[]{(byte) 0x00, (byte) 0x04};
        b = new byte[]{(byte) 0xff, (byte) 0xff};
        except = new byte[]{(byte) (byte) 0xff, (byte) 0xfc};

        assertArrayEquals(except, FixBytesUtil.multiply(a, b));
    }

    @Test
    public void divide() {
        byte[] b1 = new byte[]{0x00, 0x00};
        byte[] b2 = new byte[]{0x00, 0x01};
        byte[] except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.divide(b1, b2));

        b1 = new byte[]{0x00, 0x01};
        b2 = new byte[]{0x00, 0x01};
        except = new byte[]{0x00, 0x01};

        assertArrayEquals(except, FixBytesUtil.divide(b1, b2));

        b1 = new byte[]{0x08, 0x01};
        b2 = new byte[]{0x00, 0x02};
        except = new byte[]{0x04, 0x00};

        assertArrayEquals(except, FixBytesUtil.divide(b1, b2));

        b1 = new byte[]{(byte) 0xff, (byte) 0xff};
        b2 = new byte[]{0x00, 0x02};
        except = new byte[]{0x7f, (byte) 0xff};

        assertArrayEquals(except, FixBytesUtil.divide(b1, b2));

        b1 = new byte[]{(byte) 0xff, (byte) 0xff};
        b2 = new byte[]{(byte) 0xff, 0x02};
        except = new byte[]{0x00, 0x01};

        assertArrayEquals(except, FixBytesUtil.divide(b1, b2));

        b1 = new byte[]{(byte) 0xff, (byte) 0x8f, (byte) 0xef, (byte) 0x21, (byte) 0xf8, (byte) 0x45};
        b2 = new byte[]{(byte) 0x0e, (byte) 0x28, (byte) 0x42, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        except = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x12};

        assertArrayEquals(except, FixBytesUtil.divide(b1, b2));
    }

    @Test
    public void remainder() {
        byte[] b1 = new byte[]{0x00, 0x00};
        byte[] b2 = new byte[]{0x00, 0x01};
        byte[] except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.remainder(b1, b2));

        b1 = new byte[]{0x00, 0x01};
        b2 = new byte[]{0x00, 0x01};
        except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.remainder(b1, b2));

        b1 = new byte[]{0x00, 0x01};
        b2 = new byte[]{0x00, 0x02};
        except = new byte[]{0x00, 0x01};

        assertArrayEquals(except, FixBytesUtil.remainder(b1, b2));

        b1 = new byte[]{(byte) 0xff, (byte) 0xfe};
        b2 = new byte[]{0x00, 0x02};
        except = new byte[]{0x00, 0x00};

        assertArrayEquals(except, FixBytesUtil.remainder(b1, b2));

        b1 = new byte[]{0x00, 0x07};
        b2 = new byte[]{0x00, 0x02};
        except = new byte[]{0x00, 0x01};

        assertArrayEquals(except, FixBytesUtil.remainder(b1, b2));

        b1 = new byte[]{0x10, 0x07};
        b2 = new byte[]{0x01, 0x02};
        except = new byte[]{0x00, (byte) 0xe9};

        assertArrayEquals(except, FixBytesUtil.remainder(b1, b2));
    }

    @Test
    public void isZero() {
        byte[] b = new byte[]{0x00, 0x00};

        assertTrue(FixBytesUtil.isZero(b));

        b = new byte[]{0x00, 0x01};

        assertFalse(FixBytesUtil.isZero(b));

        b = new byte[]{0x01, 0x01};

        assertFalse(FixBytesUtil.isZero(b));
    }


    @Test
    public void compare() {
        byte[] a = new byte[]{0x00, 0x00};
        byte[] b = new byte[]{0x00, 0x00};

        // a == b
        assertTrue(FixBytesUtil.compare(a, a) == 0);
        assertTrue(FixBytesUtil.compare(a, b) == 0);

        // a < b
        a = new byte[]{0x00, 0x00};
        b = new byte[]{0x00, 0x01};

        assertTrue(FixBytesUtil.compare(a, b) < 0);

        a = new byte[]{0x00, 0x00};
        b = new byte[]{(byte) 0xff, (byte) 0xff};

        assertTrue(FixBytesUtil.compare(a, b) < 0);

        a = new byte[]{0x00, 0x01};
        b = new byte[]{(byte) 0xff, (byte) 0xff};

        assertTrue(FixBytesUtil.compare(a, b) < 0);

        // b < a
        a = new byte[]{0x00, 0x01};
        b = new byte[]{0x00, 0x00};

        assertTrue(0 < FixBytesUtil.compare(a, b));

        a = new byte[]{(byte) 0xff, (byte) 0xff};
        b = new byte[]{0x00, 0x00};

        assertTrue(0 < FixBytesUtil.compare(a, b));

        a = new byte[]{(byte) 0xff, (byte) 0xff};
        b = new byte[]{0x00, 0x01};

        assertTrue(0 < FixBytesUtil.compare(a, b));
    }
}