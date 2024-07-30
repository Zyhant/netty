package com.zyhant.netty.mqtt.domain;

import io.netty.channel.Channel;

/**
 * 客户端信息
 * @author zyhant
 * @date 2024/7/23 20:48
 */
public class MqttClient {

    private String clientId;

    private String agreement;

    private String address;

    private int port;

    private int heartbeat;

    private boolean cleanSession;

    private Channel channel;

    /**
     * 获取客户端ID
     * @return ClientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 获取协议类型
     * @return 协议类型
     */
    public String getAgreement() {
        return agreement;
    }

    /**
     * 获取IP地址
     * @return IP地址
     */
    public String getAddress() {
        return address;
    }

    /**
     * 获取端口
     * @return 端口
     */
    public int getPort() {
        return port;
    }

    /**
     * 获取心跳间隔
     * @return (秒)
     */
    public int getHeartbeat() {
        return heartbeat;
    }

    /**
     * 获取是否清除会话
     * @return true:清除;false:不清除
     */
    public boolean isCleanSession() {
        return cleanSession;
    }


    /**
     * 获取通道
     * @return 通道
     */
    public Channel getChannel() {
        return channel;
    }
}
