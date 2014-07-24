package com.github.kmkt.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * MJPEG over HTTP ストリームを受け取り、callback するクラス
 * 
 * License : MIT License
 */
public class MjpegHTTPReader {
    private static final Logger logger = LoggerFactory.getLogger(MjpegHTTPReader.class);

    /**
     * フレーム受信毎に呼び出される callback interface
     */
    public interface RecvFrameCallback {
        /**
         * フレーム受信毎に呼び出される callback
         * @param frame_data 1フレーム分のデータ
         */
        void onRecvFrame(byte[] frame_data);
    }

    /**
     * 状態変化時に呼び出される callback interface
     */
    public interface StateChangeCallback {
        /**
         * ストリーム終了時に呼び出される callback
         */
        void onStreamClosed();
        /**
         * 受信スレッド終了時に呼び出される callback
         */
        void onFinished();
    }

    public static long StatisticsDispleyPeriod = 60*1000;   // 

    private URI target = null;
    private StreamReaderThread streamReadLoop = null;
    private RecvFrameCallback recv_callback = null;
    private StateChangeCallback state_callback = null;


    /**
     * 
     * @param target MJPEG 配信元 URL
     * @param recv_callback フレーム受信毎に呼び出される callback
     * @param state_callback 状態変化時に呼び出される callback
     */
    public MjpegHTTPReader(URI target, RecvFrameCallback recv_callback, StateChangeCallback state_callback) {
        if (target == null)
            throw new IllegalArgumentException("target should not be null");

        this.target = target;
        this.recv_callback = recv_callback;
        this.state_callback = state_callback;
    }

    public synchronized boolean isActive() {
        if (this.streamReadLoop == null)
            return false;

        return this.streamReadLoop.isAlive();
    }

    /**
     * MJPEG の受信を開始する
     * 
     * @param connecte_timeout 接続タイムアウト
     * @param read_timeout Socket Reead タイムアウト
     * @throws ClientProtocolException
     * @throws IOException
     */
    public synchronized void start(int connecte_timeout, int read_timeout) throws ClientProtocolException, IOException {
        if (connecte_timeout < 0)
            throw new IllegalArgumentException("connecte_timeout should be positive");
        if (isActive())
            throw new IllegalStateException("Already started");

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connecte_timeout)
                .setSocketTimeout(read_timeout).build();
        HttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

        HttpGet httpget = new HttpGet(target);
        try {
            // GET リクエスト
            HttpResponse response = httpclient.execute(httpget);

            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                httpget.abort();
                throw new IOException("HTTP Response is not 200 but " + status);
            }

            HttpEntity entity = response.getEntity();
            String[] content_type = entity.getContentType().getValue().split("\\s*;\\s*");

            if (!"multipart/x-mixed-replace".equals(content_type[0])) {
                httpget.abort();
                throw new IOException("Content-type is not multipart/x-mixed-replace but " + content_type[0]);
            }
            if (!content_type[1].startsWith("boundary=")) {
                httpget.abort();
                throw new IOException("Content-type should have boundary option");
            }

            // boundary バイト列作成
            String boundary_str = "--"+content_type[1].replaceFirst("boundary=", "");
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            b.write(boundary_str.getBytes());
            b.write((byte) 0x0d);
            b.write((byte) 0x0a);
            byte[] boundary = b.toByteArray();

            streamReadLoop = new StreamReaderThread(entity.getContent(), boundary, recv_callback, state_callback);
            streamReadLoop.start();
        } catch(ClientProtocolException e) {
            httpget.abort();
            throw e;
        } catch(IOException e) {
            httpget.abort();
            throw e;
        }
    }


    public synchronized void stop() throws InterruptedException, IOException {
        if (!isActive())
            return;        
        streamReadLoop.requestStop();
        streamReadLoop.join();
        streamReadLoop = null;
    }

    /**
     * HTTP body を読み込み、MJPEGからJPEGを切り出す Thread
     */
    private class StreamReaderThread extends Thread {
        private StreamSplitter splitter = null;
        private InputStream inputStream = null;
        private RecvFrameCallback recv_callback = null;
        private StateChangeCallback state_callback = null;
        private volatile boolean threadLoop = true;

        /**
         * 
         * @param is 元InputStream
         * @param boundary 境界バイト列
         * @param recv_callback ブロック受信時の callback  null では呼ばれない
         * @param state_callback 状態変化時の callback
         */
        public StreamReaderThread(InputStream is, byte[] boundary, RecvFrameCallback recv_callback, StateChangeCallback state_callback) {
            if (is == null)
                throw new IllegalArgumentException("is should not be null");
            if (boundary == null)
                throw new IllegalArgumentException("boundary should not be null");

            this.inputStream = is;
            this.splitter = new StreamSplitter(is, boundary);
            this.recv_callback = recv_callback;
            this.state_callback = state_callback;
        }

        /**
         * 受信停止を要求する
         * @throws IOException 
         */
        public void requestStop() throws IOException {
            threadLoop = false;
            if (this.getState() == Thread.State.BLOCKED || 
                this.getState() == Thread.State.WAITING ||
                this.getState() == Thread.State.TIMED_WAITING) {
                streamReadLoop.interrupt();
            }
            splitter.close();
        }

        @Override
        public void run() {
            long recv_frames = 0;
            long recv_bytes = 0;
            long error_frames = 0;
            long notify_frames = 0;
            long notify_bytes = 0;
            long last_shown_statistics = System.currentTimeMillis();
            
            logger.info("Start recv thread");
            try {
                byte[] deleimter_of_header = new byte[]{(byte) 0x0d, (byte) 0x0a, (byte) 0x0d, (byte) 0x0a};
                byte[] readbuf = new byte[4*1024];  // 読み出しバッファ
                while (threadLoop) {
                    int len = 0;
                    logger.trace("Wait next stream");
                    InputStream is = splitter.nextStream();
                    if (is == null) {
                        logger.info("Stream ended");
                        if (state_callback != null) {
                            state_callback.onStreamClosed();
                        }
                        break;
                    }

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();

                    logger.trace("Wait recving");
                    while (true) {
                        len = is.read(readbuf);
                        if (len == -1)
                            break;
                        bos.write(readbuf, 0, len);
                    }
                    byte[] recv_block = bos.toByteArray();
                    logger.trace("Recv {} byte", recv_block.length);

                    recv_frames++;
                    recv_bytes += recv_block.length;

                    // TODO multipart のヘッダ確認

                    // body 部の JPEG フレームの取り出し
                    byte[] jpeg_frame = new byte[]{};
                    for (int i = 0; i < recv_block.length; i++) {
                        for (int j = 0; j < deleimter_of_header.length && i + j < recv_block.length; j++) {
                            if (recv_block[i + j] != deleimter_of_header[j])
                                break;
                            if (j == deleimter_of_header.length - 1) {  // found delimiter
                                int limit = recv_block.length;
                                jpeg_frame = Arrays.copyOfRange(recv_block, i + deleimter_of_header.length, limit);
                            }
                        }
                    }

                    // XXX 要JPEGフォーマット判定？
                    logger.trace("Frame size {} byte", jpeg_frame.length);

                    if (recv_callback != null) {
                        recv_callback.onRecvFrame(jpeg_frame);
                        notify_frames++;
                        notify_bytes += jpeg_frame.length;
                    }
                    if (StatisticsDispleyPeriod < System.currentTimeMillis() - last_shown_statistics) {
                        last_shown_statistics = System.currentTimeMillis();
                        logger.debug("Statistics [Frames Recv: {}, Send: {}, Drop: {}, Size Recv: {}, Send: {}]", 
                                recv_frames, notify_frames, error_frames,
                                recv_bytes, notify_bytes);
                    }
                }
            } catch (IOException e) {
                if (threadLoop) {
                    logger.error("IOException when stream reading", e);
                }
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("IOException when stream closing", e);
                }
            }
            logger.info("Stop recv thread");
            if (state_callback != null) {
                state_callback.onFinished();
            }
        }
    }
}
