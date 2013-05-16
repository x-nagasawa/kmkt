package com.github.kmkt.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * License: MIT License
 */

/**
 * MJPEG over HTTP servlet
 * 
 * ※複数接続には未対応
 */
public class MjpegServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(MjpegServlet.class);

    private static final byte[] CRLF = new byte[]{0x0d, 0x0a};
    private static final String CONTENT_TYPE = "multipart/x-mixed-replace";

    private AtomicBoolean servicing = new AtomicBoolean();  // 接続中フラグ
    BlockingQueue<byte[]> frameServer = new SynchronousQueue<byte[]>();

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
        frameServer.offer(frame);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        logger.debug("doGet");
        if (!servicing.compareAndSet(false, true)) {
            // 別接続有り
            logger.debug("there are other connections");
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        String delemeter_str = Long.toHexString(System.currentTimeMillis());
        byte[] delimiter = ("--"+delemeter_str).getBytes();
        byte[] content_type = "Content-Type: image/jpeg".getBytes();

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(CONTENT_TYPE+";boundary=" + delemeter_str);
        resp.setHeader("Connection", "Close");

        OutputStream out = new BufferedOutputStream(resp.getOutputStream());
        try {
            frameServer.clear();
            int i=-1;
            while (true) {
                byte[] frame = frameServer.poll(10, TimeUnit.SECONDS);
                if (frame == null)
                    continue;

                byte[] content_length = ("Content-Length: " + frame.length).getBytes();

                i++;

                logger.debug("Send frame {}", i);

                out.write(delimiter);
                out.write(CRLF);
                out.write(content_type);
                out.write(CRLF);
                out.write(content_length);
                out.write(CRLF);
                out.write(CRLF);
                out.write(frame);
//                out.write(CRLF); // XXX 厳密には必要？
                out.flush();
            }
        } catch (IOException e) {
            // connection closed
            logger.info("HTTP connection closed.");
        } catch (InterruptedException e) {
            logger.info(e.getMessage(), e);
        }
        servicing.set(false);
    }
}
