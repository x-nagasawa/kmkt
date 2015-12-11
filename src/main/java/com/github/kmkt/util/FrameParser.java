package com.github.kmkt.util;

import java.nio.ByteBuffer;

public class FrameParser {
    private final ByteBuffer buffer;
    private int search_pos = 0;
    private int soi_pos = -1;

    public FrameParser(int bufsize) {
        buffer = ByteBuffer.allocateDirect(bufsize);
    }

    public FrameParser(ByteBuffer buffer) {
        if (buffer == null)
            throw new IllegalArgumentException("buffer should not be null");

        this.buffer = buffer;
    }

    public ByteBuffer getByteBuffer() {
        return buffer;
    }

    /**
     * JPEG frame 抽出
     * @return バッファに JPEG frame が含まれる場合はそのbyte配列 含まれない場合は null
     */
    public byte[] getFrame() {
        // SOI 未検出
        if (soi_pos < 0) {
            // SOI (0xFFD8) 検索
            byte b0 = 0;
            byte b1 = 0;
            while (search_pos < buffer.position() - 1) {
                b0 = buffer.get(search_pos);
                b1 = buffer.get(search_pos+1);
                
                // simple search
                if (b0 == (byte) 0xff && b1 == (byte) 0xd8) {
                    // hit
                    buffer.limit(buffer.position());
                    buffer.position(search_pos);
                    buffer.compact();
                    search_pos = 2;
                    soi_pos = 0;
                    break;
                } else {
                    search_pos++;
                    continue;
                }
            }
        }
        if (soi_pos < 0) {
            // SOI 未検出
            buffer.limit(buffer.position());
            buffer.position(search_pos);
            buffer.compact();
            search_pos = 0;
            return null;
        }

        if (0 <= soi_pos) {
            // EOI (0xFFD9) 検索
            byte b0 = 0;
            byte b1 = 0;
            while (search_pos < buffer.position() - 1) {
                b0 = buffer.get(search_pos);
                b1 = buffer.get(search_pos+1);
                
                // simple search
                if (b0 == (byte) 0xff && b1 == (byte) 0xd9) {
                    // hit
                    // SOI有り, EOI有り -> frame 抽出
                    int length = search_pos + 2 - soi_pos;
                    byte[] frame = new byte[length];

                    // frame 抽出
                    int pos = buffer.position();    // 書き込み位置保存
                    buffer.position(soi_pos);
                    buffer.limit(search_pos + 2);
                    buffer.get(frame);
                    soi_pos = -1;

                    buffer.limit(pos);
                    buffer.position(search_pos + 2);
                    buffer.compact();
                    search_pos = 0;
                    return frame;
                } else {
                    search_pos++;
                    continue;
                }
            }
        }

        // SOI有り, EOI未検出
        return null;
    }
    
    
    // for debug
    protected int getSearchPos() {
        return search_pos;
    }
    protected int getSOIPos() {
        return soi_pos;
    }
}
