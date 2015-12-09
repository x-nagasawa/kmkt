kmkt
====
### 自分用Javaライブラリ


* W3CDTF W3CDTF形式のフォーマッタ
* PrefixedProperties プロパティを階層的に扱い読み込むための Properties ラッパー
* StreamSplitter InputStream を指定のバイト列をデリミタとして分割読み込みする補助クラス
* MjpegServlet MJPEG over HTTP 配信 servlet. 組み込みJetty用. 要 Jetty8
* WSImageServlet WebSocket 経由でイメージを連続送信する servlet. 組み込みJetty用. 要 Jetty8
* ImageServlet サーバ側の操作で更新可能な画像を返す servlet. 組み込みJetty用. 要 Jetty8
* MjpegHTTPReader MJPEG over HTTP 受信用. 要 Apache HttpClient
* SimpleFuture Task等に関係なくスレッド間の値受け渡しに絞ったシンプルな Future<V> 実装

### 使用外部ライブラリ

* [Jetty 8.1.15](http://www.eclipse.org/jetty/)
* [HttpClient 4.3.4](http://hc.apache.org/httpclient-3.x/)


