package com.github.kmkt.util.concurrent;

/**
 * 要求に応じタスク処理の実体 TaskWorker を返すのサプライヤorファクトリインタフェース
 * @param <T> TaskWorker への引数の型
 * @param <R> TaskWorker からの返り値の型
 */
public interface TaskWorkerSupplier<T, R> {
    /**
     * タスク処理の実体 TaskWorker を返す
     * @return タスク処理の実体 TaskWorker
     */
    TaskWorker<T, R> get();
}
