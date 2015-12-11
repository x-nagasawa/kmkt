package com.github.kmkt.util.mjpeg;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NetworkChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Socket から Raw MJPEG Stream をフレーム単位で読み出し、リスナを callback する
 * 
 * License : MIT License
 */
public class RawMJPEGReceiver {
    private static final Logger logger = LoggerFactory.getLogger(RawMJPEGReceiver.class);

    private final ExecutorService execPool;
    private final boolean ownExecPool;
    private final InetSocketAddress listenEndpoint;
    private volatile ListenCompletionListener listenCallback;


    public RawMJPEGReceiver(InetSocketAddress listen) {
        this(listen, null);
    }

    public RawMJPEGReceiver(InetSocketAddress listen, ExecutorService pool) {
        if (listen == null)
            throw new IllegalArgumentException("listen should not be null");

        if (pool != null) {
            ownExecPool = false;
            execPool = pool;
        } else {
            ownExecPool = true;
            execPool = Executors.newCachedThreadPool(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                }
            });
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
     * Socket での MJPEG フレーム受信時に呼び出される callback
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
         * Socket での MJPEG フレーム受信時に呼び出される callback
         * 
         * @param frame 受信された MJPEG フレーム (JPEGフレーム)
         * @param attachement コンストラクタで与えたオブジェクト
         */
        public abstract void onReceive(byte[] frame, T attachement);
        
        /**
         * Socket close 時に呼び出される callback
         * @param attachement
         */
        public abstract void onClose(T attachement);

        void onReceive(byte[] frame) {
            this.onReceive(frame, this.attachment);
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
                        if (listen == null) {
                            logger.debug("Ignore and close connection from {}", remote);
                            result.close();
                            return;
                        }
                    } else {
                        listen = null;
                    }

                    execPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            FrameParser parser = new FrameParser(8*1024*1024);
                            ByteBuffer buffer = parser.getByteBuffer();
                            activeChannels.add(result);
                            try {
                                while (result.isOpen()) {
                                    // 受信
                                    if (result.read(buffer).get() < 0) {
                                        break;  // EoS
                                    }

                                    byte[] frame = null;
                                    while ((frame = parser.getFrame()) != null) {
                                        if (listen != null) {
                                            listen.onReceive(frame);
                                        }
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
}
