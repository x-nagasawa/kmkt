package com.github.kmkt.util.mjpeg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kmkt.util.StreamSplitter;


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
    @FunctionalInterface
    public interface RecvFrameCallback {
        /**
         * フレーム受信毎に呼び出される callback
         * @param frame_data 1フレーム分のデータ
         */
        void onRecvFrame(byte[] frame_data);
    }

    /**
     * フレーム受信毎に呼び出される callback interface
     */
    @FunctionalInterface
    public interface RecvFrameOnBufferCallback {
        /**
         * フレーム受信毎に呼び出される callback
         * @param b JPEG フレームが格納されるバッファ
         * @param off b 内の JPEG フレーム開始位置
         * @param len b 内の JPEG フレームサイズ
         */
        void onRecvFrame(byte[] b, int off, int len);
    }

    /**
     * ストリーム受信終了時に呼び出される callback
     */
    @FunctionalInterface
    public interface StreamClosedCallback {
        void onStreamClosed();
    }

    /**
     * 受信スレッド終了時に呼び出される callback
     */
    public interface ThreadTerminatedCallback {
        void onFinished();
    }

    /** 集計情報のログ出力間隔 (ms)*/
    public static long StatisticsDispleyPeriod = 60*1000;
    /** 標準の受信バッファサイズ (バイト) */
    public static final int DEFAULT_BUFFER_SIZE = 128*1024;

    /** MJPEG 配信元 URL */
    private URI target = null;
    /** BASIC 認証認証情報 */
    private Credentials credential = null;

    /** 受信スレッド */
    private Thread streamReadThread = null;
    /** スレッドループ有効フラグ */
    private volatile boolean threadLoop = true;

    /** フレーム受信バッファサイズ (byte) */
    private int receiveBufferSize = DEFAULT_BUFFER_SIZE;

    /** フレーム受信毎に呼び出される callback */
    private RecvFrameCallback recvCallback = null;
    /** フレーム受信毎に呼び出される callback */
    private RecvFrameOnBufferCallback recvOnBufferCallback = null;
    /** ストリーム受信終了時に呼び出される callback */
    private StreamClosedCallback streamClosedCallback = null;
    /** 受信スレッド終了時に呼び出される callback */
    private ThreadTerminatedCallback threadTerminatedCallback = null;


    /**
     * target URL から MJPEG を受信するインスタンスを生成する。
     *
     * 受信できた JPEG フレームは recv_callback で指定された callback に渡される。
     * 受信バッファサイズは DEFAULT_BUFFER_SIZE となり、 callback には受信バッファ内の
     * JPEG フレームのコピーが渡される。
     *
     * @param target MJPEG 配信元 URL notnull
     * @param recv_callback フレーム受信毎に呼び出される callback null 時は無視される
     * @param stream_closed_callback 状態変化時に呼び出される callback null 時は無視される
     * @param thread_terminated_callback 受信スレッド終了時に呼び出される callback null 時は無視される
     * @throws NullPointerException target == null 時
     */
    public MjpegHTTPReader(URI target, RecvFrameCallback recv_callback,
            StreamClosedCallback stream_closed_callback, ThreadTerminatedCallback thread_terminated_callback) {
        this(target, recv_callback, stream_closed_callback, thread_terminated_callback, null, null);
    }

    /**
     * Basic 認証付き target URL から MJPEG を受信するインスタンスを生成する。
     *
     * 受信できた JPEG フレームは recv_callback で指定された callback に渡される。
     * 受信バッファサイズは DEFAULT_BUFFER_SIZE となり、 callback には受信バッファ内の
     * JPEG フレームのコピーが渡される。
     *
     * @param target MJPEG 配信元 URL notnull
     * @param recv_callback フレーム受信毎に呼び出される callback null 時は無視される
     * @param stream_closed_callback 状態変化時に呼び出される callback null 時は無視される
     * @param thread_terminated_callback 受信スレッド終了時に呼び出される callback null 時は無視される
     * @param user Basic 認証ユーザ名 null 時は Basic認証を行わない
     * @param pass Basic 認証パスワード null 時は Basic認証を行わない
     * @throws NullPointerException target == null 時
     */
    public MjpegHTTPReader(URI target, RecvFrameCallback recv_callback,
            StreamClosedCallback stream_closed_callback, ThreadTerminatedCallback thread_terminated_callback,
            String user, String pass) {
        this(target, stream_closed_callback, thread_terminated_callback, user, pass);

        this.recvCallback = recv_callback;
    }

    /**
     * target URL から MJPEG を受信するインスタンスを生成する。
     *
     * JPEG フレームを含む受信データと JPEG フレーム位置が recv_callback で指定された callback に渡される。
     * callback に渡されるバイト配列は原則として受信バッファそのものが渡されるため、必要があればコピーを作成すること。
     * 受信バッファが溢れる場合には、動的バッファに切り替えられるためデータロスは生じない（メモリ消費量は増加する）。
     *
     * @param target MJPEG 配信元 URL notnull
     * @param recv_buffer_size 受信バッファサイズ(バイト) 1 以上の整数
     * @param recv_callback フレーム受信毎に呼び出される callback null 時は無視される
     * @param stream_closed_callback 状態変化時に呼び出される callback null 時は無視される
     * @param thread_terminated_callback 受信スレッド終了時に呼び出される callback null 時は無視される
     * @throws NullPointerException target == null 時
     * @throws IllegalArgumentException recv_buffer_size に 0 以下を与えた場合
     */
    public MjpegHTTPReader(URI target, int recv_buffer_size, RecvFrameOnBufferCallback recv_callback,
            StreamClosedCallback stream_closed_callback, ThreadTerminatedCallback thread_terminated_callback) {
        this(target, recv_buffer_size, recv_callback, stream_closed_callback, thread_terminated_callback, null, null);
    }

    /**
     * Basic 認証付き target URL から MJPEG を受信するインスタンスを生成する。
     *
     * JPEG フレームを含む受信データと JPEG フレーム位置が recv_callback で指定された callback に渡される
     * callback に渡されるバイト配列は受信バッファそのものが渡されるため、必要があればコピーを作成すること。
     * 受信バッファが溢れる場合には、動的バッファに切り替えられるためデータロスは生じない（メモリ消費量は増加する）。
     *
     * @param target MJPEG 配信元 URL notnull
     * @param recv_buffer_size 受信バッファサイズ(バイト) 1 以上の整数
     * @param recv_callback フレーム受信毎に呼び出される callback null 時は無視される
     * @param stream_closed_callback 状態変化時に呼び出される callback null 時は無視される
     * @param thread_terminated_callback 受信スレッド終了時に呼び出される callback null 時は無視される
     * @param user Basic 認証ユーザ名 null 時は Basic認証を行わない
     * @param pass Basic 認証パスワード null 時は Basic認証を行わない
     * @throws NullPointerException target == null 時
     * @throws IllegalArgumentException recv_buffer_size に 0 以下を与えた場合
     */
    public MjpegHTTPReader(URI target, int recv_buffer_size, RecvFrameOnBufferCallback recv_callback,
            StreamClosedCallback stream_closed_callback, ThreadTerminatedCallback thread_terminated_callback,
            String user, String pass) {
        this(target, stream_closed_callback, thread_terminated_callback, user, pass);
        if (recv_buffer_size <= 0)
            throw new IllegalArgumentException("recv_buffer_size must be positive");

        this.recvOnBufferCallback = recv_callback;
        this.receiveBufferSize = recv_buffer_size;
    }

    /**
     * 内部コンストラクタ
     *
     * @param target MJPEG 配信元 URL notnull
     * @param stream_closed_callback 状態変化時に呼び出される callback null 時は無視される
     * @param thread_terminated_callback 受信スレッド終了時に呼び出される callback null 時は無視される
     * @param user Basic 認証ユーザ名 null 時は Basic認証を行わない
     * @param pass Basic 認証パスワード null 時は Basic認証を行わない
     * @throws NullPointerException target == null 時
     */
    private MjpegHTTPReader(URI target,
            StreamClosedCallback stream_closed_callback, ThreadTerminatedCallback thread_terminated_callback,
            String user, String pass) {
        Objects.requireNonNull(target, "target should not be null");

        this.target = target;
        this.streamClosedCallback = stream_closed_callback;
        this.threadTerminatedCallback = thread_terminated_callback;

        if (user != null && pass != null) {
            this.credential = new UsernamePasswordCredentials(user, pass);
        }
    }

    /**
     * MJPEG の受信中か否かを返す。
     *
     * @return true 受信中 false それ以外（{@link #start(int, int)}前あるいは {@link #stop()} 後）
     */
    public synchronized boolean isActive() {
        return (this.streamReadThread != null && threadLoop);
    }

    /**
     * MJPEG の受信を開始する。
     *
     * @param connecte_timeout 接続タイムアウト (ms) 1以上の整数
     * @param read_timeout Socket Read タイムアウト (ms) 1以上の整数
     * @throws IllegalArgumentException connecte_timeout, read_timeout に 0 以下を与えた場合
     * @throws IllegalStateException 既に受信中の場合
     * @throws ClientProtocolException
     * @throws IOException
     */
    public synchronized void start(int connecte_timeout, int read_timeout) throws ClientProtocolException, IOException {
        if (connecte_timeout < 0)
            throw new IllegalArgumentException("connecte_timeout must be positive");
        if (read_timeout < 0)
            throw new IllegalArgumentException("read_timeout must be positive");
        if (isActive())
            throw new IllegalStateException("Already started");

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connecte_timeout)
                .setSocketTimeout(read_timeout).build();
        HttpClient httpclient;
        if (credential == null) {
            httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        } else {
            CredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(AuthScope.ANY, credential);
            httpclient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).setDefaultRequestConfig(config).build();
        }

        HttpGet httpget = new HttpGet(target);
        HttpResponse response;
        try {
            // GET リクエスト
            response = httpclient.execute(httpget);
        } catch(ClientProtocolException e) {
            httpget.abort();
            throw e;
        } catch(IOException e) {
            httpget.abort();
            throw e;
        }

        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            httpget.abort();
            throw new IOException("HTTP Response is not 200 but " + status);
        }

        HttpEntity entity = response.getEntity();
        Header content_type_header = entity.getContentType();
        if (content_type_header.getElements().length == 0) {
            httpget.abort();
            throw new IOException("HTTP Response should have a Content-type header");
        }

        logger.debug("Content-type '{}'", entity.getContentType().getValue());
        HeaderElement ele = content_type_header.getElements()[0];
        if (!"multipart/x-mixed-replace".equals(ele.getName())) {
            httpget.abort();
            throw new IOException("Content-type is not multipart/x-mixed-replace but " + ele.getName());
        }

        NameValuePair boundary_param = ele.getParameterByName("boundary");
        if (boundary_param == null) {
            httpget.abort();
            throw new IOException("Content-type should have boundary option");
        }

        String boundary_value = boundary_param.getValue();
        if (boundary_value == null || boundary_value.isEmpty()) {
            httpget.abort();
            throw new IOException("Content-type should have non-empty boundary option");
        }

        // boundary バイト列作成
        // RFC2046 に反して、"--"を含むバウンダリ文字列そのものを boundary option に
        // 設定するサーバ対策として、boundary_value 頭にある "--" は一端削除する。
        // これにより、multipart body の末尾に "--" が残るが、 JPEG フレーム切り出し時に
        // 切り捨てられるため、出力される JPEG フレームには影響しない。
        String boundary_str = "--"+boundary_value.replaceFirst("--", "");
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        b.write(boundary_str.getBytes());
        b.write((byte) 0x0d);
        b.write((byte) 0x0a);
        byte[] boundary = b.toByteArray();

        threadLoop = true;
        streamReadThread = new Thread(() -> {
            logger.info("Start recv thread");

            final byte[] SOI = new byte[]{(byte) 0xff, (byte) 0xd8};    // JPEG SOI
            final byte[] EOI = new byte[]{(byte) 0xff, (byte) 0xd9};    // JPEG EOI
            // multipart の ヘッダ/ボディ境界
            final byte[] deleimter_of_header = new byte[]{(byte) 0x0d, (byte) 0x0a, (byte) 0x0d, (byte) 0x0a};
            // 計測カウンタ
            long recv_frames = 0;
            long recv_bytes = 0;
            long error_frames = 0;
            long notify_frames = 0;
            long notify_bytes = 0;
            long last_shown_statistics = System.currentTimeMillis();
            // 受信バッファ
            byte[] recv_buf = new byte[receiveBufferSize];

            try (final InputStream source_stream = entity.getContent();
                 final StreamSplitter splitter = new StreamSplitter(source_stream, boundary)) {

                while (threadLoop) {
                    logger.trace("Wait next stream");
                    InputStream is = splitter.nextStream();
                    if (is == null) {
                        logger.info("Stream ended");
                        if (streamClosedCallback != null) {
                            streamClosedCallback.onStreamClosed();
                        }
                        break;
                    }

                    // boundary 間のデータ受信
                    int off = 0;
                    ByteArrayOutputStream bos = null;   // recv_buf overflow 時用代替バッファ

                    logger.trace("Wait receiving");
                    while (true) {
                        int len = is.read(recv_buf, off, recv_buf.length - off);
                        if (len == -1)
                            break;

                        if (bos != null) {
                            bos.write(recv_buf, 0, len);
                            off = 0;
                        } else {
                            off += len;
                            if (recv_buf.length == off) {
                                logger.debug("Pre-defined buffer is overflowed. Switch to ByteArrayOutputStream");
                                // recv_buf overflow -> switch to ByteArrayOutputStream
                                bos = new ByteArrayOutputStream();
                                bos.write(recv_buf, 0, recv_buf.length);
                            }
                        }
                    }

                    int recv_size = off;
                    if (bos != null) {
                        recv_buf = bos.toByteArray();
                        recv_size = recv_buf.length;
                        bos = null;
                    }

                    logger.trace("Recv {} byte", recv_size);

                    recv_frames++;
                    recv_bytes += recv_size;

                    // TODO multipart のヘッダ確認

                    // body 部の取り出し
                    int body_pos = -1;
                    found_jpegbody:
                    for (int i = 0; i < recv_size; i++) {
                        for (int j = 0; j < deleimter_of_header.length && i + j < recv_size; j++) {
                            if (recv_buf[i + j] != deleimter_of_header[j])
                                break;
                            if (j == deleimter_of_header.length - 1) {  // found delimiter
                                body_pos = i + deleimter_of_header.length;
                                break found_jpegbody;
                            }
                        }
                    }
                    if (body_pos < 0) {
                        logger.warn("Invalid chunk received. Cannot found chunk body.");
                        error_frames++;
                        continue;
                    }

                    // JPEG部の探索
                    int pos_soi = -1;
                    int pos_eoi = -1;
                    found_SOI:
                    for (int i = body_pos; i < recv_size; i++) {
                        for (int j = 0; j < SOI.length && i + j < recv_size; j++) {
                            if (recv_buf[i + j] != SOI[j])
                                break;
                            if (j == SOI.length - 1) {  // found soi
                                pos_soi = i;
                                break found_SOI;
                            }
                        }
                    }
                    found_EOI:
                    for (int i = recv_size - EOI.length; pos_soi < i; i--) {    // 後ろから探索
                        for (int j = 0; j < EOI.length && i + j < recv_size; j++) {
                            if (recv_buf[i + j] != EOI[j])
                                break;
                            if (j == EOI.length - 1) {  // found eoi
                                pos_eoi = i;
                                break found_EOI;
                            }
                        }
                    }

                    if (pos_soi < 0 || pos_eoi < 0) {
                        logger.warn("Invalid JPEG frame received. Cannot found SOI or EOI.");
                        error_frames++;
                        continue;
                    }
                    logger.trace("Frame size {} byte", pos_eoi + 2 - pos_soi);

                    if (recvOnBufferCallback != null) {
                        recvOnBufferCallback.onRecvFrame(recv_buf, pos_soi, pos_eoi + 2 - pos_soi);
                        notify_frames++;
                        notify_bytes += (pos_eoi + 2 - pos_soi);
                    }
                    if (recvCallback != null) {
                        byte[] jpeg_frame = Arrays.copyOfRange(recv_buf, pos_soi, pos_eoi + 2);
                        recvCallback.onRecvFrame(jpeg_frame);
                        notify_frames++;
                        notify_bytes += jpeg_frame.length;
                    }
                    if (StatisticsDispleyPeriod < System.currentTimeMillis() - last_shown_statistics) {
                        last_shown_statistics = System.currentTimeMillis();
                        logger.debug("Statistics [Frames Recv: {}, Send: {}, Error: {}, Size Recv: {}, Send: {}]",
                                recv_frames, notify_frames, error_frames,
                                recv_bytes, notify_bytes);
                    }
                }
            } catch (IOException e) {
                if (threadLoop) {
                    logger.error("IOException when stream reading", e);
                }
            }
            threadLoop = false;
            logger.info("Stop recv thread");
            if (threadTerminatedCallback != null) {
                threadTerminatedCallback.onFinished();
            }
        });

        streamReadThread.start();
    }


    /**
     * MJPEG の受信を停止する。
     *
     * 受信していない場合は何もしない。
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public synchronized void stop() throws InterruptedException, IOException {
        if (!isActive())
            return;

        threadLoop = false;
        Thread.State thread_state = streamReadThread.getState();
        if (thread_state == Thread.State.BLOCKED ||
            thread_state == Thread.State.WAITING ||
            thread_state == Thread.State.TIMED_WAITING) {
            streamReadThread.interrupt();
        }

        streamReadThread.join();
        streamReadThread = null;
    }
}
