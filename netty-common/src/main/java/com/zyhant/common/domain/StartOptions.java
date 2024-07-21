package com.zyhant.common.domain;

/**
 * 启动配置
 * @author zyhant
 * @date 2024/7/21 22:56
 */
public class StartOptions {

    private int port;

    private int queue = 1024;

    private int buffer = 10485760;

    private boolean nagle = true;

    private boolean probe = true;

    private int readerIdle = 600;

    private int writerIdle = 600;

    private int allIdle = 1200;

    /**
     * 设置端口
     * @param port 端口
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 设置队列数量
     * @param queue 队列数量
     */
    public void setQueue(int queue) {
        this.queue = queue;
    }

    /**
     * 设置缓冲区大小
     * @param buffer 缓冲区大小
     */
    public void setBuffer(int buffer) {
        this.buffer = buffer;
    }

    /**
     * 是否使用Nagle算法
     * @param nagle Nagle算法
     */
    public void setNagle(boolean nagle) {
        this.nagle = nagle;
    }

    /**
     * 是否自动发送一个活动探测数据报文
     * @param probe 数据报文
     */
    public void setProbe(boolean probe) {
        this.probe = probe;
    }

    /**
     * 设置读取空闲时间(秒)
     * @param readerIdle 读取空闲时间
     */
    public void setReaderIdle(int readerIdle) {
        this.readerIdle = readerIdle;
    }

    /**
     * 设置写入空闲时间(秒)
     * @param writerIdle 写入空闲时间
     */
    public void setWriterIdle(int writerIdle) {
        this.writerIdle = writerIdle;
    }

    /**
     * 设置所有空闲时间(秒)
     * @param allIdle 所有空闲时间
     */
    public void setAllIdle(int allIdle) {
        this.allIdle = allIdle;
    }
}
