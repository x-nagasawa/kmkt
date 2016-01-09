package com.github.kmkt.util.concurrent;

/**
 * TaskWorker の実行が阻害された場合に生じる例外。
 * 実行予定の TaskWorker に割り当てられた taskreq を持ち、 {@link #getRequest()} で取得できる。
 */
public class TaskWorkerStartException extends TaskWorkerRunnerException {
    private static final long serialVersionUID = 1L;

    /** 実行予定の TaskWorker に割り当てられた taskreq */
    private Object taskreq;

    /**
     * 指定された詳細メッセージを持つ、新規例外を構築します。
     * @param message 詳細メッセージ。
     * @param taskreq 実行予定の TaskWorker に割り当てられた taskreq
     */
    public TaskWorkerStartException(String message, Object taskreq) {
        super(message);
        this.taskreq = taskreq;
    }

    /**
     * 実行予定の TaskWorker に割り当てられた taskreq を取得する
     * @return taskreq 実行予定の TaskWorker に割り当てられた taskreq
     */
    public Object getRequest() {
        return taskreq;
    }
}
