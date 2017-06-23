kmkt
====
### 自分用Javaライブラリ


* W3CDTF W3CDTF形式のフォーマッタ
* PrefixedProperties プロパティを階層的に扱い読み込むための Properties ラッパー
* StreamSplitter InputStream を指定のバイト列をデリミタとして分割読み込みする補助クラス
* MjpegServlet MJPEG over HTTP 配信 servlet. 組み込みJetty用. 要 Jetty9
* WSImageServlet WebSocket 経由でイメージを連続送信する servlet. 組み込みJetty用. 要 Jetty9
* ImageServlet サーバ側の操作で更新可能な画像を返す servlet. 組み込みJetty用. 要 Jetty9
* MjpegHTTPReader A MJPEG over HTTP receiver. 要 Apache HttpClient
* SimpleFuture Task等に関係なくスレッド間の値受け渡しに絞ったシンプルな Future<V> 実装
* Gate A synchronize mechanism like .NET's ManualResetEvent

### 使用外部ライブラリ

* [Jetty 9.3.6](http://www.eclipse.org/jetty/)
* [HttpClient 4.5.1](http://hc.apache.org/httpcomponents-client-4.5.x/index.html)
* [SLF4J 1.7.13](https://www.slf4j.org/)

### ライセンス License

MIT License
Copyright (c) 2012-2017 NagasawaXien
