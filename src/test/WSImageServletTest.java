package test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.github.kmkt.util.WSImageServlet;

public class WSImageServletTest {
    private static final String PROPERTY_FILE = "imageserver.properties";
    private static final int DEFAULT_WEBPORT = 8080;   // Web ポート番号
    private static final String DEFAULT_WSPATH = "/ws";

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println("Image 出力サンプルサーバ");
                System.out.println();
                System.out.println("USAGE A : WSImageServletTest [-p <port>] (duration) (jpg) (jpg) ...");
                System.out.println("USAGE B : WSImageServletTest [-p <port>] (duration) (lst)");
                System.out.println("port     : ポート番号");
                System.out.println("duration : フレーム間隔 (ms)");
                System.out.println("jpg      : 送出するJPEGファイル 複数指定時は先頭から順に送出される");
                System.out.println("lst      : 送出するJPEGファイルのリスト");
                System.exit(-1);
            }
            
            // デフォルト値
            int webport = DEFAULT_WEBPORT;
            String wspath = DEFAULT_WSPATH;

            // プロパティファイル読み込み
            File f = new File(PROPERTY_FILE);
            if (f.exists() && f.isFile()) {
                System.out.println("Load from property file. : " + PROPERTY_FILE);
                // Try to read property file.
                Properties serverprop = new Properties();
                Reader freader = null;
                try {
                    freader = new FileReader(PROPERTY_FILE);
                    serverprop.load(freader);
                    // may
                    if (serverprop.containsKey("port")) {
                        webport = Integer.parseInt(serverprop.getProperty("port"));
                    }
                    // may
                    if (serverprop.containsKey("path")) {
                        wspath = serverprop.getProperty("path");
                    }
                } catch (IOException e) {
                    System.out.println("IO error at reading property file. : " + PROPERTY_FILE);
                    System.exit(-1);
                } finally {
                    if (freader != null)
                        freader.close();
                }
            }

            int arg_index = 0;
            // ポート指定
            if (args[0].toLowerCase().equals("-p")) {
                webport = Integer.parseInt(args[1]);
                arg_index = 2;
            }

            final int duration = Integer.parseInt(args[arg_index]);
            arg_index++;

            // 読み込むファイルリストの作成
            List<String> files = new ArrayList<String>();
            if (args[arg_index].toLowerCase().endsWith("jpg") || args[arg_index].toLowerCase().endsWith("jpeg")) {
                for (; arg_index < args.length; arg_index++) {
                    files.add(args[arg_index]);
                }
            } else {
                try {
                    FileReader fr = new FileReader(args[arg_index]);
                    BufferedReader reader = new BufferedReader(fr);
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        files.add(line.trim());
                    }
                    reader.close();
                    fr.close();
                } catch (IOException e) {
                    System.out.println("Following IOException occurred. Ignore this file.");
                    e.printStackTrace();
                    System.exit(-1);
                } 
            }

            // 画像ファイルの読み込み
            final List<byte[]> frames = new ArrayList<byte[]>(files.size());
            List<String> loaded_files = new ArrayList<String>(files.size());
            for (String file : files) {
                System.out.print("Loading " + file + " ... ");
                try {
                    FileInputStream fi = new FileInputStream(file);
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    int ch = 0;
                    while ((ch = fi.read()) != -1) {
                        bo.write(ch);
                    }
                    fi.close();
                    bo.close();
                    byte[] frame = bo.toByteArray();
                    if (frame[0] == (byte) 0xff && frame[1] == (byte) 0xd8 &&     // SOI
                        frame[frame.length-2] == (byte) 0xff && frame[frame.length-1] == (byte) 0xd9) {   // EOI
                        frames.add(frame);
                        loaded_files.add(file);
                        System.out.println("OK");
                    } else {
                        System.out.println("Invalid JPEG file format. Ignore this file.");
                    }
                } catch (IOException e) {
                    System.out.println("Following IOException occurred. Ignore this file.");
                    e.printStackTrace();
                }
            }

            if (loaded_files.size() == 0) {
                System.out.println("No avaleble JPEG.");
                System.exit(-1);
            }

            System.out.println("Duration (ms) : " + duration);
            System.out.println("JPEG frames   :");
            int i=0;
            for (String file : loaded_files) {
                System.out.printf(" %2d %s%n", i, file);
                i++;
            }

            // init Jetty
            Server server = new Server(webport);
            server.setStopAtShutdown(true);
            ServletContextHandler root = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);

            // set default web servlet and document root.
            root.setResourceBase("./");
            root.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
            root.addServlet(DefaultServlet.class, "/*");

            // MJPEGのセットアップ
            final WSImageServlet servlet = new WSImageServlet();
            ServletHolder wsh = new ServletHolder(servlet);
            root.addServlet(wsh, wspath);

            // set callback when receive JPEG frame
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int frame = 0;
                        long next_time = System.currentTimeMillis() + duration;
                        while (true) {
                            servlet.pourFrame(frames.get(frame));   // Servlet 側でフレームが取り出されるまでブロック
                            long now = System.currentTimeMillis();
                            frame++;
                            if (frames.size() == frame) {
                                frame = 0;
                            }
                            if (now < next_time) {
                                // 次フレーム開始時刻まで待機
                                Thread.sleep(next_time - now);
                                // 次々フレーム開始時刻
                                next_time += duration;
                            } else {
                                // 遅延したのでウエイト無しで次フレーム
                                while (next_time < now) {
                                    next_time += duration;
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.setDaemon(true);
            t.start();
    
            // Jettyサーバ起動
            server.start();
            System.out.println("Start web server at port : " + webport);
            System.out.println("WebSocket path           : " + wspath);

            // コマンド入力待ち
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print("Input Command >");
                String line = reader.readLine();
                // バックグラウンド動作対策
                if (line == null) {
                    Thread.sleep(1000);
                    continue;
                }

                if ("".equals(line.trim()))
                    continue;

                String[] cmds = line.split("\\s+");

                if ("bye".equals(cmds[0])) {
                    // bye : 終了
                    break;
                }
            }
            if (server != null) {
                // Jettyサーバ停止
                server.stop();
                server.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
