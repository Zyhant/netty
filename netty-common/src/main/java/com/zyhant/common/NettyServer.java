package com.zyhant.common;

import com.zyhant.common.domain.StartOptions;
import com.zyhant.common.listener.ConfigListener;
import com.zyhant.common.utils.AsyncTask;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.lang.reflect.Field;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

/**
 * Netty服务端
 * @author zyhant
 * @date 2024/7/22 03:02
 */
public abstract class NettyServer {

    private ConfigListener listener;
    private AsyncTask<String> connectTask;
    private Channel channel;
    private int port;
    private boolean isClosed = false;

    public NettyServer(ConfigListener listener) {
        this.listener = listener;
    }

    public void start(StartOptions options) {
        if (options == null) {
            listener.logError("NettyServer启动失败,配置不能为空");
            return;
        }
        try {
            this.doStart(options);
            this.setClosed(false);
        } catch (Exception e) {
            listener.logError("NettyServer错误", e);
        }
    }

    public void stop() {
        this.setClosed(true);
        if (this.channel != null) {
            this.channel.close();
            this.channel = null;
        }
    }

    private void doStart(StartOptions options) throws Exception {
        Field fieldPort = options.getClass().getDeclaredField("port");
        fieldPort.setAccessible(true);
        this.port = (int) fieldPort.get(options);
        fieldPort.setAccessible(false);
        EventLoopGroup parentGroup = new NioEventLoopGroup();
        EventLoopGroup childGroup = new NioEventLoopGroup();
        this.connectTask = new AsyncTask<String>(listener) {
            @Override
            public String call() throws Exception {
                Field fieldQueue = options.getClass().getDeclaredField("queue");
                fieldQueue.setAccessible(true);
                int queue = (int) fieldQueue.get(options);
                fieldQueue.setAccessible(false);
                Field fieldBuffer = options.getClass().getDeclaredField("buffer");
                fieldBuffer.setAccessible(true);
                int buffer = (int) fieldBuffer.get(options);
                fieldBuffer.setAccessible(false);
                Field fieldNagle = options.getClass().getDeclaredField("nagle");
                fieldNagle.setAccessible(true);
                boolean nagle = (boolean) fieldNagle.get(options);
                fieldNagle.setAccessible(false);
                Field fieldProbe = options.getClass().getDeclaredField("probe");
                fieldProbe.setAccessible(true);
                boolean probe = (boolean) fieldProbe.get(options);
                fieldProbe.setAccessible(false);
                Field fieldReaderIdle = options.getClass().getDeclaredField("readerIdle");
                fieldReaderIdle.setAccessible(true);
                int readerIdle = (int) fieldReaderIdle.get(options);
                fieldReaderIdle.setAccessible(false);
                Field fieldWriterIdle = options.getClass().getDeclaredField("writerIdle");
                fieldWriterIdle.setAccessible(true);
                int writerIdle = (int) fieldWriterIdle.get(options);
                fieldWriterIdle.setAccessible(false);
                Field fieldAllIdle = options.getClass().getDeclaredField("allIdle");
                fieldAllIdle.setAccessible(true);
                int allIdle = (int) fieldAllIdle.get(options);
                fieldAllIdle.setAccessible(false);
                ServerBootstrap bootstrap = new ServerBootstrap();
                // 绑定线程组,设置react模式的主线程池以及IO操作线程池
                bootstrap.group(parentGroup, childGroup);
                // 设置通讯模式,调用的是实现io.netty.channel.Channel接口的类
                bootstrap.channel(NioServerSocketChannel.class);
                // 设置服务端口
                bootstrap.localAddress(port);
                // 设置通道的选项参数
                bootstrap.option(ChannelOption.SO_REUSEADDR, true)// 允许重复使用本地地址和端口
                        .option(ChannelOption.SO_BACKLOG, queue)// 指定队列的大小
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)// 重用缓冲区
                        .option(ChannelOption.SO_RCVBUF, buffer);// 设置接收缓冲区大小
                bootstrap.childOption(ChannelOption.TCP_NODELAY, nagle)// 是否使用Nagle算法
                        .childOption(ChannelOption.SO_KEEPALIVE, probe)// 是否自动发送一个活动探测数据报文
                        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);// 重用缓冲区
                bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 设置读写空闲超时时间
                        pipeline.addLast(new IdleStateHandler(readerIdle, writerIdle, allIdle));
                        initPipeline(pipeline);
                    }
                });
                ChannelFuture future = bootstrap.bind().sync();
                channel = future.channel();
                listener.logInfo("NettyServer启动成功,端口:" + port);
                return null;
            }
        }.execute();
        try {
            this.connectTask.get(5000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            listener.logError("NettyServer错误", e);
            parentGroup.shutdownGracefully();
            childGroup.shutdownGracefully();
            throw e;
        }
        if (this.channel == null) {
            return;
        }

        this.loadNetty();

        this.connectTask = new AsyncTask<String>(listener) {
            @Override
            public String call() {
                try {
                    channel.closeFuture().sync();
                } catch (Exception e) {
                    listener.logError("NettyServer错误", e);
                } finally {
                    parentGroup.shutdownGracefully();
                    childGroup.shutdownGracefully();
                    if (!isClosed()) {
                        // 非主动断开，可能源于服务器原因
                        Exception e = new ConnectException("NettyServer意外关闭");
                        listener.logError("NettyServer错误", e);
                        stop();
                    } else {
                        listener.logInfo("NettyServer停止成功,端口:" + port);
                    }
                }
                return null;
            }
        }.execute();
    }

    private void setClosed(boolean closed) {
        this.isClosed = closed;
    }

    private boolean isClosed() {
        return this.isClosed;
    }

    abstract void initPipeline(ChannelPipeline pipeline);

    abstract void loadNetty();

}
