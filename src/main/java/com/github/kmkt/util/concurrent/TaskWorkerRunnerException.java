package com.github.kmkt.util.concurrent;

/**
 * TaskWorkerRunner が発生させる例外
 */
public abstract class TaskWorkerRunnerException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * 詳細メッセージが null である新規例外を構築します。
     */
    public TaskWorkerRunnerException() {
        super();
    }

    /**
     * 指定された詳細メッセージ、原因、抑制の有効化または無効化、書き込み可能スタックトレースの有効化または無効化に基づいて、新しい例外を構築します。
     * @param message 詳細メッセージ
     * @param cause 原因
     * @param enableSuppression 抑制を有効化するか、それとも無効化するか
     * @param writableStackTrace スタックトレースを書き込み可能にするかどうか
     */
    public TaskWorkerRunnerException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * 指定された詳細メッセージおよび原因を使用して新規例外を構築します。
     * @param message 詳細メッセージ
     * @param cause 原因
     */
    public TaskWorkerRunnerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 指定された詳細メッセージを持つ、新規例外を構築します。
     * @param message 詳細メッセージ
     */
    public TaskWorkerRunnerException(String message) {
        super(message);
    }

    /**
     * 指定された原因と詳細メッセージ (cause==null ? null : cause.toString()) を持つ新しい例外を構築します
     * @param cause 原因
     */
    public TaskWorkerRunnerException(Throwable cause) {
        super(cause);
    }
}
