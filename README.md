kmkt
====
### 自分用Javaライブラリ


* org.slf4j.impl SLF4j org.grlea.log.SimpleLogger ブリッジ
* W3CDTF W3CDTF形式のフォーマッタ
* PrefixedProperties プロパティを階層的に扱い読み込むための Properties ラッパー
* StreamSplitter InputStream を指定のバイト列をデリミタとして分割読み込みする補助クラス
* MjpegServlet MJPEG over HTTP 配信 servlet. 組み込みJetty用. 要 Jetty8
* WSImageServlet WebSocket 経由でイメージを連続送信する servlet. 組み込みJetty用. 要 Jetty8
* ImageServlet サーバ側の操作で更新可能な画像を返す servlet. 組み込みJetty用. 要 Jetty8
* MjpegHTTPReader MJPEG over HTTP 受信用. 要 Apache HttpClient


### 使用外部ライブラリ

* [slf4j 1.7.7](http://www.slf4j.org/)
* [Jetty 8.1.15](http://www.eclipse.org/jetty/)
* [HttpClient 4.3.4](http://hc.apache.org/httpclient-3.x/)


