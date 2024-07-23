package com.zyhant.mqtt.domain;

/**
 * 发布消息
 * @author zyhant
 * @date 2024/7/23 20:54
 */
public class MqttPublish {

    private MqttClient client;

    private String topic;

    private String content;

    /**
     * 客户端信息
     * @return
     */
    public MqttClient getClient() {
        return client;
    }

    /**
     * 主题
     * @return
     */
    public String getTopic() {
        return topic;
    }

    /**
     * 内容
     * @return
     */
    public String getContent() {
        return content;
    }
}
