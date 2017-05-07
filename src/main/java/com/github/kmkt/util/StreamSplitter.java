package com.github.kmkt.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * InputStream を指定されたデリミタ(byte[])で個別の InputStream に分割する。
 * mod 演算高速化のためバッファサイズ調整版
 * <pre>
 * 非スレッドセーフ
 * </pre>
 *
 * License : MIT License
 */
public class StreamSplitter implements AutoCloseable {
    private byte[] delimiter = null;        // デリミタ
    private InputStream inputStream = null; // 元InputStream

    private InnerInputStream childStream = null;    // 分割後のInputStream

    private static final int BUF_SIZE = 4*1024;
    private byte[] ringBufffer = null;
    private int modMask = 0;

    private int avalableBufSize = 0;    // 読み込み済みサイズ
    private int avalableSize = 0;       // 検索済みサイズ
    private int readPos = 0;            // 次読み込みindex
    private int searchPos = 0;          // 次検索開始index
    private int delimiterPos = -1;      // デリミタ先頭index -1 はデリミタ未発見

    private boolean inputEoS = false;   // 元InputStream終端フラグ

    /**
     *
     * @param is
     * @param delimiter
     */
    public StreamSplitter(InputStream is, byte[] delimiter) {
        this(is, delimiter, BUF_SIZE);
    }

    /**
     *
     * @param is
     * @param delimiter
     * @param buf_size
     */
    public StreamSplitter(InputStream is, byte[] delimiter, int buf_size) {
        if (is == null)
            throw new IllegalArgumentException("is should not be null");
        if (delimiter == null)
            throw new IllegalArgumentException("delimiter should not be null");
        if (buf_size < delimiter.length*4)
            throw new IllegalArgumentException("buf_size should be 4 times larger than delimiter size");

        // 指定の buf_size が収まる 2^n サイズにバッファサイズを調整
        int mod_mask = buf_size - 1;
        mod_mask |= (mod_mask >>> 1);
        mod_mask |= (mod_mask >>> 2);
        mod_mask |= (mod_mask >>> 4);
        mod_mask |= (mod_mask >>> 6);
        mod_mask |= (mod_mask >>> 16);
        buf_size = mod_mask + 1;
        this.modMask = mod_mask;

        this.inputStream = is;
        this.delimiter = Arrays.copyOf(delimiter, delimiter.length);
        this.ringBufffer = new byte[buf_size];
    }

    /**
     * ringBufffer 内の delimiter を検索する
     * @param stpos 検索範囲先頭
     * @param limitpos 検索範囲末
     * @return 発見できない場合は-1
     */
    private int searchDelimiter(int stpos, int limitpos) {
        if (stpos < 0 || ringBufffer.length <= stpos)
            throw new IllegalArgumentException("stpos is out of range");
        if (limitpos < 0 || ringBufffer.length <= limitpos)
            throw new IllegalArgumentException("limitpos is out of range");
        if (stpos == limitpos)
            throw new IllegalArgumentException("stpos and limitpos should not be at same pos");

        int search_range = (stpos < limitpos ? limitpos - stpos + 1 : ringBufffer.length - stpos + limitpos + 1);
        search_range = search_range - delimiter.length;
//        System.out.printf("%d %d %d%n", stpos, limitpos,search_range);

        if (search_range < 0)
            return -1;

        // バカサーチ パフォーマンス出ない場合は簡易BM, Quick Search等に
        for (int i = 0; i <= search_range; i++) {
            int pos = (stpos + i) & modMask;
            boolean found = true;
            for (int j = 0; j < delimiter.length; j++) {
                if (ringBufffer[(pos + j) & modMask] != delimiter[j]) {
                    found = false;
                    break;
                }
            }
            if (found)
                return (pos & modMask);
        }
        return -1;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    public InputStream nextStream() {
        if (childStream == null) {
            childStream = new InnerInputStream();
            return childStream;
        }
        if (inputEoS && avalableBufSize <= delimiter.length)
            return null;
        if (!childStream.isFinished())
            throw new IllegalStateException("Previous stream has available data yet");

        childStream = new InnerInputStream();
        // デリミタ分読み飛ばし操作
        avalableBufSize -= delimiter.length;
        readPos = searchPos;
        searchNextDelimiter();

        return childStream;
    }

    /**
     * 分割後のInputStream
     */
    private class InnerInputStream extends InputStream {
        private boolean eos = false;

        protected boolean isFinished() {
            return eos;
        }

        @Override
        public int available() throws IOException {
            return avalableSize;
        }

        @Override
        public int read() throws IOException {
            if (eos || readPos == delimiterPos) {
                eos = true;
                return -1;
            }

            if (!inputEoS && (avalableSize <= 0)) {
                readNextBlock();
            }
            int c = readRingBuffer();
            if (c == -1) {
                eos = true;
            }
            return c;
        }
    }

    /**
     * RingBufferへの書き込み
     * @param c
     * @return
     */
    private boolean writeRingBuffer(byte c) {
        if (avalableBufSize == ringBufffer.length)
            return false;   // full
        int pos = (readPos + avalableBufSize) & modMask;
        ringBufffer[pos] = c;
        avalableBufSize++;
        return true;
    }

    /**
     * RingBufferから読み出し
     * @return
     */
    private int readRingBuffer() {
        if (avalableBufSize == 0 || avalableSize == 0)
            return -1;  // empty

        int c = ringBufffer[readPos];

        readPos++;
        if (ringBufffer.length == readPos)
            readPos = 0;
        avalableBufSize--;
        avalableSize--;

        return c & 0x00ff;
    }

    /**
     * 元InputStreamからRingBufferの空き部分に読み込む
     * @throws IOException
     */
    private void readNextBlock() throws IOException {
        if (inputEoS)
            return;

        int limit = ringBufffer.length - avalableBufSize;

        int i = 0;
        IOException e = null;
        try {
            for (i = 0; i < limit; i++) {
                int c = inputStream.read();
                if (c == -1) {
                    inputEoS = true;    // 入力側EOS
                    break;
                }
                writeRingBuffer((byte) (c & 0x00ff));
            }
        } catch (IOException e2) {
            if (i == 0) {
                throw e2;   // 初回読み込みでの例外はそのまま投げる
            } else {
                e = e2; // それ以外は次の処理を終えてから
            }
        }
        searchNextDelimiter();
        if (e != null)
            throw e;
    }

    /**
     * デリミタ検索：前回終了位置～今回の書き込み末 の範囲
     */
    private void searchNextDelimiter() {
        if (avalableBufSize < delimiter.length) {
            // バッファ内データがデリミタサイズに満たない場合：未発見
            delimiterPos = -1;
            return;
        }

        int edpos = (readPos + avalableBufSize - 1) & modMask;    // 有効範囲末
        int delimiterpos = searchDelimiter(searchPos, edpos);   // 検索

        if (delimiterpos == -1) {
            // 未発見
            delimiterPos = -1;

            // デリミタが無い範囲
            int len = (edpos + 1) - searchPos - delimiter.length + 1;
            if (len < 0)
                len += ringBufffer.length;
            avalableSize += len;

            searchPos = (edpos + 1) - delimiter.length + 1;   // 次検索開始位置
            if (searchPos < 0) {
                searchPos += ringBufffer.length;
            }
        } else {
            // 発見
            delimiterPos = delimiterpos;

            int len = delimiterpos - searchPos;
            if (len < 0) {
                len += ringBufffer.length;
            }
            avalableSize += len;

            // 次検索開始はデリミタの後ろから
            searchPos = (delimiterpos + delimiter.length) & modMask;
        }
    }
}
