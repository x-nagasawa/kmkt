package com.github.kmkt.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NetworkChannel;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Socket から UTF-8 の文字列を行単位で読み出し、リスナを callback する
 * 
 * License : MIT License
 */
public class UTF8StringReceiver {
    private static final Logger logger = LoggerFactory.getLogger(UTF8StringReceiver.class);

    private final ExecutorService execPool;
    private final boolean ownExecPool;
    private final InetSocketAddress listenEndpoint;
    private volatile ListenCompletionListener listenCallback;


    public UTF8StringReceiver(InetSocketAddress listen) {
        if (listen == null)
            throw new IllegalArgumentException("listen should not be null");
        ownExecPool = true;
        execPool = Executors.newCachedThreadPool(Executors.defaultThreadFactory());
        listenEndpoint = listen;
    }

    public UTF8StringReceiver(InetSocketAddress listen, ExecutorService pool) {
        if (listen == null)
            throw new IllegalArgumentException("listen should not be null");
        if (pool != null) {
            ownExecPool = false;
            execPool = pool;
        } else {
            ownExecPool = true;
            execPool = Executors.newCachedThreadPool(Executors.defaultThreadFactory());
        }
        listenEndpoint = listen;
    }

    /**
     * Socket accept 時の callback
     */
    public interface ListenCompletionListener {
        /**
         * accept できた場合に callback される
         * accept された socket からのデータを受け取るための ReceiveListener を返す
         * null を返した場合は、受信されたデータは読み捨てされる
         * 
         * @param remote リモートアドレス
         * @return 
         */
        ReceiveListener<?> accepted(SocketAddress remote);

        /**
         * accept に失敗した場合に callback される
         * @param e 失敗要因となった例外
         */
        void failed(Throwable e);
    }

    /**
     * Socket での UTF-8 文字列受信時に呼び出される callback
     *
     * @param <T>
     */
    public static abstract class ReceiveListener<T> {
        private T attachment;

        /**
         * コンストラクタ
         * @param attachment callback時に付与される任意のオブジェクト
         */
        public ReceiveListener(T attachment) {
            this.attachment = attachment;
        }

        /**
         * Socket での UTF-8 文字列受信時に呼び出される callback
         * 
         * @param line 受信された文字列
         * @param attachement コンストラクタで与えたオブジェクト
         */
        public abstract void onReceive(String line, T attachement);
        
        /**
         * Socket close 時に呼び出される callback
         * @param attachement
         */
        public abstract void onClose(T attachement);

        void onReceive(String line) {
            this.onReceive(line, this.attachment);
        }
        void onClose() {
            this.onClose(this.attachment);
        }
    }

    public void setCallback(ListenCompletionListener callback) {
        this.listenCallback = callback;
    }

    private Set<NetworkChannel> activeChannels = Collections.synchronizedSet(new HashSet<NetworkChannel>());
    private AsynchronousServerSocketChannel assc = null;
    public void start() throws IOException {
        if (assc != null)
            return;
        assc = AsynchronousServerSocketChannel.open().bind(listenEndpoint);
        assc.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(final AsynchronousSocketChannel result,
                    Void attachment) {
                assc.accept(null, this);
                try {
                    SocketAddress remote = result.getRemoteAddress();
                    ListenCompletionListener local_listener = listenCallback;
                    final ReceiveListener<?> listen;
                    if (local_listener != null) {
                        listen = local_listener.accepted(remote);
                    } else {
                        listen = null;
                    }

                    execPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            activeChannels.add(result);
                            try {
                                StringBuilder buf = new StringBuilder();
                                while (result.isOpen()) {
                                    // 受信
                                    if (result.read(buffer).get() < 0) {
                                        break;  // EoS
                                    }
                                    buffer.flip();

                                    // ByteBuffer から文字列に変換
                                    buf.append(getStr(buffer));

                                    // 行毎に分割
                                    int st = 0;
                                    int ed = 0;
                                    while (0 < (ed = buf.indexOf("\r\n", st))) {
                                        String line = buf.substring(st, ed);
                                        if (listen != null) {
                                            listen.onReceive(line);
                                        }
                                        st = ed + 2;
                                    }
                                    ed = buf.lastIndexOf("\r\n");
                                    if (0 < ed) {
                                        buf.delete(0, ed+2);
                                    }
                                }
                            } catch (InterruptedException e) {
                                logger.debug("Exception occured when SocketChannel reading", e);
                            } catch (ExecutionException e) {
                                if (assc.isOpen()) {
                                    logger.error("Exception occured when SocketChannel reading", e);
                                }
                            } finally {
                                try {
                                    result.close();
                                    activeChannels.remove(result);
                                } catch (IOException e) {
                                    logger.error("Exception occured when SocketChannel closing", e);
                                }
                                if (listen != null) {
                                    listen.onClose();
                                }
                            }
                        }
                    });
                } catch (IOException e) {
                    this.failed(e, attachment);
                    try {
                        result.close();
                    } catch (IOException e1) {
                        logger.error("Exception occured when SocketChannel closing", e1);
                    }
                }
            }

            @Override
            public void failed(Throwable e, Void attachment) {
                // close時に出るAsynchronousCloseException は無視
                if (!(e instanceof AsynchronousCloseException)) {
                    listenCallback.failed(e);
                }
            }
        });
    }

    public void stop() throws IOException {
        if (ownExecPool) {
            execPool.shutdown();
        }
        if (assc != null) {
            assc.close();
        }
        synchronized (activeChannels) {
            for (NetworkChannel soc : activeChannels) {
                try {
                    soc.close();
                } catch (IOException e) {
                    logger.error("Error at processing a socket of " + soc.getLocalAddress(), e);
                }
            }
            activeChannels.clear();
        }
    }

    // flip 済み ByteBuffer からUTF-8文字列を抽出
    private String getStr(ByteBuffer buffer) {
        if (buffer.limit() == buffer.position())
            return "";

        String line = null;
        Charset charset = Charset.forName("UTF-8");
        // 文字列端探索
        if ((buffer.get(buffer.limit() - 1) & (byte) 0x80) == 0) {
            // 受信したバイト列の末端がASCII
            line = charset.decode(buffer).toString();
            buffer.clear();
        } else {
            // 受信したバイト列の末端が中途半端な可能性あり
            int pos = buffer.limit() - 1;
            // UTF-8 1 byte 目探索
            byte b = 0;
            for (; 0 <= pos; pos--) {
                b = buffer.get(pos);
                if ((b & (byte) 0xc0) == (byte) 0xc0) {
                    break;
                }
            }
            int following_len = 0;   // 文字コード長
            if ((byte) 0xf0 <= b && b <= (byte) 0xf7) {
                following_len = 3;
            } else if ((byte) 0xe0 <= b && b <= (byte) 0xef) {
                following_len = 2;
            } else if ((byte) 0xc2 <= b && b <= (byte) 0xdf) {
                following_len = 1;
            }
            if (pos + following_len == buffer.limit() - 1) {
                // 全部読める
                line = charset.decode(buffer).toString();
                buffer.clear();
            } else {
                // 端切れがある
                int parts_len = buffer.limit() - pos;   // 端切れ分
                byte[] parts = new byte[parts_len];
                for (int i=0; pos < buffer.limit(); i++, pos++) {
                    parts[i] = buffer.get(pos);
                }
                buffer.limit(buffer.limit() - parts_len);
                line = charset.decode(buffer).toString();
                buffer.clear();
                buffer.put(parts);
            }
        }
        return line;
    }
}