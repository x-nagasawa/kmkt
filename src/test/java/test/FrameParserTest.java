package test;

import java.nio.ByteBuffer;

import com.github.kmkt.util.FrameParser;

public class FrameParserTest {

    public static void main(String[] args) {
        byte[] buf = new byte[16];
        FrameParser parser = new FrameParserTst(ByteBuffer.wrap(buf));
        ByteBuffer buffer = parser.getByteBuffer();

        buffer.put(new byte[]{0,0});
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        buffer.put(new byte[]{0,0,(byte) 0xff});
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        buffer.put(new byte[]{0,0,(byte) 0xff, (byte) 0xff, (byte) 0xd8});
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

    
        buffer.put(new byte[]{0,0,(byte) 0xff, (byte) 0xff, (byte) 0xd9, 00});
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");
        
        buffer.put(new byte[]{0,0,(byte) 0xff, (byte) 0xff, (byte) 0xd8,(byte) 0xf0, (byte) 0xff, (byte) 0xd9, (byte) 0xff, (byte) 0xd8,(byte) 0xf1, (byte) 0xff, (byte) 0xd9, (byte) 0xff});
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");
        
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        buffer.put(new byte[]{(byte) 0xd8,(byte) 0xf2});

        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        buffer.put(new byte[]{(byte) 0xff});

        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        buffer.put(new byte[]{(byte) 0xd9, (byte) 0xff, (byte) 0xd8, 0, (byte) 0xff, (byte) 0xd9});

        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        buffer.put(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xd8});
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");
        
        buffer.put(new byte[]{1});
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        buffer.put(new byte[]{2});
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        buffer.put(new byte[]{(byte) 0xff});
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        buffer.put(new byte[]{(byte) 0xd9, (byte) 0xff});
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        buffer.put(new byte[]{(byte) 0xd8, 3, 4, 5});
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        System.out.println("["+Util.dump(parser.getFrame())+"]");

        buffer.put(new byte[]{(byte) 0xff});
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");

        buffer.put(new byte[]{(byte) 0xd9});
        System.out.println("["+Util.dump(parser.getFrame())+"]");
        System.out.print(parser.toString());
        System.out.println(" ["+Util.dump(buf)+"]");
    }

    static class FrameParserTst extends FrameParser {
        @Override
        public String toString() {
            ByteBuffer buff = getByteBuffer();
            return String.format("pos:%d lim:%d, cap:%d, search_pos:%d, soi_pos:%d", 
                    buff.position(), buff.limit(), buff.capacity(), getSearchPos(), getSOIPos());
        }

        public FrameParserTst(ByteBuffer buffer) {
            super(buffer);
        }

        public FrameParserTst(int bufsize) {
            super(bufsize);
        }


        /* BM like search
        if (b1 == (byte) 0xd8) {
            if (b0 == (byte) 0xff) {
                // hit b0, b1 -> 0xff, 0xd8
                pos_soi = serch_pos;
                break;
            } else {
                // miss b0, b1 -> !0xff, 0xd8
                serch_pos += 2;   // pos can move next to b1
                continue;
            }
        } else if (b1 == (byte) 0xff) {
            // miss b0, b1 -> ?, 0xff
            serch_pos++;   // pos set at b1
            continue;
        } else {
            // miss b0, b1 -> !0xff, !0xd8
            serch_pos += 2;   // pos can move next to b1
            continue;
        }
        */
    }
    
    static class Util {
        static String dump(byte[] buf) {
            if (buf == null)
                return "null";
            StringBuilder result = new StringBuilder();
            for (byte b : buf) {
                result.append(String.format("%02x ", b));
            }
            return result.toString();
        }
    }
}
