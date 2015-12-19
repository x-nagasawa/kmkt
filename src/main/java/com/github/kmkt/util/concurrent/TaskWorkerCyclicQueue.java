package com.github.kmkt.util.concurrent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TaskWorkerRunner の supplier と collector に同一インスタンスを与えることで
 * 登録された TaskWorker をラウンドロビンで利用する TaskWorker キュー
 * TaskWorkerRunner での並行実行数はこのキューに登録された TaskWorker の数で制限されます
 * 
 * @param <T> TaskWorker の引数の型
 * @param <R> TaskWorker の返り値の型
 */
public class TaskWorkerCyclicQueue<T, R> implements TaskWorkerSupplier<T, R>, TaskWorkerCollector {
    private static final Logger logger = LoggerFactory.getLogger(TaskWorkerCyclicQueue.class);

    /** Worker キュー本体 */
    private final BlockingQueue<TaskWorker<T, R>> workerQueue = new LinkedBlockingQueue<>();
    /** 実行中のため collect 時に削除予定の TaskWorker */
    private final Map<TaskWorker<T, R>, SimpleFuture<Boolean>> disabledWorker = new HashMap<>();
    /** この Worker キューの管理下にある TaskWorker */
    private final Set<TaskWorker<T, R>> containsWorker = new HashSet<>();

    /**
     * @inheritDoc
     */
    @Override
    public void collect(TaskWorker<?, ?> worker) {
        try {
            @SuppressWarnings("unchecked")
            TaskWorker<T, R> workeragt = (TaskWorker<T, R>) worker;
            synchronized (containsWorker) {
                SimpleFuture<Boolean> f = disabledWorker.get(workeragt);
                if (f != null) {
                    // 削除予定に入っているので回収せずに return
                    containsWorker.remove(workeragt);
                    f.set(new Boolean(true));
                    return;
                }
            }
            workerQueue.put(workeragt);     // 回収
        } catch (ClassCastException e) {
            logger.error("Invalid worker", e);
        } catch (InterruptedException e) {
            logger.error("InterruptedException at enqueuing a worker", e);
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public TaskWorker<T, R> get() {
        try {
            return workerQueue.take();
        } catch (InterruptedException e) {
            logger.error("InterruptedException at dequeuing a worker", e);
            return null;
        }
    }

    /**
     * TaskWorker を Worker キューに登録する
     * @param worker Worker キューに登録する TaskWorker notnull
     * @return 登録に成功した場合に true
     */
    public boolean addTaskWorker(TaskWorker<T, R> worker) {
        Objects.requireNonNull(worker, "worker should not be null");

        synchronized (containsWorker) {
            if (containsWorker.contains(worker))
                throw new IllegalArgumentException("worker is already added");

            boolean result = workerQueue.add(worker);
            if (result) {
                containsWorker.add(worker);
            }
            return result;
        }
    }

    /**
     * TaskWorker を Worker キューに登録する
     * @param workers null を含まない TaskWorker の Set notnull
     * @return workers 全ての add に成功した場合に true
     */
    public boolean addTaskWorker(Set<TaskWorker<T, R>> workers) {
        Objects.requireNonNull(workers, "workers should not be null");
        for (TaskWorker<T, R> worker : workers) {
            if (worker == null)
                throw new NullPointerException("workers should not have null worker");
        }

        boolean result = true;
        for (TaskWorker<T, R> worker : workers) {
            result &= this.addTaskWorker(worker);
        }
        return result;  // 全ての add に成功した場合に true
    }

    /**
     * TaskWorker を Worker キューから削除する
     * 対象の TaskWorker が実行中の場合は実行終了時に Worker キューから削除される
     * 削除の完了は返り値の Future により知ることができる
     * 
     * @param worker Worker キューから削除する TaskWorker
     * @return 削除の完了を通知する Future
     */
    public Future<Boolean> removeTaskWorker(TaskWorker<T, R> worker) {
        synchronized (containsWorker) {
            if (!containsWorker.contains(worker))
                throw new IllegalArgumentException("worker is not contained");

            SimpleFuture<Boolean> future = new SimpleFuture<>();
            if (workerQueue.remove(worker)) {
                // workerQueue に含まれており削除に成功
                containsWorker.remove(worker);
                future.set(new Boolean(true));  // done removing
            } else {
                // 実行中のため削除予定に登録
                disabledWorker.put(worker, future);
            }
            return future;
        }
    }
    

    /**
     * Worker キューに登録されている TaskWorker 数を取得する
     * @return Worker キューに登録されている TaskWorker 数
     */
    public int getNumOfTaskWorker() {
        synchronized (containsWorker) {
            return containsWorker.size();
        }
    }
}
