package com.github.kmkt.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * submit により投入される引数（タスク）を TaskWorker で実装された処理実体により並列処理するクラス
 * ThreadPoolExecutor では runnable, callable を投入するが、TaskWorkerRunner では TaskWorker への
 * 引数を投入する
 * 
 * License : MIT License
 * Copyright (c) 2015 NagasawaXien
 *
 * @param <T> TaskWorker への引数の型
 * @param <R> TaskWorker からの返り値の型
 */
public class TaskWorkerRunner<T, R> {
    private static final Logger logger = LoggerFactory.getLogger(TaskWorkerRunner.class);

    /** タスク処理用 ThreadPool */
    protected final ExecutorService pool;
    /** 処理スレッド数の最大 */
    protected int maximimParallel;
    /** タスク処理の実体 TaskWorker のサプライヤ・ファクトリ */
    protected final TaskWorkerSupplier<T, R> workerSupplier;
    /** タスク処理が終了した TaskWorker の通知インタフェース */
    protected final TaskWorkerCollector workerCollector;
    /** CallbackCaller callback しない場合は null */
    protected final CallbackCaller<T, R> callbackCaller;

    /** 総タスク数カウンタ */
    private AtomicInteger tasks = new AtomicInteger(0);
    /** 処理中タスク数カウンタ */
    private AtomicInteger runningTasks = new AtomicInteger(0);

    /**
     * 処理終了時に CallbackCaller に終了タスクを引き渡す FutureTask
     * TaskRunner 専用の内部クラス
     * 
     * @see java.util.concurrent.FutureTask
     */
    protected class CallbackFutureTask extends FutureTask<R> {
        private T req;  // TaskRunner が持つ TaskWorker への引数
        public CallbackFutureTask(TaskRunner runner) {
            super(runner);
            if (runner == null)
                throw new IllegalArgumentException("runner should not be null");

            req = runner.getTaskReq();
        }

        public T getTaskReq() {
            return req;
        }

        @Override
        protected void done() {
            // callback 有効時は CallbackCaller にリクエストと自身を登録する
            if (callbackCaller != null)
                callbackCaller.registerFinishedTask(req, this);
        }
    }

    /**
     * TaskWorker を呼び出す Callable wrapper
     */
    protected class TaskRunner implements Callable<R> {
        private T req;  // TaskWorker への引数

        /**
         * 
         * @param taskreq TaskWorker への引数
         */
        public TaskRunner(T taskreq) {
            this.req = taskreq;
        }

        /**
         * TaskWorker への引数を取得する
         * @return
         */
        public T getTaskReq() {
            return req;
        }

        @Override
        public R call() throws Exception {
            TaskWorker<T, R> worker = null;
            try {
                runningTasks.incrementAndGet();
                worker = workerSupplier.get();    // TaskWorker 取得

                R result = null;
                if (worker != null) {
                    result =  worker.doTask(req);
                }
                return result;
            } finally {
                if (workerCollector != null && worker != null)
                    workerCollector.collect(worker);
                runningTasks.decrementAndGet();
                tasks.decrementAndGet();
            }
        }
    }

    /**
     * コールバック処理スレッド
     * @param <T> TaskWorker への引数の型
     * @param <R> TaskWorker からの返り値の型
     */
    private static class CallbackCaller<T, R> implements Runnable {
        /** 処理終了時の callback */
        private TaskCompleteListener<T, R> listener;

        private BlockingQueue<Pair> queue = new LinkedBlockingQueue<>();

        /**
         * タスクとその処理結果を含む Future のペア
         */
        private class Pair {
            T t;
            Future<R> r;
        }

        /**
         * 
         * @param listener 処理終了時の callback
         */
        public CallbackCaller(TaskCompleteListener<T, R> listener) {
            if (listener == null)
                throw new IllegalArgumentException("listener should not be null");

            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Pair ele = queue.take();    // block
                    try {
                        listener.onComplete(ele.t, ele.r);  // callback
                    } catch (Exception e) {
                        logger.error("Unexpedted exception in TaskCompleteListener callback", e);
                    }
                }
            } catch (InterruptedException e) {
                // exit loop by interruption
            }
        }

        /**
         * 処理終了したタスクの登録
         * @param taskreq 処理終了したタスク
         * @param future taskreq の処理結果を含む Future
         */
        public void registerFinishedTask(T taskreq, Future<R> future) {
            Pair ele = new Pair();
            ele.t = taskreq;
            ele.r = future;
            try {
                queue.put(ele);
            } catch (InterruptedException e) {
                logger.error(null, e);      // not occure
            }
        }
    }

    /**
     * Future のみで処理完了待ちをする TaskWorkerRunner
     * 
     * @param maxparallels 処理スレッド数の最大
     * @param supplier タスク処理の実体 TaskWorker のサプライヤorファクトリ
     */
    public TaskWorkerRunner(int maxparallels, TaskWorkerSupplier<T, R> supplier) {
        this(maxparallels, supplier, null, null);
    }

    /**
     * Future のみで処理完了待ちをする TaskWorkerRunner
     * 
     * @param maxparallels 処理スレッド数の最大
     * @param supplier タスク処理の実体 TaskWorker のサプライヤorファクトリ
     * @param collector タスク処理が終了した TaskWorker の通知インタフェース
     */
    public TaskWorkerRunner(int maxparallels, TaskWorkerSupplier<T, R> supplier, TaskWorkerCollector collector) {
        this(maxparallels, supplier, collector, null);
    }

    /**
     * Future での処理完了待機と callback での処理完了通知をする TaskWorkerRunner
     * 
     * @param maxparallels 処理スレッド数の最大
     * @param supplier タスク処理の実体 TaskWorker のサプライヤorファクトリ
     * @param listener 処理終了時の callback null時は callback しない
     */
    public TaskWorkerRunner(int maxparallels, TaskWorkerSupplier<T, R> supplier, TaskCompleteListener<T, R> listener) {
        this(maxparallels, supplier, null, listener);
    }

    /**
     * Future での処理完了待機と callback での処理完了通知をする TaskWorkerRunner
     * 
     * @param maxparallels 処理スレッド数の最大
     * @param supplier タスク処理の実体 TaskWorker のサプライヤorファクトリ
     * @param collector タスク処理が終了した TaskWorker の通知インタフェース
     * @param listener 処理終了時の callback null時は callback しない
     */
    public TaskWorkerRunner(int maxparallels, TaskWorkerSupplier<T, R> supplier, TaskWorkerCollector collector, TaskCompleteListener<T, R> listener) {
        if (maxparallels <= 0)
            throw new IllegalArgumentException("maxparallels should be a positive integer");
        if (supplier == null)
            throw new IllegalArgumentException("supplier should not be null");

        maximimParallel = maxparallels;
        workerSupplier = supplier;
        workerCollector = collector;
        if (listener != null) {
            callbackCaller = new CallbackCaller<T, R>(listener);
        } else {
            callbackCaller = null;
        }

        int threads = (callbackCaller == null) ? maxparallels : maxparallels + 1;   // callback 有効時は callback 用スレッドを確保する
        pool = Executors.newFixedThreadPool(threads, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });

        // callback 有効時は callback 用スレッドを確保する
        if (callbackCaller != null) {
            pool.execute(callbackCaller);
        }
    }

    /**
     * TaskWorkerRunner に処理対象の taskreq を送信する
     * @param taskreq TaskWorker に処理させる引数 nullable
     * @return タスクの保留完了を表すFuture
     */
    public Future<R> submit(T taskreq) {
        FutureTask<R> taskhost = new CallbackFutureTask(new TaskRunner(taskreq));
        tasks.incrementAndGet();
        pool.execute(taskhost);
        return taskhost;
    }

    /** 
     * 未実行のタスク数を取得する
     * @return
     */
    public int getNumOfWaitingTask() {
        return tasks.get() - runningTasks.get();
    }

    /**
     * 処理中のタスク数を取得する
     * @return
     */
    public int getNumOfRunningTask() {
        return runningTasks.get();
    }

    /**
     * 処理中・未実行タスクを合わせた総タスク数を取得する
     * @return
     */
    public int getNumOfTask() {
        return tasks.get();
    }

    /**
     * 内部の ThreadPool をシャットダウンする
     * @see java.util.concurrent.ExecutorService.shutdown()
     */
    public void shutdown() {
        pool.shutdown();
    }

    /**
     * 内部の ThreadPool をシャットダウンする
     * @see java.util.concurrent.ExecutorService.shutdownNow()
     * @return 未処理のタスクのリスト
     */
    @SuppressWarnings("unchecked")
    public List<T> shutdownNow() {
        List<Runnable> remain = pool.shutdownNow();
        List<T> result = new ArrayList<T>(remain.size());
        for (Runnable r : remain) {
            if (!(r instanceof CallbackCaller)) {
                result.add(((CallbackFutureTask) r).getTaskReq());
            }
        }
        return result;
    }

    /**
     * 内部の ThreadPool のシャットダウン完了を待つ
     * @see java.util.concurrent.ExecutorService.awaitTermination(int, TimeUnit)
     * 
     * @param timeout 待機する最長時間
     * @param unit timeout引数の時間単位
     * @return シャットダウンした場合は true それ以外は false
     * @throws InterruptedException
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return pool.awaitTermination(timeout, unit);
    }
}
