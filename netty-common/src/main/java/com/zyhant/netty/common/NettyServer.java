package com.zyhant.netty.common;

import com.zyhant.netty.common.domain.NettyConfig;
import com.zyhant.netty.common.domain.StartOptions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Netty服务端
 * @author zyhant
 * @date 2024/7/22 03:02
 */
public abstract class NettyServer<T extends NettyConfig> {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);
    protected T config;
    protected int port;
    private EventLoopGroup parentGroup;
    private EventLoopGroup childGroup;

    public NettyServer(T config) {
        this.config = config;
    }

    public void start(StartOptions options) {
        if (options == null) {
            log.error("NettyServer启动失败,配置不能为空");
            return;
        }
        this.doStart(options);
    }

    public void stop() {
        if (this.parentGroup != null && this.childGroup != null) {
            this.parentGroup.shutdownGracefully();
            this.childGroup.shutdownGracefully();
        }
    }

    private void doStart(StartOptions options) {
        try {
            Field fieldPort = options.getClass().getDeclaredField("port");
            fieldPort.setAccessible(true);
            this.port = (int) fieldPort.get(options);
            fieldPort.setAccessible(false);
            Field fieldParentGroup = options.getClass().getDeclaredField("parentGroup");
            fieldParentGroup.setAccessible(true);
            int parentGroupNumber = (int) fieldParentGroup.get(options);
            fieldParentGroup.setAccessible(false);
            if (parentGroupNumber <= 0) {
                parentGroupNumber = 1;
            }
            if (parentGroupNumber > 5) {
                parentGroupNumber = 5;
            }
            Field fieldChildGroup = options.getClass().getDeclaredField("childGroup");
            fieldChildGroup.setAccessible(true);
            int childGroupNumber = (int) fieldChildGroup.get(options);
            fieldChildGroup.setAccessible(false);
            if (childGroupNumber < 0) {
                childGroupNumber = 0;
            }
            if (childGroupNumber > 100) {
                childGroupNumber = 100;
            }
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
            this.parentGroup = new NioEventLoopGroup(parentGroupNumber);
            this.childGroup = new NioEventLoopGroup(childGroupNumber);
            ServerBootstrap bootstrap = new ServerBootstrap();
            // 绑定线程组,设置react模式的主线程池以及IO操作线程池
            bootstrap.group(this.parentGroup, this.childGroup);
            // 设置通讯模式,调用的是实现io.netty.channel.Channel接口的类
            bootstrap.channel(NioServerSocketChannel.class);
            // 设置服务端口
            bootstrap.localAddress(this.port);
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
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    // 设置读写空闲超时时间
                    pipeline.addLast(new IdleStateHandler(readerIdle, writerIdle, allIdle));
                    initPipeline(pipeline);
                }
            });
            ChannelFuture future = bootstrap.bind().sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("NettyServer startup failed", e);
        } finally {
            if (this.parentGroup != null && this.childGroup != null) {
                this.parentGroup.shutdownGracefully();
                this.childGroup.shutdownGracefully();
            }
        }
    }

    protected abstract void initPipeline(ChannelPipeline pipeline);

}
