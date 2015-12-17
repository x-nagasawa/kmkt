package com.github.kmkt.util.concurrent;

/**
 * タスク処理が終了した TaskWorker の通知インタフェース
 */
public interface TaskWorkerCollector {
    /**
     * タスク処理が終了した TaskWorker を与える
     * @param worker タスク処理の実体 TaskWorker
     */
    void collect(TaskWorker<?, ?> worker);
}
