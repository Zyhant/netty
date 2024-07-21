package com.zyhant.common.utils;

import com.zyhant.common.listener.ConfigListener;

import java.util.concurrent.*;

/**
 * 线程工具
 * @author zyhant
 * @date 2024/7/21 23:05
 */
public abstract class AsyncTask<T> implements RunnableFuture<T>, Callable<T> {

    private ConfigListener listener;
    private FutureTask<T> futureTask;

    public AsyncTask(ConfigListener listener) {
        this.listener = listener;
        this.futureTask = new FutureTask<T>(this) {
            @Override
            protected void done() {
                AsyncTask.this.done();
            }
        };
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return futureTask.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return futureTask.isCancelled();
    }

    @Override
    public boolean isDone() {
        return futureTask.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return futureTask.get();
        } catch (Exception e) {
            // 发生异常，尝试停止任务
            cancel(true);
            throw e;
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return futureTask.get(timeout, unit);
        } catch (Exception e) {
            // 发生异常，尝试停止任务
            cancel(true);
            throw e;
        }
    }

    @Override
    public void run() {
        futureTask.run();
    }

    protected void done() {

    }

    public AsyncTask<T> execute() {
        try {
            listener.execute(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

}
