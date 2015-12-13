package com.github.kmkt.util.concurrent;

/**
 * タスク処理インタフェース
 * 
 * @param <T> 引数の型
 * @param <R> 返り値の型
 */
public interface TaskWorker<T, R> {

    /**
     * T taskreq を処理して R を返す
     * @param taskreq
     * @return
     * @throws Exception
     */
    R doTask(T taskreq) throws Exception;
}
