package com.github.kmkt.util.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * 状態に応じスレッドの実行がブロック/通過するゲート処理を行う実行制御インタフェース。
 */
public interface Gate {

    /**
     * 現在の {@link LockGate} の状態に応じて呼び出しスレッドの実行をブロックする。
     *
     * 現在の {@link LockGate} がブロック状態でない場合は、即座に制御を戻す。
     * ブロック状態の場合は、別スレッドにより {@link #setGateState(boolean)} false とするか
     * {@link #releaseGate()} が呼び出されるまでブロックする。
     *
     * @throws InterruptedException
     */
    void gate() throws InterruptedException;

    /**
     * 現在の {@link LockGate} の状態に応じて呼び出しスレッドの実行をタイムアウト付きでブロックする。
     *
     * 現在の {@link LockGate} がブロック状態でない場合は、即座に制御を戻す。
     * ブロック状態の場合は、別スレッドにより {@link #setGateState(boolean)} false とするか
     * {@link #releaseGate()} が呼び出されるかタイムアウトするまでブロックする。
     *
     * @param nanos_timeout
     * @return
     * @throws InterruptedException
     */
    long gateNanos(long nanos_timeout) throws InterruptedException;

    /**
     * 現在の {@link LockGate} の状態に応じて呼び出しスレッドの実行をタイムアウト付きでブロックする。
     *
     * 現在の {@link LockGate} がブロック状態でない場合は、即座に制御を戻す。
     * ブロック状態の場合は、別スレッドにより {@link #setGateState(boolean)} false とするか
     * {@link #releaseGate()} が呼び出されるかタイムアウトするまでブロックする。
     *
     * @param time
     * @param unit
     * @return false タイムアウトした場合 true それ以外
     * @throws InterruptedException
     */
    boolean gate(long time, TimeUnit unit) throws InterruptedException;

    /**
     * {@link Gate} の状態を設定する。
     *
     * ブロック状態とした場合は、{@link #gate()}, {@link #gate(long, TimeUnit)}, {@link #gateNanos(long)} の
     * 各メソッドは呼び出しスレッドの実行をブロックする。
     * 非ブロック状態とした場合は、これらのメソッドは既にブロック中のものも含めて即座に制御を返す。
     *
     * @param block true ブロック状態 false 非ブロック状態
     */
    void setGateState(boolean block);

    /**
     * {@link Gate} の状態を設定する。
     *
     * @return true ブロック状態 false 非ブロック状態
     */
    boolean getGateState();

    /**
     * ブロック状態にする。
     * {@link #setGateState(boolean)} true と等価。
     */
    void setGate();

    /**
     * 非ブロック状態にする。
     * {@link #setGateState(boolean)} false と等価。
     */
    void releaseGate();
}
