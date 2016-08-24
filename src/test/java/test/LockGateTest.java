package test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.kmkt.util.concurrent.Gate;
import com.github.kmkt.util.concurrent.LockGate;

/**
 * {@link LockGate} の実行テスト。
 * Thread での実行時、<br>
 * gate が非ブロック状態では  {@link Gate#gate(long, TimeUnit)} はブロックされず、200ms 間隔で 's' が出力される。
 * gete がブロック状態では 100ms 間隔で {@link Gate#gate(long, TimeUnit)} がタイムアウトし、その間隔で 't' が出力される。
 *
 * コンソール出力例。<br>
 * <pre>
 * Start thread
 * ssssstttttttttttttttttttssssstttttttttttttttttttsssssstttttttttttttttttssssstttttttttttttttttttssssstttttttttttttttttttsExit thread
 * </pre>
 */
public class LockGateTest {
    public static void main(String[] args) {
        try {
            final Gate gate = new LockGate(false);

            final AtomicBoolean loopf = new AtomicBoolean(true);    // スレッド実行継続フラグ
            Thread loop = new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Start thread");
                    try {
                        while (loopf.get()) {
                            if (!gate.gate(100, TimeUnit.MILLISECONDS)) {   // Gate がブロック状態の場合はブロックする
                                System.out.print("t");  // timeout 時
                                continue;
                            }
                            System.out.print("s");
                            Thread.sleep(200);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Exit thread");
                }
            });
            loop.start();

            for (int i = 0; i<5;i++) {
                Thread.sleep(1000); // 1sec 走る
                gate.setGateState(true);    // ブロック状態に
                Thread.sleep(2000); // 2sec 停まる
                gate.setGateState(false);   // ブロック状態解除
            }

            loopf.set(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
