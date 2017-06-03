package com.github.kmkt.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * InputStream を指定されたデリミタ(byte[])で個別の InputStream に分割する。
 * <pre>
 * 非スレッドセーフ
 * {@code
 * InputStream is = {@link #nextStream()};
 * while (is != null) {
 *   int c = is.read();
 *   if (c == -1) {
 *     // Get next InputStream if last one is finished.
 *     is = {@link #nextStream()};
 *   }
 * }
 * }
 * </pre>
 * License : MIT License
 */
public class StreamSplitter implements AutoCloseable {
    private byte[] delimiter = null;        // デリミタ
    private int delimiterLength;            // デリミタ長
    private int[] skipTable = new int[256]; // デリミタに対応するQuickSearch用シフトテーブル

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
     * 元となる InputStream とデリミタを与えてインスタンスを生成する。
     * デリミタ探索用バッファはデフォルトサイズ {@link #BUF_SIZE} で生成される。
     * @param is 元となる InputStream notnull
     * @param delimiter デリミタ notnull
     * @throws NullPointerException is, delimiter が null の場合
     */
    public StreamSplitter(InputStream is, byte[] delimiter) {
        this(is, delimiter, BUF_SIZE);
    }

    /**
     * 元となる InputStream とデリミタを与えてインスタンスを生成する。
     * @param is 元となる InputStream notnull
     * @param delimiter デリミタ notnull
     * @param buf_size デリミタ探索用バッファサイズ(2^nサイズに調整される)
     * @throws NullPointerException is, delimiter が null の場合
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
        this.delimiterLength = this.delimiter.length;
        this.ringBufffer = new byte[buf_size];

        // delimiter に対応する QuickSearch シフトテーブル作成
        for (int i = 0; i < this.skipTable.length; i++) {
            this.skipTable[i] = delimiter.length + 1;
        }
        for (int i = 0; i < delimiter.length; i++) {
            this.skipTable[delimiter[i]] = delimiter.length - i;
        }
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

        // Quick Search
        for (int i = 0; i <= search_range;) {
            int pos = (stpos + i) & modMask;
            for (int j = 0; j < delimiterLength; j++) {
                if (ringBufffer[(pos + j) & modMask] == delimiter[j]) {
                    if (j == delimiterLength - 1) {
                        return (pos & modMask);
                    }
                } else {
                    i += skipTable[ringBufffer[(pos + delimiterLength) & modMask] & 0xff];
                    break;
                }
            }
        }
        return -1;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    /**
     * 分割された次の InputStream を取得する。
     * @return 分割後の内容を返す InputStream 元 InputStream が終端している場合は null を返す。
     * @throws IllegalStateException 以前に返した InputStream が終端していない（内容が残っている）場合。
     */
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

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            Objects.requireNonNull(b);
            if (off < 0)
                throw new IllegalArgumentException("off must be zero or positive");
            if (len < 0)
                throw new IllegalArgumentException("len must be zero or positive");
            if (b.length < off + len)
                throw new IllegalArgumentException("off + len must be less than or equal to b.length");
            if (len == 0)
                return 0;

            if (eos || readPos == delimiterPos) {
                eos = true;
                return -1;
            }

            if (!inputEoS && (avalableSize <= 0)) {
                readNextBlock();
            }
            int c = readRingBuffer(b, off, len);
            if (c == -1) {
                eos = true;
            }
            return c;
        }
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
     * RingBufferから読み出し
     * @return
     */
    private int readRingBuffer(byte[] b, int off, int len) {
        if (avalableBufSize == 0 || avalableSize == 0)
            return -1;  // empty

        // 読み込み長と読み込み可能長で短い方を採用
        len = len < avalableSize ? len : avalableSize;

        if (ringBufffer.length < readPos + len) {
            // 分割必要
            // readPos -> ringBufffer limit
            int buf_len = ringBufffer.length - readPos;
            System.arraycopy(ringBufffer, readPos, b, off, buf_len);
            off += buf_len;

            // 0 -> limit
            buf_len = len - buf_len;
            System.arraycopy(ringBufffer, 0, b, off, buf_len);
        } else {
            // 分割不要
            System.arraycopy(ringBufffer, readPos, b, off, len);
        }
        readPos = (readPos + len) & modMask;
        avalableBufSize -= len;
        avalableSize -= len;
        return len;
    }


    /**
     * 元InputStreamからRingBufferの空き部分に読み込む
     * @throws IOException
     */
    private void readNextBlock() throws IOException {
        if (inputEoS)
            return;     // 入力側EOS

        if (avalableBufSize == ringBufffer.length)
            return;     // buffer is full

        int write_pos = (readPos + avalableBufSize) & modMask;
        int empty_size = ringBufffer.length - avalableBufSize;

        while (0 < empty_size) {
            int buf_len = empty_size;
            if (ringBufffer.length < write_pos + buf_len) {
                // 分割必要
                // write_pos -> ringBufffer limit
                buf_len = ringBufffer.length - write_pos;
            }
            int read_len = inputStream.read(ringBufffer, write_pos, buf_len);
            if (read_len == -1) {
                inputEoS = true;    // 入力側EOS
                break;
            }
            avalableBufSize += read_len;
            empty_size -= read_len;
            write_pos = (write_pos + read_len) & modMask;
        }
        searchNextDelimiter();
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
