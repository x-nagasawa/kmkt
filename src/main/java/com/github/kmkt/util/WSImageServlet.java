package com.github.kmkt.util;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WSImageServlet extends WebSocketServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(WSImageServlet.class);

    private final Set<WebSocketCallback> connectedWebSockets = new CopyOnWriteArraySet<WebSocketCallback>();

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

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest req, String prot) {
        return new WebSocketCallback(req.getRemoteAddr());
    }

    private class WebSocketCallback implements WebSocket.OnTextMessage {
        private Connection connection;
        private String remoteAddr;

        public WebSocketCallback(String remoteAddr) {
            this.remoteAddr = remoteAddr;
        }

        @Override
        public void onClose(int close_code, String msg) {
            connectedWebSockets.remove(this);
            logger.debug("WebSocket closed from {}", remoteAddr);
        }

        @Override
        public void onOpen(Connection connection) {
            this.connection = connection;
            connectedWebSockets.add(this);
            logger.debug("WebSocket connected from {}", remoteAddr);
        }

        @Override
        public void onMessage(String msg) {
            logger.debug("Message {} from {}", msg, remoteAddr);
        }

        public boolean isOpen() {
            return connection.isOpen();
        }

        public void sendFrame(byte[] frame) {
            try {
                connection.sendMessage(frame, 0, frame.length);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
