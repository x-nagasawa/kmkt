package com.github.kmkt.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MJPEG over HTTP servlet
 * 
 * License : MIT License
 */
public class MjpegServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(MjpegServlet.class);
    public static long StatisticsDispleyPeriod = 60*1000;   // 

    private static final byte[] CRLF = new byte[]{0x0d, 0x0a};
    private static final String CONTENT_TYPE = "multipart/x-mixed-replace";

    private Set<ClientChannel> clientConnectionSet = new CopyOnWriteArraySet<ClientChannel>();

    class ClientChannel {
        final BlockingQueue<byte[]> frameServer = new SynchronousQueue<byte[]>();
        // statistics
        long channelOpenedAt = 0;
        long lastShownStatistics = 0;
        final AtomicLong recvedFrames = new AtomicLong();
        final AtomicLong sentFrames = new AtomicLong();
        final AtomicLong dropFrames = new AtomicLong();
        final AtomicLong recvedBytes = new AtomicLong();
        final AtomicLong sentBytes =  new AtomicLong();
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
        for (ClientChannel client : clientConnectionSet) {
            client.recvedFrames.incrementAndGet();
            client.recvedBytes.addAndGet(frame.length);
            if (!client.frameServer.offer(frame)) {
                client.dropFrames.incrementAndGet();
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        logger.debug("doGet");
        
        ClientChannel client = new ClientChannel();
        client.channelOpenedAt = System.currentTimeMillis();
        client.lastShownStatistics = client.channelOpenedAt;
        try {
            clientConnectionSet.add(client);
            logger.debug("queueSet size : {}", clientConnectionSet.size());

            logger.info("Accept HTTP connection.");

            String delemeter_str = Long.toHexString(System.currentTimeMillis());
            byte[] delimiter = ("--"+delemeter_str).getBytes();
            byte[] content_type = "Content-Type: image/jpeg".getBytes();

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType(CONTENT_TYPE+";boundary=" + delemeter_str);
            resp.setHeader("Connection", "Close");

            BlockingQueue<byte[]> frameServer = client.frameServer;

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

                    logger.trace("Send frame {}", i);

                    out.write(delimiter);
                    out.write(CRLF);
                    out.write(content_type);
                    out.write(CRLF);
                    out.write(content_length);
                    out.write(CRLF);
                    out.write(CRLF);
                    out.write(frame);
                    out.write(CRLF);
                    out.flush();

                    client.sentFrames.incrementAndGet();
                    client.sentBytes.addAndGet(frame.length);
                    if (StatisticsDispleyPeriod < System.currentTimeMillis() - client.lastShownStatistics) {
                        client.lastShownStatistics = System.currentTimeMillis();
                        logger.debug("[Frames Recv: {}, Send: {}, Drop: {}, Size Recv: {}, Send: {}]", 
                                client.recvedFrames.get(), client.sentFrames.get(), client.dropFrames.get(),
                                client.recvedBytes.get(), client.sentBytes.get());
                    }
                }
            } catch (IOException e) {
                // connection closed
                logger.info("Close HTTP connection.");
            } catch (InterruptedException e) {
                logger.info(e.getMessage(), e);
            }
        } finally {
            clientConnectionSet.remove(client);
            logger.debug("queueSet size : {}", clientConnectionSet.size());
        }
    }
}
