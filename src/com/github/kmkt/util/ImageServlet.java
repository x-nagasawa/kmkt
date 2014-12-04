package com.github.kmkt.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Image servlet
 * 
 * License : MIT License
 */
public class ImageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ImageServlet.class);

    private static final String CONTENT_TYPE = "image/jpeg";

    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private byte[] frame = null;
    private String contentType = "";

    /**
     * イメージデータを供給する。
     * 
     * <pre>
     * 送出するイメージデータを与える。
     * クライアントから接続されていない場合、与えられたフレームデータは破棄される。
     * </pre>
     * 
     * @param image イメージデータ
     */
    public void pourFrame(byte[] image, String content_type) {
        lock.writeLock().lock();
        try {
            this.frame = image;
            this.contentType = content_type;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        logger.debug("doGet");
        logger.info("Accept HTTP connection.");

        byte[] sending_frame = null;
        String content_type = "";
        lock.readLock().lock();
        try {
            sending_frame = frame;
            content_type = contentType;
        } finally {
            lock.readLock().unlock();
        }

        if (sending_frame == null) {
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "No image");
            return;
        }
        if (content_type == null || content_type.isEmpty()) {
            content_type = CONTENT_TYPE;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(content_type);
        resp.setContentLength(sending_frame.length);
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("Connection", "Close");

        OutputStream out = new BufferedOutputStream(resp.getOutputStream());
        try {
            out.write(sending_frame);
            out.flush();
            logger.trace("Send image : {} bytes", sending_frame.length);
        } catch (IOException e) {
            // connection closed
            logger.info("Close HTTP connection.");
        }
    }
}
