package com.github.kmkt.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WSImageServlet extends WebSocketServlet {
    
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(WSImageServlet.class);

    private final Set<WebSocketCallback> connectedWebSockets = new CopyOnWriteArraySet<WebSocketCallback>();


    @Override
    public void configure(WebSocketServletFactory factory) {
        logger.debug("configure");
        factory.setCreator(new WebSocketCreator(){
            @Override
            public Object createWebSocket(ServletUpgradeRequest req,
                    ServletUpgradeResponse resp) {
                return new WebSocketCallback();
            }
        });
    }

    /**
     * JPEG フレームデータを供給する。
     * 
     * <pre>
     * MJPEG とする JPEG フレームデータを与える。
     * 与えられたフレームはGET接続時に MJPEG over HTTP でクライアントに送られる。
     * クライアント接続時には pourFrame でフレームデータが与えられ次第、クライアントにその
     * フレームが送信される。
     * クライアントから接続されていない場合、与えられたフレームデータは破棄される。
     * </pre>
     * 
     * @param frame JPEG フレームデータ
     */
    public void pourFrame(byte[] frame) {
        for (WebSocketCallback socket : connectedWebSockets) {
            if (socket.isOpen()) {
                socket.sendFrame(frame);
            }
        }
    }

    private class WebSocketCallback implements WebSocketListener {
        private Session session;
        private String remoteAddr;

        @Override
        public void onWebSocketConnect(Session session) {
            this.session = session;
            this.remoteAddr = session.getRemote().toString();

            connectedWebSockets.add(this);
            logger.debug("WebSocket connected from {}", remoteAddr);
        }


        @Override
        public void onWebSocketClose(int close_code, String msg) {
            connectedWebSockets.remove(this);
            logger.debug("WebSocket closed from {}", remoteAddr);
        }

        @Override
        public void onWebSocketError(Throwable error) {
            logger.error("Error on WebSocket {} {}", session.getRemoteAddress(), error);
        }


        @Override
        public void onWebSocketText(String msg) {
            logger.debug("Message {} from {}", msg, remoteAddr);
        }

        @Override
        public void onWebSocketBinary(byte[] payload, int offset, int length) {
            logger.debug("Message {} bytes from {}", length, remoteAddr);
        }

        public boolean isOpen() {
            return session.isOpen();
        }

        public void sendFrame(byte[] frame) {
            try {
                session.getRemote().sendBytes(ByteBuffer.wrap(frame));
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
