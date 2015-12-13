package com.github.kmkt.util.concurrent;

import java.util.concurrent.Future;

/**
 * タスク処理終了時の通知 callback インタフェース
 * 
 * @param <T> 引数の型
 * @param <R> 返り値の型
 */
public interface TaskCompleteListener<T, R> {
    /**
    * T taskreq の処理が終了した際に、処理結果を含む Future f とともに呼ばれる
     * @param taskreq
     * @param f
     */
    public void onComplete(T taskreq, Future<R> f);
}
