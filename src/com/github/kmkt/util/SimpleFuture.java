package com.github.kmkt.util;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * スレッド間の単純な値受け渡しに用いる Future 
 * FutureTask 使うほど
 * @see java.util.concurrent.Future
 * @param <V>
 */
public class SimpleFuture<V> implements Future<V> {
    protected final CountDownLatch latch = new CountDownLatch(1);

    protected volatile boolean cancelled = false;
    protected volatile boolean accepted = false;

    protected V result = null;
    protected Throwable exception = null;

    /**
     * Future で受け渡すオブジェクトを設定する
     * @param obj
     * @return 受け付けられた場合は true<br> 既に受け付け済み、又は cancel 済みの場合は false
     */
    public boolean set(V obj) {
        synchronized(this) {
            if (accepted || cancelled) {
                return false;
            }
            accepted = true;
            result = obj;
        }
        latch.countDown();  // release latch
        done();

        return true;
    }

    /**
     * Future で受け渡す例外を設定する
     * 例外が設定された場合、get は設定された例外をラップした ExecutionException を起こす
     * @param t
     * @return 受け付けられた場合は true<br> 既に受け付け済み、又は cancel 済みの場合は false
     */
    public boolean setException(Throwable t) {
        synchronized(this) {
            if (accepted || cancelled) {
                return false;
            }
            accepted = true;
            exception = t;
        }
        latch.countDown();  // release latch
        done();

        return true;
    }

    /**
     * @see java.util.concurrent.Future#cancel(boolean)
     * @param mayInterruptIfRunning 無視されます
     * @return @see java.util.concurrent.Future#cancel(boolean)
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized(this) {
            if (accepted || cancelled) {
                return false;
            }
            cancelled = true;
        }
        latch.countDown();  // release latch
        done();

        return false;
    }

    /**
     * Future の状態が done (isDone が true) になる際に呼び出される protected メソッド
     * デフォルトは空実装
     */
    protected void done() {
    }

    /**
     * @see java.util.concurrent.Future#get()
     */
    @Override
    public V get() throws InterruptedException, ExecutionException {
        if (!accepted && !cancelled) {
            latch.await();  // wait to release latch
        }
        if (cancelled) {
            throw new CancellationException();
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    /**
     * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        if (!accepted && !cancelled) {
            if (!latch.await(timeout, unit))      // wait to release latch
                throw new TimeoutException();
        }
        if (cancelled) {
            throw new CancellationException();
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    /**
     * Future に値、例外が設定されるか、cancel されるまで待機する
     * @throws InterruptedException
     */
    public void await() throws InterruptedException {
        latch.await();  // wait to release latch
    }

    /**
     * Future の状態が確定（値、例外が設定されるか、cancel されるか）するか、タイムアウトするまで待機する
     * @param timeout 待機する最長時間
     * @param unit timeout 引数の時間単位
     * @return Future の状態が確定した場合は true タイムアウトした場合は false
     * @throws InterruptedException
     */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);  // wait to release latch
    }

    /**
     * @see java.util.concurrent.Future#isCancelled()
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * @see java.util.concurrent.Future#isDone()
     */
    @Override
    public boolean isDone() {
        return (cancelled || accepted);
    }

    /**
     * 例外が設定されたか否かを取得する
     * true を返す場合、 get を呼ぶと ExecutionException が生じる
     * @return
     */
    public boolean hasException() {
        return (accepted && exception != null);
    }
}
