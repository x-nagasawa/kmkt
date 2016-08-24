package com.github.kmkt.util.concurrent;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link Gate} の {@link ReentrantLock} による実装。
 * 状態に応じスレッドの実行がブロック/通過するゲート処理を行う実行制御インタフェース。
 * @see Gate
 * 利用サンプル
 * <pre>
 * {@code
 *  public static void main(String[] args) {
 *      try {
 *          AtomicBoolean loopf = new AtomicBoolean(true);
 *          Gate gate = new LockGate(false);
 *          Thread loop = new Thread(new Runnable() {
 *              public void run() {
 *                  System.out.println("Start thread");
 *                  try {
 *                      while (loopf.get()) {
 *                          if (!gate.gate(100, TimeUnit.MILLISECONDS)) {   // Gate がブロック状態の場合はブロックする
 *                              System.out.print("t");  // timeout
 *                              continue;
 *                          }
 *                          System.out.print("s");
 *                          Thread.sleep(200);
 *                      }
 *                  } catch (InterruptedException e) {
 *                      e.printStackTrace();
 *                  }
 *                  System.out.println("Exit thread");
 *              }
 *          });
 *          loop.start();
 *
 *          for (int i = 0; i<5;i++) {
 *              Thread.sleep(1000); // 1sec 走る
 *              gate.setGateState(true);
 *              Thread.sleep(2000); // 2sec 停まる
 *              gate.setGateState(false);
 *          }
 *          loopf.set(false);
 *      } catch (Exception e) {
 *          e.printStackTrace();
 *      }
 *  }
 * }
 * </pre>
 */
public class LockGate implements Gate {
    /** Lock オブジェクト */
    private final Lock lock = new ReentrantLock();
    /** Lock オブジェクト */
    private final Condition condition = lock.newCondition();

    /** true await でブロックする false await でブロックしない */
    private final AtomicBoolean blockState;

    /**
     * 非ブロック状態のインスタンスを生成する。
     */
    public LockGate() {
        this(false);
    }

    /**
     * 初期状態を与えてインスタンスを生成する。
     * @param block 初期状態 true ブロック状態 false 非ブロック状態
     */
    public LockGate(boolean block) {
        this.blockState = new AtomicBoolean(block);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gate() throws InterruptedException {
        if (blockState.get()) {
            try {
                lock.lock();
                while (blockState.get()) {
                    condition.await();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long gateNanos(long nanos_timeout) throws InterruptedException {
        if (blockState.get()) {
            try {
                lock.lock();
                while (blockState.get()) {
                    nanos_timeout = condition.awaitNanos(nanos_timeout);
                    if (nanos_timeout <= 0)
                        return nanos_timeout;
                }
            } finally {
                lock.unlock();
            }
        }
        return nanos_timeout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean gate(long time, TimeUnit unit) throws InterruptedException {
        return gateNanos(unit.toNanos(time)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGateState(boolean block) {
        if (block == true) {
            this.blockState.set(true);
        } else {
            if (!this.blockState.compareAndSet(true, false))
                return;

            try {
                lock.lock();
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getGateState() {
        return this.blockState.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGate() {
        setGateState(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseGate() {
        setGateState(false);
    }

    @Override
    public String toString() {
        return "LockGate [blockState=" + blockState + "]";
    }
}
